/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.domain.crypto.decryption

import android.content.ContentResolver
import android.net.Uri
import de.lukaspieper.truvark.common.constants.FixedValues
import de.lukaspieper.truvark.common.domain.entities.OriginalFileMetadata
import de.lukaspieper.truvark.common.domain.vault.Vault
import okio.FileHandle
import okio.ForwardingSource
import okio.Source
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

/**
 * A [FileHandle] that decrypts the content of a file. The header is read and decrypted during initialization and
 * it's content is available via [header].
 *
 * To support random file access, this class utilizes [SeekableByteChannel] that requires Android SDK 24 (N).
 */
class DecryptingFileHandle(
    contentResolver: ContentResolver,
    vault: Vault,
    uri: Uri
) : FileHandle(false) {

    private var assetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r")
    private var fileInputStream = assetFileDescriptor!!.createInputStream()
    private var decryptingByteChannel = vault.newSeekableDecryptingChannel(fileInputStream.channel)

    val header: OriginalFileMetadata

    init {
        val headerByteArray = ByteArray(FixedValues.ENCRYPTED_FILE_HEADER_SIZE)
        val headerBuffer = ByteBuffer.wrap(headerByteArray)
        val readBytes = decryptingByteChannel.read(headerBuffer)

        check(readBytes == FixedValues.ENCRYPTED_FILE_HEADER_SIZE) {
            "Could not read header of encrypted file"
        }

        header = OriginalFileMetadata.fromJsonOrNull(String(headerByteArray))!!
    }

    override fun protectedRead(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int): Int {
        if (byteCount == 0) return 0
        if (fileOffset >= protectedSize()) return -1

        synchronized(decryptingByteChannel) {
            val sizeToRead = minOf(byteCount, (size() - fileOffset).toInt())
            decryptingByteChannel.position(fileOffset + FixedValues.ENCRYPTED_FILE_HEADER_SIZE)

            val destination = ByteBuffer.wrap(array, arrayOffset, sizeToRead)
            return decryptingByteChannel.read(destination)
        }
    }

    override fun protectedSize(): Long {
        return header.fileSize
    }

    override fun protectedWrite(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int) {
        throw NotImplementedError("This file handle is read-only.")
    }

    override fun protectedFlush() {
        throw NotImplementedError("This file handle is read-only.")
    }

    override fun protectedResize(size: Long) {
        throw NotImplementedError("This file handle is read-only.")
    }

    override fun protectedClose() {
        decryptingByteChannel.close()
        fileInputStream.close()
        assetFileDescriptor?.close()
    }

    /**
     * In some cases it's not possible to close the [FileHandle] and the [FileHandle.FileHandleSource] when using
     * [source]. This method returns a [Source] that closes the [FileHandle] when it's closed.
     */
    fun singleSource(): Source {
        // There does not seem to be a way to check thru the base class if the file handle is closed.
        checkNotNull(assetFileDescriptor) { "closed" }
        return ClosingFileHandleSource(this)
    }

    private class ClosingFileHandleSource(private val fileHandle: FileHandle) : ForwardingSource(fileHandle.source()) {
        override fun close() {
            super.close()
            fileHandle.close()
        }
    }
}
