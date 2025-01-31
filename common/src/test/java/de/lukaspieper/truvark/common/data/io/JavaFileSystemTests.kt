/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.data.io

import de.lukaspieper.truvark.common.test.helpers.IoBase
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Tests for [JavaFileSystem] that is not part of production code. It tests some default implementation and the test
 * implementation of [FileSystem] that is likely to be used in production code (desktop client).
 */
class JavaFileSystemTests : IoBase() {

    @ParameterizedTest
    @ValueSource(strings = ["any.file", "any"])
    fun `name returns file name of existing file`(fileName: String) {
        // Arrange
        val file = createEmptyFileInTempDirectory(fileName)

        // Act, Assert
        assertAll(
            { assertTrue { fileSystem.exists(file) } },
            { assertEquals(fileName, file.fullName) }
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing.file", "missing"])
    fun `name returns file name of not existing file`(fileName: String) {
        // Arrange
        val missingFile = combineTempDirectoryWithPath(fileName)

        // Act, Assert
        assertAll(
            { assertFalse { fileSystem.exists(missingFile) } },
            { assertEquals(fileName, missingFile.fullName) }
        )
    }

    @ParameterizedTest
    @ValueSource(longs = [32, 128])
    fun `length returns file length of existing file`(fileLength: Long) {
        // Arrange
        val bytes = ByteArray(fileLength.toInt())
        val file = createFileInTempDirectory("any.file", bytes)

        // Act, Assert
        assertAll(
            { assertTrue { fileSystem.exists(file) } },
            { assertEquals(fileLength, file.size) }
        )
    }

    @Test
    fun `length returns length 0 of not existing file`() {
        // Arrange
        val missingFile = combineTempDirectoryWithPath("missing.file")

        // Act, Assert
        assertAll(
            { assertFalse { fileSystem.exists(missingFile) } },
            { assertEquals(0L, missingFile.size) }
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["any.file", "any"])
    fun `delete() deletes existing file successfully`(fileName: String) {
        // Arrange
        val file = createEmptyFileInTempDirectory(fileName)

        // Act
        fileSystem.delete(file)

        // Assert
        assertFalse { fileSystem.exists(file) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing.file", "missing"])
    fun `delete() throws IOException on not existing file`(fileName: String) {
        // Arrange
        val missingFile = combineTempDirectoryWithPath(fileName)

        // Act, Assert
        assertThrows<IOException> { fileSystem.delete(missingFile) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["any.file", "any"])
    fun `openInputStream() returns InputStream on existing file`(fileName: String) {
        // Arrange
        val file = createEmptyFileInTempDirectory(fileName)

        // Act, Assert
        fileSystem.openInputStream(file).use { inputStream ->
            assertNotNull(inputStream)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing.file", "missing"])
    fun `openInputStream() throws FileNotFoundException on not existing file`(fileName: String) {
        // Arrange
        val missingFile = combineTempDirectoryWithPath(fileName)

        // Act, Assert
        assertThrows<FileNotFoundException> { fileSystem.openInputStream(missingFile) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["any.file", "any"])
    fun `openOutputStream() returns OutputStream on existing file`(fileName: String) {
        // Arrange
        val file = createEmptyFileInTempDirectory(fileName)

        // Act, Assert
        fileSystem.openOutputStream(file).use { outputStream ->
            assertNotNull(outputStream)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing.file", "missing"])
    fun `openOutputStream() returns OutputStream on not existing file`(fileName: String) {
        // Arrange
        val missingFile = combineTempDirectoryWithPath(fileName)

        // Act, Assert
        fileSystem.openOutputStream(missingFile).use { outputStream ->
            assertNotNull(outputStream)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["any.file", "any"])
    fun `readBytes() returns ByteArray on existing file`(fileName: String) {
        // Arrange
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val file = createFileInTempDirectory(fileName, bytes)

        // Act, Assert
        assertArrayEquals(bytes, fileSystem.readBytes(file))
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing.file", "missing"])
    fun `readBytes() throws FileNotFoundException on not existing file`(expectedFileName: String) {
        // Arrange
        val missingFile = combineTempDirectoryWithPath(expectedFileName)

        // Act, Assert
        assertThrows<FileNotFoundException> { fileSystem.readBytes(missingFile) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing.file", "missing"])
    fun `writeBytes() writes successfully to not existing file`(fileName: String) {
        // Arrange
        val missingFile = combineTempDirectoryWithPath(fileName)
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        // Act
        fileSystem.writeBytes(missingFile, bytes)

        // Assert
        assertArrayEquals(bytes, fileSystem.readBytes(missingFile))
    }

    @ParameterizedTest
    @ValueSource(strings = ["any.file", "any"])
    fun `writeBytes() writes successfully to existing file`(fileName: String) {
        // Arrange
        val initialBytes = ByteArray(16)
        val file = createFileInTempDirectory(fileName, initialBytes)
        val expectedBytes = byteArrayOf(1, 2, 3, 4, 5)

        // Act
        fileSystem.writeBytes(file, expectedBytes)

        // Assert
        assertArrayEquals(expectedBytes, fileSystem.readBytes(file))
    }
}
