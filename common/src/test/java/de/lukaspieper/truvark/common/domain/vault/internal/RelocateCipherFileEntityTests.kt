/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.test.fakes.SchedulerFake
import de.lukaspieper.truvark.common.test.helpers.VaultBase
import de.lukaspieper.truvark.common.test.helpers.findCipherFileEntityByName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RelocateCipherFileEntityTests : VaultBase() {

    private val anyData = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,anyFileDisplayName"
    )
    fun `ScheduleRelocation with single file`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val parentFolder = insertCipherFolderEntity(folderId)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, parentFolder)
        val cipherFileEntity = vault.realm.findCipherFileEntityByName(fileDisplayName).single()
        val destinationFolder = insertCipherFolderEntity("destination")

        // Act
        vault.scheduleRelocation(SchedulerFake.MetadataFake, destinationFolder, listOf(cipherFileEntity), emptyList())

        // Assert
        val destinationFileEntities = runBlocking {
            vault.findCipherFileEntitiesForFolder(destinationFolder.id).first()
        }
        val originFileEntities = runBlocking { vault.findCipherFileEntitiesForFolder(parentFolder.id).first() }

        val destinationFileInfo = vault.fileSystem.findFileInCipherDirectory(destinationFolder.id, cipherFileEntity.id)
        val originFileInfo = vault.fileSystem.findFileInCipherDirectory(parentFolder.id, cipherFileEntity.id)

        assertAll(
            { assertTrue(destinationFileEntities.any { it.fullName() == fileDisplayName }) },
            { assertTrue(originFileEntities.none { it.fullName() == fileDisplayName }) },
            { assertNotNull(destinationFileInfo) },
            { assertNull(originFileInfo) }
        )
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,anyFileDisplayName",
        "jdkIken678J,anyFileDisplayName"
    )
    fun `ScheduleRelocation with failing database operation reverts changes`(
        folderId: String,
        fileDisplayName: String
    ) {
        // Arrange
        val parentFolder = insertCipherFolderEntity(folderId)
        val fileToEncrypt = createFileInTempDirectory(fileDisplayName, anyData)
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { fileToEncrypt }, parentFolder)
        val cipherFileEntity = vault.realm.findCipherFileEntityByName(fileDisplayName).single()
        val destinationFolder = RealmCipherFolderEntity().apply { id = "destination" }

        // Act
        vault.scheduleRelocation(SchedulerFake.MetadataFake, destinationFolder, listOf(cipherFileEntity), emptyList())

        // Assert
        val destinationFileEntities =
            runBlocking { vault.findCipherFileEntitiesForFolder(destinationFolder.id).first() }
        val originFileEntities = runBlocking { vault.findCipherFileEntitiesForFolder(parentFolder.id).first() }

        val destinationFileInfo = vault.fileSystem.findFileInCipherDirectory(destinationFolder.id, cipherFileEntity.id)
        val originFileInfo = vault.fileSystem.findFileInCipherDirectory(parentFolder.id, cipherFileEntity.id)

        assertAll(
            { assertTrue(destinationFileEntities.none { it.fullName() == fileDisplayName }) },
            { assertTrue(originFileEntities.any { it.fullName() == fileDisplayName }) },
            { assertNull(destinationFileInfo) },
            { assertNotNull(originFileInfo) }
        )
    }
}
