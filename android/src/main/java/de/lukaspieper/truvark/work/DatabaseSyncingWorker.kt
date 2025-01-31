/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.lukaspieper.truvark.common.constants.FileNames
import de.lukaspieper.truvark.common.domain.vault.Vault
import de.lukaspieper.truvark.common.logging.LogPriority.INFO
import de.lukaspieper.truvark.common.logging.logcat
import de.lukaspieper.truvark.common.work.WorkBundle
import de.lukaspieper.truvark.data.database.DatabaseFileSynchronization
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Because there is no database implementation with SAF, the database in the vault directory is copied to the internal
 * storage for direct access. As a consequence both database files must be synchronized before usage and after
 * modifications.
 *
 * This worker handles the synchronization after modifications while [DatabaseFileSynchronization] is responsible for
 * the synchronization before usage.
 *
 * NOTE: Make sure that this worker always runs sequentially (e. g. by using unique work).
 */
@HiltWorker
class DatabaseSyncingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workScheduler: WorkScheduler,
    private val vault: Vault
) : UniversalWorker(appContext, workerParams, workScheduler) {

    /**
     * Safely creates an encrypted copy of the internal database in the internal cache directory while the database is
     * in use. Then the database in the vault directory will be overwritten with the new database copy.
     *
     * This workaround is required because Realm does not support SAF/OutputStream
     * ([issue #289](https://github.com/realm/realm-kotlin/issues/289)).
     */
    override suspend fun tryDoWork() {
        // Force updating the notification as it might not run as expedited work
        workScheduler.buildUpdatedNotification(notificationId, contentTitleResId)

        val cacheDatabaseFile = applicationContext.cacheDir.resolve(FileNames.INDEX_REALM)

        vault.writeEncryptedDatabaseCopyTo(cacheDatabaseFile)
        cacheDatabaseFile.inputStream().use { inputStream ->
            vault.fileSystem.openOutputStream(vault.fileSystem.databaseFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        logcat(INFO) { "Database synchronization finished." }
    }

    override fun onStopped() {
        workScheduler.finishNotification(notificationId)
    }

    /**
     * An empty [WorkBundle] that can be used to trigger [DatabaseSyncingWorker] by sending it to
     * [WorkScheduler.schedule]. This will cause one unnecessary job to run, however, the job will return immediately.
     */
    class EmptyWorkBundle : WorkBundle(size = 1) {
        override val progress = MutableStateFlow(0)

        override suspend fun processUnit() {
            // Not incrementing the progress because the notification progress bar would switch from indeterminate and
            // the user should not be confused by this internal implementation detail.
        }
    }
}
