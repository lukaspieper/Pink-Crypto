/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.work

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.Worker
import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.common.NotificationChannel
import de.lukaspieper.truvark.common.domain.vault.Vault
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.logcat
import de.lukaspieper.truvark.common.work.Scheduler
import de.lukaspieper.truvark.common.work.WorkBundle
import de.lukaspieper.truvark.di.VaultModule

/**
 * A [WorkManager] that schedules [WorkBundle]s and automatically enqueues [DatabaseSyncingWorker] to ensure database
 * consistency. For running work, notifications are shown.
 *
 * Note that most workers (including [DatabaseSyncingWorker]) require [VaultModule.initializeVaultModule]
 * to be executed before.
 */
class WorkScheduler(private val appContext: Context) : Scheduler() {

    companion object {
        const val CHANNEL_ID = "de.lukaspieper.truvark"

        /**
         * Just any constant name that is used for enqueuing all [Worker]s to prevent running multiple operations in
         * the same folder or even on the same files.
         */
        const val UNIQUE_WORK_NAME = "UNIQUE_WORK_NAME"
    }

    private val workManager = WorkManager.getInstance(appContext)
    private val notificationChannel = NotificationChannel(appContext, CHANNEL_ID, R.string.foreground_service)

    private val scheduledBundles = mutableMapOf<Int, ScheduledBundle>()

    init {
        val enqueuedWorkQuery = WorkQuery.fromStates(WorkInfo.State.ENQUEUED)
        val enqueuedWorkInfo = workManager.getWorkInfos(enqueuedWorkQuery).get()
        logcat(LogPriority.INFO) {
            "Number of enqueued work that did not run before being canceled: ${enqueuedWorkInfo.size}"
        }

        // The user might have switched the vault. To avoid any side effects all work will be canceled.
        workManager.cancelAllWork()

        // Delete data from eligible finished work because failed work cannot be retried (SAF permissions, etc).
        workManager.pruneWork()
    }

    override fun onVaultChanged(vault: Vault) {
        // TODO: Pass down the vault ID, to check if the worker got the correct vault injected
        schedule(DatabaseSyncingWorker.EmptyWorkBundle(), AndroidSchedulerMetadata(R.string.sync_database))
    }

    override fun schedule(workBundle: WorkBundle, metadata: SchedulerMetadata) {
        require(metadata is AndroidSchedulerMetadata)

        var notificationId: Int
        do {
            notificationId = notificationChannel.generateNotificationId()
        } while (scheduledBundles.containsKey(notificationId))

        val notificationBuilder = notificationChannel.provideNotificationBuilder().apply {
            setSmallIcon(R.drawable.ic_truvark)
            setContentTitle(appContext.getString(metadata.notificationTitle))
            setCategory(Notification.CATEGORY_SERVICE)
            setOnlyAlertOnce(true)
            setOngoing(true)
            setVisibility(NotificationCompat.VISIBILITY_SECRET)
            setProgress(0, 0, true)
        }
        notificationChannel.notify(notificationId, notificationBuilder.build())

        val scheduledBundle = ScheduledBundle(notificationId, metadata, workBundle, notificationBuilder)
        scheduledBundles[scheduledBundle.notificationId] = scheduledBundle

        val workRequests = MutableList(workBundle.size) { buildOneTimeWorkRequest<UniversalWorker>(scheduledBundle) }
        val dbSyncRequest = buildOneTimeWorkRequest<DatabaseSyncingWorker>(
            scheduledBundle.copy(
                metadata = scheduledBundle.metadata.copy(notificationTitle = R.string.sync_database)
            )
        )

        workManager.beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, workRequests)
            .then(dbSyncRequest)
            .enqueue()
    }

    private inline fun <reified T : ListenableWorker> buildOneTimeWorkRequest(
        scheduledBundle: ScheduledBundle
    ): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<T>()
            .setInputData(scheduledBundle.toInputData())
            .addTag(scheduledBundle.notificationId.toString())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }

    suspend fun processWork(notificationId: Int, @StringRes contentTitleResId: Int) {
        val scheduledBundle = scheduledBundles[notificationId] ?: return

        val notification = buildUpdatedNotification(notificationId, contentTitleResId)!!
        notificationChannel.notify(notificationId, notification)

        scheduledBundle.workBundle.processUnit()
    }

    fun buildUpdatedNotification(notificationId: Int, @StringRes contentTitleResId: Int): Notification? {
        val scheduledBundle = scheduledBundles[notificationId] ?: return null
        var contentTitle = appContext.getString(contentTitleResId)

        // TODO: This implementation could allow race conditions?
        val (size, progress) = Pair(scheduledBundle.workBundle.size, scheduledBundle.workBundle.progress.value)
        if (size > 0 && progress > 0) {
            contentTitle = "$contentTitle ($progress/$size)"
            scheduledBundle.notificationBuilder.setProgress(size, progress, false)
        }

        scheduledBundle.notificationBuilder.setContentTitle(contentTitle)
        return scheduledBundle.notificationBuilder.build()
    }

    fun finishNotification(notificationId: Int) {
        val scheduledBundle = scheduledBundles[notificationId]

        if (scheduledBundle?.metadata?.notificationFinishTitle == null) {
            notificationChannel.cancel(notificationId)
            return
        }

        val notification = scheduledBundle.notificationBuilder.apply {
            setContentTitle(appContext.getString(scheduledBundle.metadata.notificationFinishTitle))
            setProgress(0, 0, false)
            setOngoing(false)
            setAutoCancel(true)

            scheduledBundle.metadata.notificationAction?.let { intent ->
                val pendingIntent = PendingIntent.getActivity(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                setContentIntent(pendingIntent)

                scheduledBundle.metadata.notificationActionText?.let { actionText ->
                    addAction(R.drawable.ic_truvark, appContext.getString(actionText), pendingIntent)
                }
            }
        }.build()
        notificationChannel.notify(notificationId, notification)
    }

    data class AndroidSchedulerMetadata(
        @StringRes val notificationTitle: Int,
        @StringRes val notificationFinishTitle: Int? = null,
        val notificationAction: Intent? = null,
        @StringRes val notificationActionText: Int? = null,
    ) : SchedulerMetadata

    private data class ScheduledBundle(
        val notificationId: Int,
        val metadata: AndroidSchedulerMetadata,
        val workBundle: WorkBundle,
        val notificationBuilder: NotificationCompat.Builder,
    ) {
        fun toInputData(): Data {
            return UniversalWorker.createInputData(notificationId, metadata.notificationTitle)
        }
    }
}
