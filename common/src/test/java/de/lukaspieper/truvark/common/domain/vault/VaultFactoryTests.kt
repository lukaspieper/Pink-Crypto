/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault

import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import de.lukaspieper.truvark.common.constants.FileNames
import de.lukaspieper.truvark.common.domain.IdGenerator
import de.lukaspieper.truvark.common.test.fakes.SchedulerFake
import de.lukaspieper.truvark.common.test.fakes.ThumbnailProviderFake
import de.lukaspieper.truvark.common.test.helpers.IoBase
import de.lukaspieper.truvark.common.test.helpers.JvmArgon2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.security.GeneralSecurityException

class VaultFactoryTests : IoBase() {

    private val vaultId = "VwNaOs"
    private val anyPassword = "z1zrwxrv2foHslr.rrlhxHcXCcwh1p"

    // `${'$'}` == `$`
    private val validSampleVaultFileContent =
        """
            {
                "DisplayName": "junit",
                "EncryptedKeyset": "Ep4B6cPiN+GNKKuPh75MJHUGxwHvo771p3cSeUITPmultVsYJJ/nUJgdXsz8Ph764AXKGFdSkFvHihFXPo1EN5CcNNMh4utvmNzx3aEL0a5QsSNTPLNTUSWkBnb1ITga1kF7DUnKc4MCYidVAzXn5vZpT1zZ6VfODJHISnSMXrDhUAiIMiwQefwkIHoDYuHdnelpZEq6fXdU9knq5hDMyOc${'$'}argon2id${'$'}v=19${'$'}m=65536,t=3,p=1${'$'}u/Ky8+ZqrCm++NnJ/1SstQ",
                "Id": "$vaultId",
                "EncryptedDatabaseKey": [40, -55, 37, 66, 127, -30, 36, -31, 91, 8, 50, -50, -117, 28, -90, -109, -98, -115, 69, -4, -62, -45, 28, 30, -127, -34, 60, -40, -56, -125, -7, -5, -70, 27, -107, -74, 64, 76, 82, -12, -45, -124, -54, 81, -107, -68, 82, 52, 45, -25, 110, -88, -104, 120, -66, 43, -126, 94, 90, 51, -92, 10, 116, 59, 98, -40, -75, 94, -10, 9, -101, -121, -90, 75, 55, 96, -77, 104, -97, -94, 50, 105, 2, -128, 118, -68, 36, -14, 0, 118, 7, 30, -58, -35, -82, 31, -35, -113, -56, 26, 23, -114, 36, 84, 76, 22, 60, -49, -53, -76, 12, 121, -11, 100, 61, 65, -49, 109, -23, 4]
            }
        """

    // encryptedKeyset is modified, compare with above one
    private val invalidSampleVaultFileContent =
        """
            {
                "DisplayName": "junit",
                "EncryptedKeyset": "Up4B6cPiN+GNKKuPh75MJHUGxwHvo771p3cSeUITPmultVsYJJ/nUJgdXsz8Ph764AXKGFdSkFvHihFXPo1EN5CcNNMh4utvmNzx3aEL0a5QsSNTPLNTUSWkBnb1ITga1kF7DUnKc4MCYidVAzXn5vZpT1zZ6VfODJHISnSMXrDhUAiIMiwQefwkIHoDYuHdnelpZEq6fXdU9knq5hDMyOc${'$'}argon2id${'$'}v=19${'$'}m=65536,t=3,p=1${'$'}u/Ky8+ZqrCm++NnJ/1SstQ",
                "Id": "$vaultId"
            }
        """

    private lateinit var vaultFactory: VaultFactory

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
    }

    @Test
    fun `CreateVault writes a VaultConfig to disk and returns valid vault`() {
        // Act
        val vault = vaultFactory.createVault(
            tempDirectory,
            anyPassword.toByteArray(),
            tempDir.resolve(FileNames.INDEX_DATABASE)
        )

        // Assert
        val vaultFile = combineTempDirectoryWithPath(FileNames.VAULT)
        val vaultConfig = VaultConfig.fromByteArray(fileSystem.readBytes(vaultFile))
        assertAll(
            { assertTrue(vaultConfig.encryptedKeyset.isNotBlank()) },
            { assertTrue(vaultConfig.id.isNotBlank()) },
            { assertTrue(vaultConfig.displayName.isNotBlank()) },
            { assertTrue(vaultConfig.encryptedDatabaseKey.isNotEmpty()) },
        )

        vault.realm.close()
    }

    @Test
    fun `CreateVault with empty password throws IllegalArgumentException`() {
        // Act, Assert
        assertThrows<IllegalArgumentException> {
            vaultFactory.createVault(tempDirectory, ByteArray(0), tempDir.resolve(FileNames.INDEX_DATABASE))
        }
    }

    @Test
    fun `CreateVault with invalid databaseFile throws IllegalArgumentException`() {
        // Act, Assert
        assertThrows<IllegalArgumentException> {
            // tempDir is a directory, not a file. Therefore it is invalid.
            vaultFactory.createVault(tempDirectory, anyPassword.toByteArray(), tempDir)
        }
    }

    @Test
    fun `DecryptVault with valid vault file and invalid external realm file throws IllegalStateException`() {
        // Arrange
        createFileInTempDirectory(FileNames.VAULT, validSampleVaultFileContent.toByteArray())
        val externalDatabasePath = tempDir.resolve(FileNames.INDEX_DATABASE)
        externalDatabasePath.writeBytes(ByteArray(16))

        // Act, Assert
        assertThrows<IllegalStateException> {
            vaultFactory.decryptVault(tempDirectory, anyPassword.toByteArray(), externalDatabasePath)
        }
    }

    @Test
    fun `DecryptVault with invalid vault file throws GeneralSecurityException`() {
        // Arrange
        createFileInTempDirectory(FileNames.VAULT, invalidSampleVaultFileContent.toByteArray())
        // database file must exist to pass parameter validation
        createEmptyFileInTempDirectory(FileNames.INDEX_DATABASE)

        // Act, Assert
        assertThrows<GeneralSecurityException> {
            vaultFactory.decryptVault(
                tempDirectory,
                anyPassword.toByteArray(),
                tempDir.resolve(FileNames.INDEX_DATABASE)
            )
        }
    }

    @Test
    fun `DecryptVault returns vault right after creation`() {
        // Act
        val createdVault = vaultFactory.createVault(
            tempDirectory,
            anyPassword.toByteArray(),
            tempDir.resolve(FileNames.INDEX_DATABASE)
        )
        createdVault.realm.close()

        val decryptedVault = vaultFactory.decryptVault(
            tempDirectory,
            anyPassword.toByteArray(),
            tempDir.resolve(FileNames.INDEX_DATABASE)
        )
        decryptedVault.realm.close()

        // Assert
        assertAll(
            { assertEquals(createdVault.id, decryptedVault.id) },
            { assertEquals(createdVault.displayName, decryptedVault.displayName) },
            { assertNotNull(createdVault.realm) },
            { assertNotNull(decryptedVault.realm) },
        )
    }
}
