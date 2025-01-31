/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.data.database

import de.lukaspieper.truvark.R
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.data.io.FileSystem
import de.lukaspieper.truvark.work.DatabaseSyncingWorker
import de.lukaspieper.truvark.work.WorkScheduler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Because there is no database implementation with SAF, the database in the vault directory is copied to the internal
 * storage for direct access. As a consequence both database files must be synchronized before usage and after
 * modifications.
 *
 * This class handles the synchronization before usage.
 */
class DatabaseFileSynchronization(
    private val workScheduler: WorkScheduler,
    private val fileSystem: FileSystem
) {
    @Throws(IllegalStateException::class)
    fun synchronizeDatabaseFiles(vaultDatabaseFile: FileInfo, internalDatabaseFile: File) {
        when {
            vaultDatabaseFileNeedsRecovery(vaultDatabaseFile, internalDatabaseFile) -> exportInternalDatabaseFile()

            canImportVaultDatabaseFile(vaultDatabaseFile, internalDatabaseFile) -> importVaultDatabaseFile(
                vaultDatabaseFile,
                internalDatabaseFile
            )

            else -> error("Database is not available")
        }
    }

    private fun vaultDatabaseFileNeedsRecovery(vaultDatabaseFile: FileInfo, internalDatabaseFile: File): Boolean {
        return vaultDatabaseFile.size == 0L && internalDatabaseFile.length() > 0L
    }

    private fun canImportVaultDatabaseFile(vaultDatabaseFile: FileInfo, internalDatabaseFile: File): Boolean {
        return vaultDatabaseFile.size > 0L && internalDatabaseFile.length() >= 0L
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun exportInternalDatabaseFile() {
        GlobalScope.launch {
            // TODO: It can happen that the synchronization starts before the vault creation is finished. In this case,
            //  the app will crash. The delay is a workaround for this issue.
            delay(2000)

            workScheduler.schedule(
                DatabaseSyncingWorker.EmptyWorkBundle(),
                WorkScheduler.AndroidSchedulerMetadata(R.string.sync_database)
            )
        }
    }

    private fun importVaultDatabaseFile(vaultDatabaseFile: FileInfo, internalDatabaseFile: File) {
        internalDatabaseFile.parentFile!!.mkdirs()

        fileSystem.openInputStream(vaultDatabaseFile).use { inputStream ->
            internalDatabaseFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}
