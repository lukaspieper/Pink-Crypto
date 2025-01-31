/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.test.helpers

import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import de.lukaspieper.truvark.common.constants.FileNames
import de.lukaspieper.truvark.common.data.io.DirectoryInfo
import de.lukaspieper.truvark.common.domain.IdGenerator
import de.lukaspieper.truvark.common.domain.entities.CipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.domain.findCipherFolderEntity
import de.lukaspieper.truvark.common.domain.vault.Vault
import de.lukaspieper.truvark.common.domain.vault.VaultFactory
import de.lukaspieper.truvark.common.test.fakes.SchedulerFake
import de.lukaspieper.truvark.common.test.fakes.ThumbnailProviderFake
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class VaultBase : IoBase() {
    protected val anyPassword = "z1zrwxrv2foHslr.rrlhxHcXCcwh1p"

    protected lateinit var vaultFactory: VaultFactory
    protected lateinit var vault: Vault
    protected lateinit var cipherFilesRootDirectory: DirectoryInfo

    @BeforeEach
    fun setUp() {
        StreamingAeadConfig.register()

        vaultFactory = VaultFactory(
            argon2 = JvmArgon2(),
            fileSystem = fileSystem,
            idGenerator = IdGenerator.Default,
            thumbnailProvider = ThumbnailProviderFake(),
            scheduler = SchedulerFake()
        )

        vault = vaultFactory.createVault(
            tempDirectory,
            anyPassword.toByteArray(),
            tempDir.resolve(FileNames.INDEX_DATABASE)
        )
        cipherFilesRootDirectory = fileSystem.findOrCreateDirectory(tempDirectory, "files")
    }

    @AfterEach
    fun tearDown() {
        if (!vault.realm.isClosed()) {
            vault.realm.close()
        }
    }

    protected fun insertCipherFolderEntity(folderId: String, parentFolderId: String = ""): CipherFolderEntity {
        return vault.realm.writeBlocking {
            val parentFolderEntity = when {
                parentFolderId.isNotBlank() -> findCipherFolderEntity(parentFolderId)
                else -> null
            }

            val folder = RealmCipherFolderEntity().apply {
                id = folderId
                displayName = folderId
                parentFolder = parentFolderEntity
            }

            copyToRealm(folder)
        }
    }

    protected fun insertCipherFileEntity(cipherFileEntityId: String, folderId: String = ""): CipherFileEntity {
        return vault.realm.writeBlocking {
            val folderEntity = when {
                folderId.isNotBlank() -> findCipherFolderEntity(folderId)
                else -> null
            }

            val cipherFileEntity = RealmCipherFileEntity().apply {
                id = cipherFileEntityId
                folder = folderEntity
            }

            copyToRealm(cipherFileEntity)
        }
    }
}
