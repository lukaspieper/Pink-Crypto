/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.test.fakes.SchedulerFake
import de.lukaspieper.truvark.common.test.helpers.VaultBase
import de.lukaspieper.truvark.common.test.helpers.findCipherFileEntityByName
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EncryptFileTests : VaultBase() {

    private val anyData = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,any.file"
    )
    fun `EncryptFile successfully encrypts file and create entry in Realm database`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val destinationFolder = insertCipherFolderEntity(folderId)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)

        // Act
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, destinationFolder)

        // Assert
        val resultEntities = vault.realm.findCipherFileEntityByName(fileDisplayName)
        assertAll(
            { assertDoesNotThrow { resultEntities.single() } },
            { assertTrue(vault.fileSystem.findFileInCipherDirectory(folderId, resultEntities.first().id) != null) },
            { assertTrue(fileSystem.exists(fileToEncrypt)) }
        )
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,any.file"
    )
    fun `EncryptFile succeeds and deletes source file`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val destinationFolder = insertCipherFolderEntity(folderId)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)

        // Act
        vault.scheduleEncryption(
            metadata = SchedulerFake.MetadataFake,
            sources = listOf { fileToEncrypt },
            destination = destinationFolder,
            deleteSources = true
        )

        // Assert
        val resultEntities = vault.realm.findCipherFileEntityByName(fileDisplayName)
        assertAll(
            { assertDoesNotThrow { resultEntities.single() } },
            { assertTrue(vault.fileSystem.findFileInCipherDirectory(folderId, resultEntities.first().id) != null) },
            { assertFalse(fileSystem.exists(fileToEncrypt)) }
        )
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,any.file"
    )
    fun `EncryptFile with unmanaged CipherFolderEntity throws IllegalArgumentException`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val destinationFolder = RealmCipherFolderEntity().apply { id = folderId }
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)

        // Act, Assert
        assertThrows<IllegalArgumentException> {
            vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, destinationFolder)
        }
    }
}
