/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.data.io

import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

/**
 * A central point of access to a file system. Note that the returned [FileInfo] and [DirectoryInfo] objects can only be
 * used with the same [FileSystem] implementation. Other implementations will likely throw exceptions.
 */
public abstract class FileSystem {

    @Throws(Exception::class)
    public abstract fun createFile(
        directoryInfo: DirectoryInfo,
        name: String,
        mimeType: String = "application/octet-stream"
    ): FileInfo

    public open fun findFileOrNull(directoryInfo: DirectoryInfo, name: String): FileInfo? {
        return listFiles(directoryInfo).firstOrNull { it.fullName == name }
    }

    @Throws(Exception::class)
    public open fun findOrCreateFile(directoryInfo: DirectoryInfo, name: String): FileInfo {
        return findFileOrNull(directoryInfo, name) ?: createFile(directoryInfo, name)
    }

    public open fun findDirectoryOrNull(directoryInfo: DirectoryInfo, name: String): DirectoryInfo? {
        return listDirectories(directoryInfo).firstOrNull { it.name == name }
    }

    @Throws(Exception::class)
    public abstract fun findOrCreateDirectory(directoryInfo: DirectoryInfo, name: String): DirectoryInfo

    public abstract fun listFiles(directoryInfo: DirectoryInfo): List<FileInfo>
    public abstract fun listDirectories(directoryInfo: DirectoryInfo): List<DirectoryInfo>

    @Throws(Exception::class)
    public abstract fun delete(fileInfo: FileInfo)

    @Throws(Exception::class)
    public abstract fun delete(directoryInfo: DirectoryInfo)

    public abstract fun relocate(
        sourceFileInfo: FileInfo,
        sourceParentDirectoryInfo: DirectoryInfo,
        targetDirectoryInfo: DirectoryInfo
    )

    @Throws(FileNotFoundException::class)
    public abstract fun openInputStream(fileInfo: FileInfo): InputStream

    @Throws(FileNotFoundException::class)
    public abstract fun openOutputStream(fileInfo: FileInfo): OutputStream
}
