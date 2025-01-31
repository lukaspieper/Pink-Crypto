/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.domain.vault.Vault

internal class FileDeletion(private val vault: Vault) {

    internal suspend fun deleteFoldersRecursively(cipherFolderEntity: RealmCipherFolderEntity) {
        cipherFolderEntity.subfolders.forEach { subfolder ->
            deleteFoldersRecursively(subfolder)
        }

        deleteFolder(cipherFolderEntity)
    }

    private suspend fun deleteFolder(cipherFolderEntity: RealmCipherFolderEntity) {
        vault.realm.write {
            findLatest(cipherFolderEntity)?.let { folder ->
                folder.files.forEach { delete(it) }
                delete(folder)
            }
        }

        vault.fileSystem.deleteCipherDirectory(cipherFolderEntity.id)
    }

    internal suspend fun deleteFile(cipherFileEntity: RealmCipherFileEntity) {
        cipherFileEntity.folder?.let { folder ->
            vault.fileSystem.deleteFileFromCipherDirectory(folder.id, cipherFileEntity.id)
        }

        // Deleting the database entry no matter whether the file exists because the entry would be invalid in any case
        vault.realm.write {
            findLatest(cipherFileEntity)?.let { delete(it) }
        }
    }
}
