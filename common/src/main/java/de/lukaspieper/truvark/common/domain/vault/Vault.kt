/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault

import com.google.crypto.tink.StreamingAead
import de.lukaspieper.truvark.common.constants.FixedValues.MAX_VAULT_NAME_LENGTH
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.domain.entities.CipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.CipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFileEntity
import de.lukaspieper.truvark.common.domain.entities.RealmCipherFolderEntity
import de.lukaspieper.truvark.common.domain.entities.RootCipherFolderEntity
import de.lukaspieper.truvark.common.domain.findCipherFileEntityOrNull
import de.lukaspieper.truvark.common.domain.findCipherFolderEntity
import de.lukaspieper.truvark.common.domain.vault.internal.CipherFolderEntityCreator
import de.lukaspieper.truvark.common.domain.vault.internal.FileDecryption
import de.lukaspieper.truvark.common.domain.vault.internal.FileDeletion
import de.lukaspieper.truvark.common.domain.vault.internal.FileEncryption
import de.lukaspieper.truvark.common.domain.vault.internal.FileRelocation
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import de.lukaspieper.truvark.common.work.Scheduler
import de.lukaspieper.truvark.common.work.WorkBundle
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel

public data class Vault internal constructor(
    public val fileSystem: VaultFileSystem,
    internal val realm: Realm,
    private var vaultConfig: VaultConfig,
    private val streamingAead: StreamingAead,
    private val scheduler: Scheduler,
    private val fileEncryption: FileEncryption,
    private val folderCreator: CipherFolderEntityCreator
) {
    private val fileDecryption = FileDecryption(fileSystem, streamingAead)
    private val fileDeletion = FileDeletion(this)
    private val fileRelocation = FileRelocation(this)

    val id: String
        get() = vaultConfig.id

    val displayName: String
        get() = vaultConfig.displayName

    internal val encryptedKeyset: String
        get() = vaultConfig.encryptedKeyset

    @Throws(NoSuchElementException::class, IllegalArgumentException::class)
    public suspend fun findCipherFolderEntity(folderId: String): CipherFolderEntity {
        return when {
            folderId.isBlank() -> RootCipherFolderEntity(displayName)
            else -> realm.findCipherFolderEntity(folderId)
        }
    }

    @Throws(NoSuchElementException::class, IllegalArgumentException::class)
    public suspend fun findCipherFileEntity(fileId: String): CipherFileEntity {
        return realm.findCipherFileEntityOrNull(fileId) ?: throw NoSuchElementException()
    }

    /**
     * Returns a flow containing the subfolders of the given [folderId]. A blank [folderId] will return the root
     * folders.
     */
    public fun findCipherFileEntitySubFolders(folderId: String): Flow<List<CipherFolderEntity>> {
        if (folderId.isBlank()) {
            return realm.query<RealmCipherFolderEntity>(
                "${RealmCipherFolderEntity.REALM_FIELD_PARENT_FOLDER} == $0",
                null
            ).asFlow().map { it.list }
        }

        return realm.query<RealmCipherFolderEntity>(
            "${RealmCipherFolderEntity.REALM_FIELD_PARENT_FOLDER}.${RealmCipherFolderEntity.REALM_FIELD_ID} == $0",
            folderId
        ).asFlow().map { it.list }
    }

    public fun findCipherFileEntitiesForFolder(folderId: String): Flow<List<CipherFileEntity>> {
        val filesQuery = "${RealmCipherFileEntity.REALM_FIELD_FOLDER}.${RealmCipherFolderEntity.REALM_FIELD_ID} == $0"
        return realm.query<RealmCipherFileEntity>(filesQuery, folderId)
            .sort(RealmCipherFileEntity.REALM_FIELD_CREATION_DATE, Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }

    public suspend fun createFolder(name: String, parentFolder: CipherFolderEntity) {
        require(name.isNotBlank())

        // No physical folder is created here. So no need to update VaultFileSystem's directory cache.
        folderCreator.createFolder(name, parentFolder)

        // Creating and updating folder are the only operation changing the database outside the control of the
        // scheduler. To keep the database consistent, the scheduler is informed about the change.
        scheduler.onVaultChanged(this)
    }

    @Throws(IllegalArgumentException::class)
    public suspend fun renameFolder(folder: CipherFolderEntity, newDisplayName: String) {
        if (folder.displayName == newDisplayName) return
        require(folder is RealmCipherFolderEntity)
        require(newDisplayName.isNotBlank())

        realm.write {
            val mutableFolder = findLatest(folder)!!
            mutableFolder.displayName = newDisplayName
        }

        // Creating and updating folder are the only operation changing the database outside the control of the
        // scheduler. To keep the database consistent, the scheduler is informed about the change.
        scheduler.onVaultChanged(this)
    }

    /**
     * Schedules the encryption of the given [sources] to the given [destination] folder. The [Scheduler] might
     * **require** some [metadata] to properly function on the platform.
     *
     * The encryption process will create a new file with a generated name in the given [destination] folder for each
     * file and writes an encrypted copy to it. The [sources] will not be modified or deleted.
     */
    @Throws(IllegalArgumentException::class)
    public fun scheduleEncryption(
        metadata: Scheduler.SchedulerMetadata,
        sources: List<() -> FileInfo>,
        destination: CipherFolderEntity,
        deleteSources: Boolean = false
    ) {
        if (sources.isEmpty()) return
        require(destination is RealmCipherFolderEntity)

        scheduler.schedule(
            workBundle = WorkBundle.EncryptingWorkBundle(
                fileEncryption = fileEncryption,
                fileSystem = fileSystem,
                sources = sources,
                destination = destination,
                deleteSources = deleteSources
            ),
            metadata = metadata
        )
    }

    /**
     * Schedules the decryption of the given [folders] and [files] from the vault. The [Scheduler] might
     * **require** some [metadata] to properly function on the platform.
     *
     * Writes decrypted copies of all folders and files to a matching directory inside
     * [VaultFileSystem.decryptionRootDirectory]. The source folders and files will not be modified or deleted.
     */
    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    public fun scheduleDecryption(
        metadata: Scheduler.SchedulerMetadata,
        parentFolder: CipherFolderEntity,
        files: List<CipherFileEntity>,
        folders: List<CipherFolderEntity>
    ) {
        require(files.isNotEmpty() || folders.isNotEmpty())

        scheduler.schedule(
            workBundle = WorkBundle.DecryptingWorkBundle(
                fileDecryption,
                parentFolder,
                files as? List<RealmCipherFileEntity> ?: throw IllegalArgumentException("Invalid files"),
                folders as? List<RealmCipherFolderEntity> ?: throw IllegalArgumentException("Invalid folders")
            ),
            metadata = metadata
        )
    }

    /**
     * Schedules the deletion the given [folders] and [files] from the vault. The [Scheduler] might
     * **require** some [metadata] to properly function on the platform.
     *
     * The physical files and folders and the database entries will be deleted.
     */
    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    public fun scheduleDeletion(
        metadata: Scheduler.SchedulerMetadata,
        files: List<CipherFileEntity>,
        folders: List<CipherFolderEntity>
    ) {
        require(files.isNotEmpty() || folders.isNotEmpty())

        scheduler.schedule(
            workBundle = WorkBundle.DeletingWorkBundle(
                fileDeletion,
                files as? List<RealmCipherFileEntity> ?: throw IllegalArgumentException("Invalid files"),
                folders as? List<RealmCipherFolderEntity> ?: throw IllegalArgumentException("Invalid folders")
            ),
            metadata = metadata
        )
    }

    /**
     * Schedules the relocation of the given [folders] and [files] to the given [destination] folder. The [Scheduler]
     * might **require** some [metadata] to properly function on the platform.
     *
     * Only relocating files will lead to IO operations. Relocating folders will only change the database entries.
     * During the relocation process, the files and folders will NOT be decrypted and encrypted.
     */
    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    public fun scheduleRelocation(
        metadata: Scheduler.SchedulerMetadata,
        destination: CipherFolderEntity,
        files: List<CipherFileEntity>,
        folders: List<CipherFolderEntity>
    ) {
        require(files.isNotEmpty() || folders.isNotEmpty())

        scheduler.schedule(
            workBundle = WorkBundle.RelocatingWorkBundle(
                fileRelocation,
                destination,
                files as? List<RealmCipherFileEntity> ?: throw IllegalArgumentException("Invalid files"),
                folders as? List<RealmCipherFolderEntity> ?: throw IllegalArgumentException("Invalid folders")
            ),
            metadata = metadata
        )
    }

    public fun newSeekableDecryptingChannel(channel: FileChannel): SeekableByteChannel {
        return streamingAead.newSeekableDecryptingChannel(channel, emptyAssociatedData)
    }

    public fun writeEncryptedDatabaseCopyTo(file: File) {
        // Realm requires a not existing destination file.
        if (file.exists()) {
            file.delete()
        }

        val exportConfiguration = RealmConfiguration.Builder(realmSchema)
            .schemaVersion(realmSchemaVersion)
            .directory(file.parent)
            .name(file.name)
            .encryptionKey(streamingAead.decryptByteArray(vaultConfig.encryptedDatabaseKey))
            .build()

        realm.writeCopyTo(exportConfiguration)
    }

    @Throws(Exception::class)
    public fun updateDisplayName(newDisplayName: String) {
        if (displayName == newDisplayName) return
        require(newDisplayName.isNotBlank())
        require(newDisplayName.length in 1..MAX_VAULT_NAME_LENGTH)

        try {
            val updatedVaultConfig = vaultConfig.copy(displayName = newDisplayName)
            updatedVaultConfig.encryptedDatabaseKey = vaultConfig.encryptedDatabaseKey

            fileSystem.openOutputStream(fileSystem.vaultFile).use { outputStream ->
                outputStream.write(updatedVaultConfig.toByteArray())
            }

            // Check if the update was successful.
            fileSystem.openInputStream(fileSystem.vaultFile).use { inputStream ->
                val controlVaultConfig = VaultConfig.fromByteArray(inputStream.readBytes())
                check(controlVaultConfig.displayName == newDisplayName)
                vaultConfig = controlVaultConfig
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }

            logcat { "Trying to restore previous vault config." }
            fileSystem.openOutputStream(fileSystem.vaultFile).use { outputStream ->
                outputStream.write(vaultConfig.toByteArray())
            }

            throw e
        }
    }

    public companion object {
        internal val realmSchema = setOf(RealmCipherFileEntity::class, RealmCipherFolderEntity::class)
        internal val realmSchemaVersion = 2L
    }
}
