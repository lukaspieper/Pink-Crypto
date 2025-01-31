/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.test.fakes.SchedulerFake
import de.lukaspieper.truvark.common.test.helpers.VaultBase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

internal class DecryptCipherFileEntityFolderTests : VaultBase() {

    private val anyData = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `DecryptCipherFolderEntity with invalid CipherFolderEntity throws IllegalStateException`(folderId: String) {
        // Arrange
        val parentFolder = runBlocking { vault.findCipherFolderEntity("") }
        val folder = RealmCipherFolderEntity().apply {
            id = folderId
            displayName = "any_display_name"
        }

        // Act, Assert
        assertThrows<IllegalStateException> {
            vault.scheduleDecryption(SchedulerFake.MetadataFake, parentFolder, emptyList(), listOf(folder))
        }
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,anyFileDisplayName"
    )
    fun `DecryptCipherFolderEntity with subfolder-less CipherFolderEntity decrypts successfully`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val parentFolder = runBlocking { vault.findCipherFolderEntity("") }
        val folder = insertCipherFolderEntity(folderId)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, folder)

        // Act
        vault.scheduleDecryption(SchedulerFake.MetadataFake, parentFolder, emptyList(), listOf(folder))

        // Assert
        val destinationDirectory = fileSystem.findDirectoryOrNull(vault.fileSystem.decryptionRootDirectory, folderId)
        val destinationFile = fileSystem.findFileOrNull(destinationDirectory!!, fileDisplayName)
        assertArrayEquals(anyData, fileSystem.readBytes(destinationFile!!))
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,anyFileDisplayName"
    )
    fun `DecryptCipherFolderEntity with CipherFolderEntity having subfolders decrypts successfully`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val parentFolder = runBlocking { vault.findCipherFolderEntity("") }
        var folder = insertCipherFolderEntity(folderId)
        val subfolder = insertCipherFolderEntity("subfolder", folderId)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, subfolder)

        // Refresh frozen folder object with updated one
        folder = runBlocking { vault.findCipherFolderEntity(folderId) }

        // Act
        vault.scheduleDecryption(SchedulerFake.MetadataFake, parentFolder, emptyList(), listOf(folder))

        // Assert
        val foldersDirectory = fileSystem.findDirectoryOrNull(vault.fileSystem.decryptionRootDirectory, folderId)
        val subfoldersDirectory = fileSystem.findDirectoryOrNull(foldersDirectory!!, subfolder.id)
        val destinationFile = fileSystem.findFileOrNull(subfoldersDirectory!!, fileDisplayName)
        assertArrayEquals(anyData, fileSystem.readBytes(destinationFile!!))
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,anyFileDisplayName"
    )
    fun `DecryptCipherFolderEntity targeting subfolder decrypts successfully`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val folder = insertCipherFolderEntity(folderId)
        val subfolder = insertCipherFolderEntity("subfolder", folderId)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, subfolder)

        // Act
        vault.scheduleDecryption(SchedulerFake.MetadataFake, folder, emptyList(), listOf(subfolder))

        // Assert
        val foldersDirectory = fileSystem.findDirectoryOrNull(vault.fileSystem.decryptionRootDirectory, folderId)
        val subfoldersDirectory = fileSystem.findDirectoryOrNull(foldersDirectory!!, subfolder.id)
        val destinationFile = fileSystem.findFileOrNull(subfoldersDirectory!!, fileDisplayName)
        assertArrayEquals(anyData, fileSystem.readBytes(destinationFile!!))
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,anyFileDisplayName"
    )
    fun `DecryptCipherFolderEntity targeting sub-subfolder decrypts successfully`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val folder = insertCipherFolderEntity(folderId)
        val subfolder1 = insertCipherFolderEntity("subfolder1", folder.id)
        val subfolder2 = insertCipherFolderEntity("subfolder2", subfolder1.id)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, subfolder2)

        // Act
        vault.scheduleDecryption(SchedulerFake.MetadataFake, subfolder1, emptyList(), listOf(subfolder2))

        // Assert
        val foldersDirectory = fileSystem.findDirectoryOrNull(vault.fileSystem.decryptionRootDirectory, folder.id)
        val subfolder1sDirectory = fileSystem.findDirectoryOrNull(foldersDirectory!!, subfolder1.id)
        val subfolder2sDirectory = fileSystem.findDirectoryOrNull(subfolder1sDirectory!!, subfolder2.id)
        val destinationFile = fileSystem.findFileOrNull(subfolder2sDirectory!!, fileDisplayName)
        assertArrayEquals(anyData, fileSystem.readBytes(destinationFile!!))
    }
}
