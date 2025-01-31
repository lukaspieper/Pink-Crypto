/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.work

import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.data.io.FileSystem
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.domain.vault.internal.FileDecryption
import de.lukaspieper.truvark.common.domain.vault.internal.FileDeletion
import de.lukaspieper.truvark.common.domain.vault.internal.FileEncryption
import de.lukaspieper.truvark.common.domain.vault.internal.FileRelocation
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate

public abstract class WorkBundle(
    public var size: Int
) {
    public abstract val progress: StateFlow<Int>

    public abstract suspend fun processUnit()

    internal class EncryptingWorkBundle(
        private val fileEncryption: FileEncryption,
        private val fileSystem: FileSystem,
        private val sources: List<() -> FileInfo>,
        private val destination: RealmCipherFolderEntity,
        private val deleteSources: Boolean
    ) : WorkBundle(
        size = sources.size
    ) {
        override val progress = MutableStateFlow(0)

        override suspend fun processUnit() {
            val source = sources[progress.getAndUpdate { it + 1 }]()
            fileEncryption.encryptFile(source, destination, RealmInstant.now())

            if (deleteSources) {
                fileSystem.delete(source)
            }
        }
    }

    internal class DecryptingWorkBundle(
        private val fileDecryption: FileDecryption,
        parentFolder: CipherFolderEntity,
        private val files: List<RealmCipherFileEntity>,
        private val folders: List<RealmCipherFolderEntity>
    ) : WorkBundle(
        size = files.size + folders.size
    ) {
        private val destinationDirectoryInfo by lazy {
            fileDecryption.findOrCreateDecryptionDestinationDirectory(parentFolder)
        }
        override val progress = MutableStateFlow(0)

        override suspend fun processUnit() {
            val index = progress.getAndUpdate { it + 1 }

            when {
                index < folders.size -> {
                    fileDecryption.decryptFoldersRecursively(folders[index], destinationDirectoryInfo)
                }

                index < size -> {
                    fileDecryption.decryptFile(files[index - folders.size], destinationDirectoryInfo)
                }
            }
        }
    }

    internal class DeletingWorkBundle(
        private val fileDeletion: FileDeletion,
        private val files: List<RealmCipherFileEntity>,
        private val folders: List<RealmCipherFolderEntity>
    ) : WorkBundle(
        size = files.size + folders.size
    ) {
        override val progress = MutableStateFlow(0)

        override suspend fun processUnit() {
            val index = progress.getAndUpdate { it + 1 }

            when {
                index < folders.size -> fileDeletion.deleteFoldersRecursively(folders[index])
                index < size -> fileDeletion.deleteFile(files[index - folders.size])
            }
        }
    }

    internal class RelocatingWorkBundle(
        private val fileRelocation: FileRelocation,
        private val destination: CipherFolderEntity,
        private val files: List<RealmCipherFileEntity>,
        private val folders: List<RealmCipherFolderEntity>
    ) : WorkBundle(
        size = files.size + folders.size
    ) {
        override val progress = MutableStateFlow(0)

        override suspend fun processUnit() {
            val index = progress.getAndUpdate { it + 1 }

            when {
                index < folders.size -> fileRelocation.relocateFolder(folders[index], destination)
                index < size -> fileRelocation.relocateFile(files[index - folders.size], destination)
            }
        }
    }
}
