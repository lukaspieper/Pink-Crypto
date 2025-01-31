/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault

import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.LegacyKeysetSerialization
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.subtle.AesGcmJce
import com.google.crypto.tink.subtle.Base64
import com.google.crypto.tink.subtle.Random
import de.lukaspieper.truvark.common.constants.FileNames
import de.lukaspieper.truvark.common.constants.FixedValues.VAULT_ID_LENGTH
import de.lukaspieper.truvark.common.crypto.Argon2
import de.lukaspieper.truvark.common.data.io.DirectoryInfo
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.data.io.FileSystem
import de.lukaspieper.truvark.common.domain.IdGenerator
import de.lukaspieper.truvark.common.domain.ThumbnailProvider
import de.lukaspieper.truvark.common.domain.vault.internal.CipherFolderEntityCreator
import de.lukaspieper.truvark.common.domain.vault.internal.FileEncryption
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.RealmLoggerAdapter
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import de.lukaspieper.truvark.common.work.Scheduler
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.log.RealmLog
import java.io.ByteArrayOutputStream
import java.io.File

public class VaultFactory(
    private val argon2: Argon2,
    private val fileSystem: FileSystem,
    private val idGenerator: IdGenerator,
    private val thumbnailProvider: ThumbnailProvider,
    private val scheduler: Scheduler
) {
    private val base64Flags = Base64.NO_PADDING or Base64.NO_WRAP

    init {
        logcat(LogPriority.INFO) { "Redirecting the Realm log..." }
        RealmLog.removeAll()
        // Log everything because the app internal logger handles the log level filtering.
        RealmLog.setLevel(LogLevel.ALL)
        RealmLog.add(RealmLoggerAdapter())
    }

    public fun tryReadVaultConfig(file: FileInfo): VaultConfig? {
        try {
            return fileSystem.openInputStream(file).use { inputStream ->
                VaultConfig.fromByteArray(inputStream.readBytes())
            }
        } catch (e: Exception) {
            logcat(LogPriority.DEBUG) { e.asLog() }
        }

        return null
    }

    @Throws(Exception::class)
    public fun createVault(
        vaultDirectory: DirectoryInfo,
        password: ByteArray,
        databaseFile: File,
        vaultId: String = idGenerator.createStringId(VAULT_ID_LENGTH),
    ): Vault {
        require(password.isNotEmpty()) { "password must not be empty" }
        require(!databaseFile.exists()) { "databaseFile must not exist" }
        require(vaultId.length == VAULT_ID_LENGTH) { "vaultId must be $VAULT_ID_LENGTH characters long" }

        logcat(LogPriority.INFO) { "Creating new vault..." }
        val vaultFile = fileSystem.findOrCreateFile(vaultDirectory, FileNames.VAULT)

        val vaultConfig = VaultConfig(
            id = vaultId,
            displayName = vaultDirectory.name,
            encryptedKeyset = generatePasswordEncryptedKeyset(password),
        )

        val databaseKey = Random.randBytes(Realm.ENCRYPTION_KEY_LENGTH)
        val realm = openRealm(databaseKey, databaseFile)

        val keyset = decryptKeyset(vaultConfig.encryptedKeyset, password)
        val streamingAead = keyset.getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)
        vaultConfig.encryptedDatabaseKey = streamingAead.encryptByteArray(databaseKey)

        fileSystem.openOutputStream(vaultFile).use { outputStream ->
            outputStream.write(vaultConfig.toByteArray())
        }
        logcat(LogPriority.INFO) { "VaultConfig written to file. Creation finished." }

        return assembleVault(
            vaultConfig = vaultConfig,
            realm = realm,
            streamingAead = streamingAead,
            vaultDirectory = vaultDirectory,
        )
    }

    private fun generatePasswordEncryptedKeyset(password: ByteArray): String {
        val hash = argon2.hashPassword(password)
        val passwordBasedKey = AesGcmJce(hash.toRaw())

        val aesKeyTemplate = KeyTemplates.get("AES256_GCM_HKDF_4KB")
        val keysetHandle = KeysetHandle.generateNew(aesKeyTemplate)

        val outputStream = ByteArrayOutputStream()
        val binaryWriter = BinaryKeysetWriter.withOutputStream(outputStream)
        LegacyKeysetSerialization.serializeEncryptedKeyset(keysetHandle, binaryWriter, passwordBasedKey, ByteArray(0))
        val encryptedKeyData = outputStream.toByteArray()

        return Base64.encodeToString(encryptedKeyData, base64Flags) + hash.toEncodedConfigAndSalt()
    }

    public fun validatePassword(vault: Vault, password: ByteArray): Boolean {
        try {
            decryptKeyset(vault.encryptedKeyset, password)
            return true
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { e.asLog() }
        }

        return false
    }

    @Throws(Exception::class)
    public fun decryptVault(vaultDirectory: DirectoryInfo, password: ByteArray, databaseFile: File): Vault {
        require(databaseFile.isFile) { "databaseFile must be a file" }

        val vaultFile = fileSystem.findOrCreateFile(vaultDirectory, FileNames.VAULT)
        val vaultConfig = fileSystem.openInputStream(vaultFile).use { inputStream ->
            VaultConfig.fromByteArray(inputStream.readBytes())
        }

        val keyset = decryptKeyset(vaultConfig.encryptedKeyset, password)
        val streamingAead = keyset.getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)

        // TODO: Missing or corrupt database key does not prevent decryption of the actual files and the database
        //  could be recreated. User should be asked if he has a backup.
        val databaseKey = streamingAead.decryptByteArray(vaultConfig.encryptedDatabaseKey)
        val realm = openRealm(databaseKey, databaseFile)

        return assembleVault(
            vaultConfig = vaultConfig,
            realm = realm,
            streamingAead = streamingAead,
            vaultDirectory = vaultDirectory,
        )
    }

    private fun openRealm(databaseKey: ByteArray, databaseFile: File): Realm {
        val config = RealmConfiguration.Builder(Vault.realmSchema)
            .schemaVersion(Vault.realmSchemaVersion)
            .directory(databaseFile.parent)
            .name(databaseFile.name)
            .encryptionKey(databaseKey)
            .build()

        return Realm.open(config)
    }

    private fun decryptKeyset(encryptedKeyset: String, password: ByteArray): KeysetHandle {
        val delimiterIndex = encryptedKeyset.indexOf('$')
        require(delimiterIndex > 0) { "Invalid encryptedKeyset format" }

        val encodedConfigAndSalt = encryptedKeyset.substring(delimiterIndex)
        val hash = argon2.hashPassword(password, encodedConfigAndSalt)
        val passwordBasedKey = AesGcmJce(hash.toRaw())

        val encryptedKeyData = Base64.decode(encryptedKeyset.take(delimiterIndex), base64Flags)
        val keysetReader = BinaryKeysetReader.withBytes(encryptedKeyData)
        return LegacyKeysetSerialization.parseEncryptedKeyset(keysetReader, passwordBasedKey, ByteArray(0))
    }

    private fun assembleVault(
        vaultConfig: VaultConfig,
        realm: Realm,
        streamingAead: StreamingAead,
        vaultDirectory: DirectoryInfo,
    ): Vault {
        val vaultFileSystem = VaultFileSystem(fileSystem, vaultDirectory)

        return Vault(
            vaultConfig = vaultConfig,
            fileSystem = vaultFileSystem,
            streamingAead = streamingAead,
            realm = realm,
            scheduler = scheduler,
            fileEncryption = FileEncryption(
                streamingAead = streamingAead,
                realm = realm,
                fileSystem = vaultFileSystem,
                thumbnailProvider = thumbnailProvider,
                idGenerator = idGenerator,
            ),
            folderCreator = CipherFolderEntityCreator(
                realm = realm,
                idGenerator = idGenerator,
            ),
        )
    }
}
