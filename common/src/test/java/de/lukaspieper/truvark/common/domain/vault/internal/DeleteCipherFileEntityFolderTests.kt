/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.test.fakes.SchedulerFake
import de.lukaspieper.truvark.common.test.helpers.VaultBase
import io.realm.kotlin.ext.query
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DeleteCipherFileEntityFolderTests : VaultBase() {

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `DeleteCipherFolderEntity with invalid folder throws IllegalStateException`(cipherFolderEntityId: String) {
        // Arrange
        val folder = RealmCipherFolderEntity().apply { id = cipherFolderEntityId }

        // Act, Assert
        assertThrows<IllegalStateException> {
            vault.scheduleDeletion(SchedulerFake.MetadataFake, emptyList(), listOf(folder))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `DeleteCipherFolderEntity without physical directory deletes realm entry`(cipherFolderEntityId: String) {
        // Arrange
        val folder = insertCipherFolderEntity(cipherFolderEntityId)

        // Act
        vault.scheduleDeletion(SchedulerFake.MetadataFake, emptyList(), listOf(folder))

        // Assert
        assertTrue(vault.realm.query<RealmCipherFileEntity>().find().isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `DeleteCipherFolderEntity without physical directory deletes realm entry including subfolders`(
        cipherFolderEntityId: String
    ) {
        // Arrange
        val folder = insertCipherFolderEntity(cipherFolderEntityId)
        repeat(3) { index ->
            insertCipherFolderEntity(cipherFolderEntityId + index, cipherFolderEntityId)
        }

        // Act
        vault.scheduleDeletion(SchedulerFake.MetadataFake, emptyList(), listOf(folder))

        // Assert
        assertTrue(vault.realm.query<RealmCipherFileEntity>().find().isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `DeleteCipherFolderEntity deletes realm entry including subfolders and physical directories`(
        cipherFolderEntityId: String
    ) {
        // Arrange
        var folder = insertCipherFolderEntity(cipherFolderEntityId)
        vault.fileSystem.createFileInCipherDirectory(cipherFolderEntityId, "any.file")
        repeat(3) { index ->
            insertCipherFolderEntity(cipherFolderEntityId + index, cipherFolderEntityId)
            vault.fileSystem.createFileInCipherDirectory(cipherFolderEntityId + index, "any.file")
        }

        // Refresh frozen folder object with updated one
        folder = runBlocking { vault.findCipherFolderEntity(folder.id) }

        // Act
        vault.scheduleDeletion(SchedulerFake.MetadataFake, emptyList(), listOf(folder))

        // Assert
        assertAll(
            { assertTrue(vault.realm.query<RealmCipherFileEntity>().find().isEmpty()) },
            { assertTrue(fileSystem.listDirectories(cipherFilesRootDirectory).isEmpty()) }
        )
    }
}
