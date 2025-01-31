/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import com.google.crypto.tink.StreamingAead
import de.lukaspieper.truvark.common.constants.FixedValues
import de.lukaspieper.truvark.common.data.io.DirectoryInfo
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.domain.entities.CipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.OriginalFileMetadata
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.RootCipherFolderEntity
import de.lukaspieper.truvark.common.domain.vault.VaultFileSystem
import de.lukaspieper.truvark.common.domain.vault.useDecryptingStream
import de.lukaspieper.truvark.common.logging.LogPriority.DEBUG
import de.lukaspieper.truvark.common.logging.LogPriority.INFO
import de.lukaspieper.truvark.common.logging.LogPriority.WARN
import de.lukaspieper.truvark.common.logging.logcat
import java.io.InputStream
import kotlin.system.measureTimeMillis

internal class FileDecryption(
    private val fileSystem: VaultFileSystem,
    private val streamingAead: StreamingAead
) {

    /**
     * Returns a [DirectoryInfo] that matches the folder hierarchy of the given [folder]. All parent directories will be
     * created if they do not exist yet. [VaultFileSystem.decryptionRootDirectory] is used as root directory.
     *
     * **NOTE: [folder] must be the parent folder of the files and folders that should be decrypted in the next step.**
     */
    internal fun findOrCreateDecryptionDestinationDirectory(folder: CipherFolderEntity): DirectoryInfo {
        return when {
            folder is RootCipherFolderEntity -> fileSystem.decryptionRootDirectory
            folder is RealmCipherFolderEntity && folder.parentFolder != null -> {
                val directory = findOrCreateDecryptionDestinationDirectory(folder.parentFolder!!)
                fileSystem.findOrCreateDirectory(directory, folder.displayName)
            }

            else -> fileSystem.findOrCreateDirectory(fileSystem.decryptionRootDirectory, folder.displayName)
        }
    }

    /**
     * Finds the physical file for [cipherFileEntity]. Reads and decrypts all bytes from it and writes them to a new
     * file on disk. The new file is located in [destinationDirectory]. The primary source for the name of the decrypted
     * file is [cipherFileEntity], in case that is not available the name will be taken from the encrypted file's header
     * (see [OriginalFileMetadata]. As a final fallback the name (id) of the encrypted file will be used.
     */
    internal fun decryptFile(
        cipherFileEntity: CipherFileEntity,
        destinationDirectory: DirectoryInfo
    ) {
        val cipherFile = fileSystem.findFileInCipherDirectory(cipherFileEntity.folder!!.id, cipherFileEntity.id)!!
        decryptToFile(cipherFile, cipherFileEntity, destinationDirectory)
    }

    /**
     * Decrypts all files in [cipherFolderEntity] and all its subfolders recursively. The matching directory hierarchy
     * will be created in [parentDecryptionDirectory].
     */
    internal fun decryptFoldersRecursively(
        cipherFolderEntity: RealmCipherFolderEntity,
        parentDecryptionDirectory: DirectoryInfo
    ) {
        val currentDecryptionDirectory = fileSystem.findOrCreateDirectory(
            directoryInfo = parentDecryptionDirectory,
            name = cipherFolderEntity.displayName
        )

        cipherFolderEntity.subfolders.forEach { subfolder ->
            decryptFoldersRecursively(subfolder, currentDecryptionDirectory)
        }

        decryptDirectory(cipherFolderEntity, currentDecryptionDirectory)
    }

    private fun decryptDirectory(cipherFolderEntity: CipherFolderEntity, decryptionDirectory: DirectoryInfo) {
        fileSystem.fetchFilesFromCipherDirectory(cipherFolderEntity.id).forEach { file ->
            // TODO: Get and pass down the database entry
            decryptToFile(file, null, decryptionDirectory)
        }
    }

    private fun decryptToFile(
        cipherFile: FileInfo,
        cipherFileEntity: CipherFileEntity?,
        destinationDirectory: DirectoryInfo
    ) {
        logcat(INFO) { "Start decrypting cipher file to disk." }

        streamingAead.useDecryptingStream(fileSystem.openInputStream(cipherFile)) { decryptingInputStream ->
            val elapsedMilliseconds = measureTimeMillis {
                val originalFileMetadata = decryptOriginalFileMetadata(decryptingInputStream)
                val destinationFile = createAppropriateDestinationFile(
                    destinationDir = destinationDirectory,
                    cipherFileEntity = cipherFileEntity,
                    originalFileMetadata = originalFileMetadata,
                    fallbackFileName = cipherFile.fullName
                )

                fileSystem.openOutputStream(destinationFile).use { outputStream ->
                    decryptingInputStream.copyTo(outputStream)
                }
            }

            logcat(INFO) { "Decryption to disk finished in $elapsedMilliseconds milliseconds." }
        }
    }

    private fun decryptOriginalFileMetadata(decryptingStream: InputStream): OriginalFileMetadata? {
        val headerBytes = ByteArray(FixedValues.ENCRYPTED_FILE_HEADER_SIZE)
        val readBytes = decryptingStream.read(headerBytes)

        return if (readBytes == FixedValues.ENCRYPTED_FILE_HEADER_SIZE) {
            OriginalFileMetadata.fromJsonOrNull(String(headerBytes))
        } else {
            null
        }
    }

    private fun createAppropriateDestinationFile(
        destinationDir: DirectoryInfo,
        cipherFileEntity: CipherFileEntity?,
        originalFileMetadata: OriginalFileMetadata?,
        fallbackFileName: String
    ): FileInfo {
        return when {
            cipherFileEntity?.name?.isNotBlank() == true -> {
                logcat(DEBUG) { "Using database entry for decryption file name." }
                fileSystem.createFile(destinationDir, cipherFileEntity.name, cipherFileEntity.mimeType)
            }

            originalFileMetadata?.name?.isNotBlank() == true -> {
                logcat(DEBUG) { "Using encrypted file header for decryption file name." }
                fileSystem.createFile(destinationDir, originalFileMetadata.name, originalFileMetadata.mimeType)
            }

            else -> {
                logcat(WARN) { "Neither database entry nor encrypted file header could be used! Using fallback name." }
                fileSystem.createFile(destinationDir, fallbackFileName)
            }
        }
    }
}
