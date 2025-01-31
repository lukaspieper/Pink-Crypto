/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault

import de.lukaspieper.truvark.common.constants.FileNames
import de.lukaspieper.truvark.common.data.io.DirectoryInfo
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.data.io.FileSystem
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * A **caching** [FileSystem] that is providing additional functionality special to a vault. The [FileSystem] methods
 * are forwarded to the underlying [fileSystem] implementation.
 */
public class VaultFileSystem internal constructor(
    private val fileSystem: FileSystem,
    private val rootDirectory: DirectoryInfo
) : FileSystem() {

    // This map is used to cache cipher directories. It is used to avoid unnecessary IO calls because
    // especially the Android file system is very slow (SAF).
    private val cipherDirectoryInfoCache = ConcurrentHashMap<String, DirectoryInfo>()

    private val cipherFilesRootDirectory: DirectoryInfo by lazy {
        fileSystem.findOrCreateDirectory(rootDirectory, CIPHER_FILES_DIRECTORY_NAME)
    }

    public val decryptionRootDirectory: DirectoryInfo
        get() = fileSystem.findOrCreateDirectory(rootDirectory, DECRYPTION_DIRECTORY_NAME)

    public val vaultFile: FileInfo
        get() = fileSystem.findFileOrNull(rootDirectory, FileNames.VAULT)!!

    public val databaseFile: FileInfo by lazy { fileSystem.findOrCreateFile(rootDirectory, FileNames.INDEX_DATABASE) }

    public fun fetchFilesFromCipherDirectory(name: String): List<FileInfo> {
        val cipherDirectory = cipherDirectoryInfoCache.getOrElse(name) {
            fileSystem.findDirectoryOrNull(cipherFilesRootDirectory, name)
        }

        return when {
            cipherDirectory != null -> {
                cipherDirectoryInfoCache.putIfAbsent(name, cipherDirectory)
                fileSystem.listFiles(cipherDirectory)
            }

            else -> emptyList()
        }
    }

    public fun findFileInCipherDirectory(directoryName: String, fileName: String): FileInfo? {
        val cipherDirectory = cipherDirectoryInfoCache.getOrElse(directoryName) {
            fileSystem.findDirectoryOrNull(cipherFilesRootDirectory, directoryName)
        }

        return when {
            cipherDirectory != null -> {
                cipherDirectoryInfoCache.putIfAbsent(directoryName, cipherDirectory)
                fileSystem.findFileOrNull(cipherDirectory, fileName)
            }

            else -> null
        }
    }

    public fun createFileInCipherDirectory(directoryName: String, fileName: String): FileInfo {
        val cipherDirectory = cipherDirectoryInfoCache.computeIfAbsent(directoryName) {
            fileSystem.findOrCreateDirectory(cipherFilesRootDirectory, directoryName)
        }

        return fileSystem.createFile(cipherDirectory, fileName)
    }

    public fun deleteFileFromCipherDirectory(directoryName: String, fileName: String) {
        val cipherDirectory = cipherDirectoryInfoCache.getOrElse(directoryName) {
            fileSystem.findDirectoryOrNull(cipherFilesRootDirectory, directoryName)
        }

        if (cipherDirectory != null) {
            cipherDirectoryInfoCache.putIfAbsent(directoryName, cipherDirectory)

            fileSystem.findFileOrNull(cipherDirectory, fileName)?.let { file ->
                fileSystem.delete(file)
            }
        }
    }

    public fun deleteCipherDirectory(name: String) {
        val cipherDirectory = cipherDirectoryInfoCache.getOrElse(name) {
            fileSystem.findDirectoryOrNull(cipherFilesRootDirectory, name)
        }

        if (cipherDirectory != null) {
            fileSystem.delete(cipherDirectory)
        }

        cipherDirectoryInfoCache.remove(name)
    }

    public fun relocateFileIntoCipherDirectory(
        sourceFileName: String,
        sourceParentDirectoryName: String,
        targetDirectoryName: String
    ) {
        // Find file first to ensure the directory is cached.
        val sourceFile = findFileInCipherDirectory(sourceParentDirectoryName, sourceFileName)!!
        val sourceParentDirectory = cipherDirectoryInfoCache[sourceParentDirectoryName]!!

        val targetDirectory = cipherDirectoryInfoCache.computeIfAbsent(targetDirectoryName) {
            fileSystem.findOrCreateDirectory(cipherFilesRootDirectory, targetDirectoryName)
        }

        fileSystem.relocate(sourceFile, sourceParentDirectory, targetDirectory)
    }

    //region FileSystemForwarding

    override fun createFile(directoryInfo: DirectoryInfo, name: String, mimeType: String): FileInfo {
        return fileSystem.createFile(directoryInfo, name, mimeType)
    }

    override fun findOrCreateDirectory(directoryInfo: DirectoryInfo, name: String): DirectoryInfo {
        return fileSystem.findOrCreateDirectory(directoryInfo, name)
    }

    override fun listFiles(directoryInfo: DirectoryInfo): List<FileInfo> {
        return fileSystem.listFiles(directoryInfo)
    }

    override fun listDirectories(directoryInfo: DirectoryInfo): List<DirectoryInfo> {
        return fileSystem.listDirectories(directoryInfo)
    }

    override fun delete(fileInfo: FileInfo) {
        return fileSystem.delete(fileInfo)
    }

    override fun delete(directoryInfo: DirectoryInfo) {
        // The directoryInfo could be a cipher directory. Therefore, we need to clear the cache.
        cipherDirectoryInfoCache.clear()
        return fileSystem.delete(directoryInfo)
    }

    override fun relocate(
        sourceFileInfo: FileInfo,
        sourceParentDirectoryInfo: DirectoryInfo,
        targetDirectoryInfo: DirectoryInfo
    ) {
        return fileSystem.relocate(sourceFileInfo, sourceParentDirectoryInfo, targetDirectoryInfo)
    }

    override fun openInputStream(fileInfo: FileInfo): InputStream {
        return fileSystem.openInputStream(fileInfo)
    }

    override fun openOutputStream(fileInfo: FileInfo): OutputStream {
        return fileSystem.openOutputStream(fileInfo)
    }

    //endregion

    public companion object {
        private const val CIPHER_FILES_DIRECTORY_NAME: String = "files"
        private const val DECRYPTION_DIRECTORY_NAME: String = "decrypted"
    }
}
