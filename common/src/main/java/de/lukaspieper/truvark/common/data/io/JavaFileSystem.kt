/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.data.io

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files

/**
 * A [FileSystem] implementation that uses the Java [File] API under the hood.
 */
public class JavaFileSystem : FileSystem() {

    /**
     * Returns a [FileInfo] for the given [File]. In case the file does not exist, it cannot be validated whether it is
     * a file or a directory. Therefore, the caller must ensure that the file is a file. Furthermore if the mime type
     * cannot be determined, it is set to "application/octet-stream".
     *
     * This behavior will likely change when this class is used in production (desktop client).
     */
    public fun fileInfo(file: File): FileInfo {
        if (file.exists()) {
            require(file.isFile)
        }

        return FileInfo(
            uri = file,
            fullName = file.name,
            size = file.length(),
            mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"
        )
    }

    public fun directoryInfo(file: File): DirectoryInfo {
        require(file.isDirectory)
        return DirectoryInfo(file, file.name)
    }

    override fun createFile(directoryInfo: DirectoryInfo, name: String, mimeType: String): FileInfo {
        val newFile = (directoryInfo.uri as File).resolve(name)
        check(newFile.createNewFile()) { "Could not create file" }

        return fileInfo(newFile)
    }

    override fun findOrCreateDirectory(directoryInfo: DirectoryInfo, name: String): DirectoryInfo {
        return findDirectoryOrNull(directoryInfo, name) ?: createDirectory(directoryInfo, name)
    }

    override fun listFiles(directoryInfo: DirectoryInfo): List<FileInfo> {
        return (directoryInfo.uri as File).listFiles()
            ?.filter { it.isFile }
            ?.map { fileInfo(it) } ?: emptyList()
    }

    override fun listDirectories(directoryInfo: DirectoryInfo): List<DirectoryInfo> {
        return (directoryInfo.uri as File).listFiles()
            ?.filter { it.isDirectory }
            ?.map { directoryInfo(it) } ?: emptyList()
    }

    override fun delete(fileInfo: FileInfo) {
        // TODO: Does this behavior match the behavior of the other implementations?
        if (!(fileInfo.uri as File).exists()) throw IOException("File does not exist")

        fileInfo.uri.delete()
    }

    override fun delete(directoryInfo: DirectoryInfo) {
        (directoryInfo.uri as File).deleteRecursively()
    }

    override fun relocate(
        sourceFileInfo: FileInfo,
        sourceParentDirectoryInfo: DirectoryInfo,
        targetDirectoryInfo: DirectoryInfo
    ) {
        val targetFile = (targetDirectoryInfo.uri as File).resolve(sourceFileInfo.fullName)
        (sourceFileInfo.uri as File).renameTo(targetFile)
    }

    override fun openInputStream(fileInfo: FileInfo): InputStream {
        return (fileInfo.uri as File).inputStream()
    }

    override fun openOutputStream(fileInfo: FileInfo): OutputStream {
        return (fileInfo.uri as File).outputStream()
    }

    //region Additional methods (used by tests primarily)

    internal fun createDirectory(directoryInfo: DirectoryInfo, name: String): DirectoryInfo {
        val newDirectory = (directoryInfo.uri as File).resolve(name)
        check(newDirectory.mkdir()) { "Could not create directory" }

        return directoryInfo(newDirectory)
    }

    internal fun exists(fileInfo: FileInfo): Boolean {
        return (fileInfo.uri as File).exists()
    }

    internal fun writeBytes(fileInfo: FileInfo, bytes: ByteArray): FileInfo {
        (fileInfo.uri as File).writeBytes(bytes)

        // Refresh the file info because the size has changed
        return fileInfo(fileInfo.uri)
    }

    internal fun readBytes(fileInfo: FileInfo): ByteArray {
        return (fileInfo.uri as File).readBytes()
    }

    //endregion
}
