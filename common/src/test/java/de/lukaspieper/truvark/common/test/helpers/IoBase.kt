/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.test.helpers

import de.lukaspieper.truvark.common.data.io.DirectoryInfo
import de.lukaspieper.truvark.common.data.io.FileInfo
import de.lukaspieper.truvark.common.data.io.JavaFileSystem
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Helper base class providing methods to ease testing file operations.
 */
abstract class IoBase {

    protected val fileSystem = JavaFileSystem()

    @field:TempDir
    lateinit var tempDir: File

    val tempDirectory: DirectoryInfo
        get() = fileSystem.directoryInfo(tempDir)

    fun createEmptyFileInTempDirectory(name: String): FileInfo {
        return createFileInTempDirectory(name, ByteArray(0))
    }

    fun createFileInTempDirectory(name: String, data: ByteArray): FileInfo {
        val file = combineTempDirectoryWithPath(name)
        return fileSystem.writeBytes(file, data)
    }

    fun createDirectoryInTempDirectory(name: String): DirectoryInfo {
        return fileSystem.createDirectory(tempDirectory, name)
    }

    fun combineTempDirectoryWithPath(fileName: String): FileInfo {
        return fileSystem.fileInfo(tempDir.resolve(fileName))
    }
}
