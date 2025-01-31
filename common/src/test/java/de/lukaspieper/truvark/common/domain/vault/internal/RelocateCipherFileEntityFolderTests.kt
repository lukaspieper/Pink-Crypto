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
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RelocateCipherFileEntityFolderTests : VaultBase() {

    private val anyData = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)

    @Test
    fun `ScheduleRelocation without any entities throws IllegalArgumentException`() {
        // Arrange
        val destinationFolder = insertCipherFolderEntity("destination")

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            vault.scheduleRelocation(SchedulerFake.MetadataFake, destinationFolder, emptyList(), emptyList())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `ScheduleRelocation with unmanaged destination throws IllegalArgumentException`(folderId: String) {
        // Arrange
        val folderToRelocate = insertCipherFolderEntity(folderId)
        val destinationFolder = RealmCipherFolderEntity().apply {
            id = "unmanaged_destination"
            displayName = "any_display_name"
        }

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            vault.scheduleRelocation(
                SchedulerFake.MetadataFake,
                destinationFolder,
                emptyList(),
                listOf(folderToRelocate)
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `ScheduleRelocation with unmanaged folder to relocate throws IllegalArgumentException`(folderId: String) {
        // Arrange
        val folderToRelocate = RealmCipherFolderEntity().apply {
            id = folderId
            displayName = "name_$folderId"
        }
        val destinationFolder = insertCipherFolderEntity("destination")

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            vault.scheduleRelocation(
                SchedulerFake.MetadataFake,
                destinationFolder,
                emptyList(),
                listOf(folderToRelocate)
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `ScheduleRelocation moves empty folder to destination folder`(folderId: String) {
        // Arrange
        val folderToRelocate = insertCipherFolderEntity(folderId)
        val destinationFolder = insertCipherFolderEntity("destination")

        // Act
        vault.scheduleRelocation(SchedulerFake.MetadataFake, destinationFolder, emptyList(), listOf(folderToRelocate))

        // Assert
        val subFolders = runBlocking { vault.findCipherFileEntitySubFolders(destinationFolder.id).first() }
        assertTrue(subFolders.any { it.id == folderToRelocate.id })
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `ScheduleRelocation moves empty folder to root folder`(folderId: String) {
        // Arrange
        val parentFolder = insertCipherFolderEntity("parent")
        val folderToRelocate = insertCipherFolderEntity(folderId, parentFolder.id)
        val rootFolder = runBlocking { vault.findCipherFolderEntity("") }

        // Act
        vault.scheduleRelocation(SchedulerFake.MetadataFake, rootFolder, emptyList(), listOf(folderToRelocate))

        // Assert
        val subFolders = runBlocking { vault.findCipherFileEntitySubFolders(rootFolder.id).first() }
        assertTrue(subFolders.any { it.id == parentFolder.id })
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `ScheduleRelocation moves folder with subfolders to destination folder`(folderId: String) {
        // Arrange
        val folderToRelocate = insertCipherFolderEntity(folderId)
        val subfolder = insertCipherFolderEntity("subfolder", folderId)
        val destinationFolder = insertCipherFolderEntity("destination")

        // Act
        vault.scheduleRelocation(SchedulerFake.MetadataFake, destinationFolder, emptyList(), listOf(folderToRelocate))

        // Assert
        val destinationSubFolders = runBlocking { vault.findCipherFileEntitySubFolders(destinationFolder.id).first() }
        val folderToRelocateSubFolders = runBlocking {
            vault.findCipherFileEntitySubFolders(folderToRelocate.id).first()
        }

        assertAll(
            { assertTrue(destinationSubFolders.any { it.id == folderToRelocate.id }) },
            { assertTrue(folderToRelocateSubFolders.any { it.id == subfolder.id }) }
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `ScheduleRelocation moves folder with file to destination folder`(folderId: String) {
        // Arrange
        val folderToRelocate = insertCipherFolderEntity(folderId)
        val destinationFolder = insertCipherFolderEntity("destination")
        val file = createFileInTempDirectory("file", anyData)
        vault.scheduleEncryption(SchedulerFake.MetadataFake, listOf { file }, folderToRelocate)

        // Act
        vault.scheduleRelocation(SchedulerFake.MetadataFake, destinationFolder, emptyList(), listOf(folderToRelocate))

        // Assert
        val cipherFileEntity = vault.realm.findCipherFileEntityByName("file").single()
        val destinationSubFolders = runBlocking { vault.findCipherFileEntitySubFolders(destinationFolder.id).first() }
        val folderToRelocateFiles = runBlocking { vault.findCipherFileEntitiesForFolder(folderToRelocate.id).first() }
        assertAll(
            { assertTrue(destinationSubFolders.any { it.id == folderToRelocate.id }) },
            { assertTrue(folderToRelocateFiles.any { it.id == cipherFileEntity.id }) }
        )
    }
}
