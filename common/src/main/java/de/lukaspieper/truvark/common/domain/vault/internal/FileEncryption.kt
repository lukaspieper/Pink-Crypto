/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import com.google.crypto.tink.StreamingAead
import de.lukaspieper.truvark.common.constants.FixedValues
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.domain.IdGenerator
import de.lukaspieper.truvark.common.domain.ThumbnailProvider
import de.lukaspieper.truvark.common.domain.entities.OriginalFileMetadata
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.domain.vault.VaultFileSystem
import de.lukaspieper.truvark.common.domain.vault.useEncryptingStream
import io.realm.kotlin.Realm
import io.realm.kotlin.types.RealmInstant
import java.util.Locale

internal class FileEncryption(
    private val streamingAead: StreamingAead,
    private val realm: Realm,
    private val fileSystem: VaultFileSystem,
    private val thumbnailProvider: ThumbnailProvider,
    private val idGenerator: IdGenerator,
) {
    internal suspend fun encryptFile(
        source: FileInfo,
        destinationFolder: RealmCipherFolderEntity,
        creationInstant: RealmInstant
    ): RealmCipherFileEntity {
        val destinationFile = createDestinationFile(destinationFolder)
        val cipherFileEntity = buildCipherFileEntityFromFile(source, destinationFile, creationInstant)

        encrypt(source, destinationFile, cipherFileEntity)

        // NOTE: It's important to return the managed instance
        return addCipherFileEntityToFolder(cipherFileEntity, destinationFolder)
    }

    private fun createDestinationFile(destinationFolder: RealmCipherFolderEntity): FileInfo {
        var targetFileName: String

        do {
            targetFileName = idGenerator.createStringId(FixedValues.FILENAME_LENGTH)
        } while (fileSystem.findFileInCipherDirectory(destinationFolder.id, targetFileName) != null)

        return fileSystem.createFileInCipherDirectory(destinationFolder.id, targetFileName)
    }

    private suspend fun buildCipherFileEntityFromFile(
        source: FileInfo,
        destinationFile: FileInfo,
        creationInstant: RealmInstant
    ): RealmCipherFileEntity {
        return RealmCipherFileEntity().apply {
            id = destinationFile.fullName
            mimeType = source.mimeType.lowercase(Locale.getDefault())
            fileSize = source.size
            thumbnail = thumbnailProvider.createThumbnail(source)

            name = source.name
            fileExtension = source.extension

            creationDate = creationInstant
            mediaDurationSeconds = source.mediaDuration?.inWholeSeconds
        }
    }

    private fun encrypt(sourceFile: FileInfo, destinationFile: FileInfo, metadata: OriginalFileMetadata) {
        val metadataBytes = metadata.toJson().toByteArray()
        require(metadataBytes.size <= FixedValues.ENCRYPTED_FILE_HEADER_SIZE)

        val headerBytes = ByteArray(FixedValues.ENCRYPTED_FILE_HEADER_SIZE)
        metadataBytes.copyInto(headerBytes)

        streamingAead.useEncryptingStream(fileSystem.openOutputStream(destinationFile)) { encryptingOutputStream ->
            fileSystem.openInputStream(sourceFile).use { inputStream ->
                encryptingOutputStream.write(headerBytes)
                inputStream.copyTo(encryptingOutputStream)
            }
        }
    }

    private suspend fun addCipherFileEntityToFolder(
        cipherFileEntity: RealmCipherFileEntity,
        folder: RealmCipherFolderEntity
    ): RealmCipherFileEntity {
        return realm.write {
            val mutableFolder = findLatest(folder)
            cipherFileEntity.folder = mutableFolder

            copyToRealm(cipherFileEntity)
        }
    }
}
