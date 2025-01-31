/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.test.fakes.SchedulerFake
import de.lukaspieper.truvark.common.test.helpers.VaultBase
import io.realm.kotlin.ext.query
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class DeleteCipherFileEntityTests : VaultBase() {

    @ParameterizedTest
    @ValueSource(strings = ["any_file_id", "ZWJDI5432"])
    fun `DeleteCipherFileEntity without physical file deletes realm entry`(cipherFileEntityId: String) {
        // Arrange
        val cipherFileEntity = insertCipherFileEntity(cipherFileEntityId)

        // Act
        vault.scheduleDeletion(SchedulerFake.MetadataFake, listOf(cipherFileEntity), emptyList())

        // Assert
        assertTrue(vault.realm.query<RealmCipherFileEntity>().find().isEmpty())
    }

    @ParameterizedTest
    @CsvSource(
        "anyFolderId,any_file_id",
        "jdkIken678J,ZWJDI5432"
    )
    fun `DeleteCipherFileEntity deletes realm entry and physical file`(folderId: String, fileId: String) {
        // Arrange
        val cipherFolderEntity = insertCipherFolderEntity(folderId)
        val cipherFileEntity = insertCipherFileEntity(fileId, cipherFolderEntity.id)
        val file = vault.fileSystem.createFileInCipherDirectory(cipherFolderEntity.id, cipherFileEntity.id)

        // Act
        vault.scheduleDeletion(SchedulerFake.MetadataFake, listOf(cipherFileEntity), emptyList())

        // Assert
        assertAll(
            { assertTrue(vault.realm.query<RealmCipherFileEntity>().find().isEmpty()) },
            { assertFalse(fileSystem.exists(file)) }
        )
    }
}
