/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.RootCipherFolderEntity
import de.lukaspieper.truvark.common.domain.vault.Vault
import de.lukaspieper.truvark.common.logging.LogPriority.ERROR
import de.lukaspieper.truvark.common.logging.LogPriority.INFO
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import io.realm.kotlin.MutableRealm

internal class FileRelocation(private val vault: Vault) {

    internal suspend fun relocateFolder(
        folder: RealmCipherFolderEntity,
        destinationFolder: CipherFolderEntity
    ) {
        vault.realm.write {
            val mutableFolder = findLatest(folder)!!
            mutableFolder.parentFolder = findDestinationFolder(destinationFolder)
        }
    }

    internal suspend fun relocateFile(file: RealmCipherFileEntity, destinationFolder: CipherFolderEntity) {
        // TODO: Switch order of operations to avoid unnecessary file system operations?!
        vault.fileSystem.relocateFileIntoCipherDirectory(file.id, file.folder!!.id, destinationFolder.id)

        try {
            vault.realm.write {
                val mutableFile = findLatest(file)!!
                mutableFile.folder = findDestinationFolder(destinationFolder)
            }
        } catch (e: Exception) {
            logcat(ERROR) { e.asLog() }

            logcat(INFO) { "File relocation failed. Restoring previous state." }
            vault.fileSystem.relocateFileIntoCipherDirectory(file.id, destinationFolder.id, file.folder!!.id)
        }
    }

    private fun MutableRealm.findDestinationFolder(folder: CipherFolderEntity): RealmCipherFolderEntity? {
        return when (folder) {
            is RootCipherFolderEntity -> null
            is RealmCipherFolderEntity -> findLatest(folder)!!
            else -> throw IllegalArgumentException("Unexpected folder type: ${folder::class.simpleName}")
        }
    }
}
