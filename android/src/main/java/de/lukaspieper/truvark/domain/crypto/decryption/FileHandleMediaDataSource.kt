/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.domain.crypto.decryption

import android.media.MediaDataSource
import okio.FileHandle

class FileHandleMediaDataSource(
    private val fileHandle: FileHandle
) : MediaDataSource() {

    override fun getSize(): Long {
        return fileHandle.size()
    }

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        return fileHandle.read(position, buffer, offset, size)
    }

    override fun close() {
        fileHandle.close()
    }
}
