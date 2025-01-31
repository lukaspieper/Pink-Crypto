/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault

import de.lukaspieper.truvark.common.constants.FileNames
import de.lukaspieper.truvark.common.constants.FixedValues.MAX_VAULT_NAME_LENGTH
import de.lukaspieper.truvark.common.test.helpers.VaultBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VaultTests : VaultBase() {

    @Test
    fun `writeEncryptedDatabaseCopyTo without existing file creates encrypted Copy`() {
        // Arrange
        val destinationFile = tempDir.resolve("anyFile.copy")

        // Act
        vault.writeEncryptedDatabaseCopyTo(destinationFile)

        // Assert
        assertTrue(destinationFile.length() > 0)
    }

    @Test
    fun `writeEncryptedDatabaseCopyTo with existing file overwrites existing file`() {
        // Arrange
        val destinationFile = tempDir.resolve("anyFile.copy")
        destinationFile.createNewFile()

        // Act
        vault.writeEncryptedDatabaseCopyTo(destinationFile)

        // Assert
        assertTrue(destinationFile.length() > 0)
    }

    @Test
    fun `findCipherFileEntitySubFolders without root folders return empty list`() {
        // Act
        val rootFolders = runBlocking {
            vault.findCipherFileEntitySubFolders("").first()
        }

        // Assert
        assertTrue(rootFolders.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `findCipherFileEntitySubFolders with one root folder return list with one folder`(folderId: String) {
        // Arrange
        insertCipherFolderEntity(folderId)

        // Act
        val rootFolders = runBlocking {
            vault.findCipherFileEntitySubFolders("").first()
        }

        // Assert
        assertEquals(folderId, rootFolders.single().id)
    }

    @ParameterizedTest
    @ValueSource(ints = [2, 5, 10, 100])
    fun `findCipherFileEntitySubFolders with multiple root folders return matching list`(amountOfFolders: Int) {
        // Arrange
        repeat(amountOfFolders) { index ->
            insertCipherFolderEntity(index.toString())
        }

        // Act
        val rootFolders = runBlocking {
            vault.findCipherFileEntitySubFolders("").first()
        }

        // Assert
        assertEquals(amountOfFolders, rootFolders.size)
    }

    @Test
    fun `findCipherFileEntitySubFolders without subfolder return empty list`() {
        // Arrange
        val rootFolderId = "root_folder"
        insertCipherFolderEntity(rootFolderId)

        // Act
        val subfolders = runBlocking {
            vault.findCipherFileEntitySubFolders(rootFolderId).first()
        }

        // Assert
        assertTrue(subfolders.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_folder_id", "ZWJDI5432"])
    fun `findCipherFileEntitySubFolders with one subfolder return list with one folder`(subfolderId: String) {
        // Arrange
        val rootFolderId = "root_folder"
        insertCipherFolderEntity(rootFolderId)
        insertCipherFolderEntity(subfolderId, rootFolderId)

        // Act
        val subfolders = runBlocking {
            vault.findCipherFileEntitySubFolders(rootFolderId).first()
        }

        // Assert
        assertEquals(subfolderId, subfolders.single().id)
    }

    @ParameterizedTest
    @ValueSource(ints = [2, 5, 10, 100])
    fun `findCipherFileEntitySubFolders with multiple subfolders return matching list`(amountOfSubfolders: Int) {
        // Arrange
        val rootFolderId = "root_folder"
        insertCipherFolderEntity(rootFolderId)
        repeat(amountOfSubfolders) { index ->
            insertCipherFolderEntity(index.toString(), rootFolderId)
        }

        // Act
        val subfolders = runBlocking {
            vault.findCipherFileEntitySubFolders(rootFolderId).first()
        }

        // Assert
        assertEquals(amountOfSubfolders, subfolders.size)
    }

    @Test
    fun `findCipherFileEntitiesForFolder without fileEntities return empty list`() {
        // Arrange
        val rootFolderId = "root_folder"
        insertCipherFolderEntity(rootFolderId)

        // Act
        val fileEntities = runBlocking {
            vault.findCipherFileEntitiesForFolder(rootFolderId).first()
        }

        // Assert
        assertTrue(fileEntities.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["any_file_id", "ZWJDI5432"])
    fun `findCipherFileEntitiesForFolder with one fileEntity return list with one entity`(fileEntityId: String) {
        // Arrange
        val rootFolderId = "root_folder"
        insertCipherFolderEntity(rootFolderId)
        insertCipherFileEntity(fileEntityId, rootFolderId)

        // Act
        val fileEntities = runBlocking {
            vault.findCipherFileEntitiesForFolder(rootFolderId).first()
        }

        // Assert
        assertEquals(fileEntityId, fileEntities.single().id)
    }

    @ParameterizedTest
    @ValueSource(ints = [2, 5, 10, 100])
    fun `findCipherFileEntitiesForFolder with multiple fileEntities return matching list`(amountOfFileEntities: Int) {
        // Arrange
        val rootFolderId = "root_folder"
        insertCipherFolderEntity(rootFolderId)
        repeat(amountOfFileEntities) { index ->
            insertCipherFileEntity(index.toString(), rootFolderId)
        }

        // Act
        val fileEntities = runBlocking {
            vault.findCipherFileEntitiesForFolder(rootFolderId).first()
        }

        // Assert
        assertEquals(amountOfFileEntities, fileEntities.size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["new display name", "!§$%&/()=?`*+'#-_.:,;<>|"])
    fun `updateDisplayName updates vault name successfully`(newDisplayName: String) {
        // Act
        vault.updateDisplayName(newDisplayName)

        // Assert
        // Close vault and reopen it to check if the vault config is not corrupted
        vault.realm.close()
        vault = vaultFactory.decryptVault(
            vaultDirectory = tempDirectory,
            password = anyPassword.toByteArray(),
            databaseFile = tempDir.resolve(FileNames.INDEX_DATABASE)
        )
        assertEquals(newDisplayName, vault.displayName)
    }

    @Test
    fun `updateDisplayName with unchanged vault name does not fail`() {
        // Act
        vault.updateDisplayName(vault.displayName)

        // Assert
        // Close vault and reopen it to check if the vault config is not corrupted
        vault.realm.close()
        vault = vaultFactory.decryptVault(
            vaultDirectory = tempDirectory,
            password = anyPassword.toByteArray(),
            databaseFile = tempDir.resolve(FileNames.INDEX_DATABASE)
        )
        assertEquals(vault.displayName, vault.displayName)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "   "])
    fun `updateDisplayName with invalid vault name throws IllegalArgumentException`(invalidVaultName: String) {
        // Act, Assert
        assertThrows<IllegalArgumentException> {
            vault.updateDisplayName(invalidVaultName)
        }
    }

    @Test
    fun `updateDisplayName with too long vault name throws IllegalArgumentException`() {
        // Arrange
        val invalidVaultName = "1".repeat(MAX_VAULT_NAME_LENGTH + 1)

        // Act, Assert
        assertThrows<IllegalArgumentException> {
            vault.updateDisplayName(invalidVaultName)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["new folder name", "!§$%&/()=?`*+'#-_.:,;<>|", "ZWJDI5432"])
    fun `renameFolder updates folder name successfully`(newFolderName: String) {
        // Arrange
        val folder = insertCipherFolderEntity("folder")

        // Act
        runBlocking {
            vault.renameFolder(folder, newFolderName)
        }

        // Assert
        val updatedFolder = runBlocking {
            vault.findCipherFolderEntity(folder.id)
        }
        assertEquals(newFolderName, updatedFolder.displayName)
    }

    @Test
    fun `renameFolder with unchanged folder name does not fail`() {
        // Arrange
        val folder = insertCipherFolderEntity("folder")

        // Act, Assert
        assertDoesNotThrow {
            runBlocking {
                vault.renameFolder(folder, folder.displayName)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "   "])
    fun `renameFolder with invalid folder name throws IllegalArgumentException`(invalidFolderName: String) {
        // Arrange
        val folder = insertCipherFolderEntity("folder")

        // Act, Assert
        assertThrows<IllegalArgumentException> {
            runBlocking {
                vault.renameFolder(folder, invalidFolderName)
            }
        }
    }
}
