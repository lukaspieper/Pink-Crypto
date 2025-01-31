/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.domain.findCipherFileEntityOrNull
import de.lukaspieper.truvark.common.test.fakes.SchedulerFake
import de.lukaspieper.truvark.common.test.helpers.VaultBase
import de.lukaspieper.truvark.common.test.helpers.findCipherFileEntityByName
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

internal class DecryptCipherFileEntityTests : VaultBase() {

    private val anyData = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)

    @ParameterizedTest
    @ValueSource(strings = ["any_file_id", "ZWJDI5432"])
    fun `DecryptCipherFileEntity without physical file throws Exception`(cipherFileEntityId: String) {
        // Arrange
        val parentFolder = RealmCipherFolderEntity()
        val cipherFileEntity = RealmCipherFileEntity().apply {
            id = cipherFileEntityId
            folder = parentFolder
        }

        // Act, Assert
        assertThrows<Exception> {
            vault.scheduleDecryption(SchedulerFake.MetadataFake, parentFolder, listOf(cipherFileEntity), emptyList())
        }
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,anyFileDisplayName"
    )
    fun `DecryptCipherFileEntity uses database entry for successful decryption`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val parentFolder = insertCipherFolderEntity(folderId)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, parentFolder)
        val cipherFileEntity = vault.realm.findCipherFileEntityByName(fileDisplayName).single()

        // Act
        vault.scheduleDecryption(SchedulerFake.MetadataFake, parentFolder, listOf(cipherFileEntity), emptyList())

        // Assert
        val destinationDirectory = fileSystem.findDirectoryOrNull(vault.fileSystem.decryptionRootDirectory, folderId)
        val destinationFile = fileSystem.findFileOrNull(destinationDirectory!!, fileDisplayName)
        val actualContent = fileSystem.readBytes(destinationFile!!)
        assertArrayEquals(anyData, actualContent)
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,anyFileDisplayName"
    )
    fun `DecryptCipherFileEntity uses encrypted file's header for successful decryption`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val parentFolder = insertCipherFolderEntity(folderId)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, parentFolder)
        val cipherFileEntity = vault.realm.findCipherFileEntityByName(fileDisplayName).single()

        vault.realm.writeBlocking {
            findCipherFileEntityOrNull(cipherFileEntity.id)?.let { delete(it) }
        }

        // Act
        vault.scheduleDecryption(SchedulerFake.MetadataFake, parentFolder, listOf(cipherFileEntity), emptyList())

        // Assert
        val destinationDirectory = fileSystem.findDirectoryOrNull(vault.fileSystem.decryptionRootDirectory, folderId)
        val destinationFile = fileSystem.findFileOrNull(destinationDirectory!!, fileDisplayName)
        val actualContent = fileSystem.readBytes(destinationFile!!)
        assertArrayEquals(anyData, actualContent)
    }
}
