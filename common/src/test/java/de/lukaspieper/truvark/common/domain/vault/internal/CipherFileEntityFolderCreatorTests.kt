/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault.internal

import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.RootCipherFolderEntity
import de.lukaspieper.truvark.common.domain.findCipherFolderEntity
import de.lukaspieper.truvark.common.test.fakes.IdGeneratorFake
import de.lukaspieper.truvark.common.test.helpers.VaultBase
import io.realm.kotlin.ext.query
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CipherFileEntityFolderCreatorTests : VaultBase() {

    private val anyId = "2wIq7Q4TxqBN7igo"
    private val anyDisplayName = "FolderName"
    private val differentId = "5wIqHQ4JUZNZ7igo"
    private val differentDisplayName = "differentDisplayName"

    @ParameterizedTest
    @ValueSource(strings = ["", "    "])
    fun `CreateFolder throws IllegalArgumentException because of blank name`(displayName: String) {
        // Act, Assert
        assertThrows<IllegalArgumentException> {
            runBlocking {
                vault.createFolder(displayName, RootCipherFolderEntity("any display name"))
            }
        }
    }

    @Test
    fun `CreateFolder inserts a new CipherFolderEntity into Realm`() {
        // Arrange
        val folderCreator = CipherFolderEntityCreator(
            vault.realm,
            IdGeneratorFake(arrayOf(anyId))
        )

        // Act
        runBlocking {
            folderCreator.createFolder(anyDisplayName, RootCipherFolderEntity("any display name"))
        }

        // Assert
        val cipherFolderEntity = vault.realm.query<RealmCipherFolderEntity>().find().single()
        assertAll(
            { assertEquals(anyId, cipherFolderEntity.id) },
            { assertEquals(anyDisplayName, cipherFolderEntity.displayName) },
        )
    }

    @Test
    fun `CreateFolder repeats until generated id is unique and creates two CipherFolderEntitys`() {
        // Arrange
        val folderCreator = CipherFolderEntityCreator(
            vault.realm,
            IdGeneratorFake(arrayOf(anyId, anyId, differentId))
        )

        // Act
        runBlocking {
            folderCreator.createFolder(anyDisplayName, RootCipherFolderEntity("any display name"))
            folderCreator.createFolder(differentDisplayName, RootCipherFolderEntity("any display name"))
        }

        // Assert
        val cipherFolderEntities = vault.realm.query<RealmCipherFolderEntity>().find()
        assertAll(
            { assertEquals(anyDisplayName, cipherFolderEntities.single { it.id == anyId }.displayName) },
            { assertEquals(differentDisplayName, cipherFolderEntities.single { it.id == differentId }.displayName) },
        )
    }

    @Test
    fun `CreateFolder successfully creates sub folder`() {
        // Arrange
        val folderCreator = CipherFolderEntityCreator(
            vault.realm,
            IdGeneratorFake(arrayOf(anyId, differentId))
        )

        // Act
        runBlocking {
            folderCreator.createFolder(anyDisplayName, RootCipherFolderEntity("any display name"))
            folderCreator.createFolder(differentDisplayName, vault.realm.findCipherFolderEntity(anyId))
        }

        // Assert
        val cipherFolderEntities = vault.realm.query<RealmCipherFolderEntity>().find()
        assertAll(
            { assertEquals(anyId, cipherFolderEntities.single { it.id == differentId }.parentFolder?.id) },
        )
    }
}
