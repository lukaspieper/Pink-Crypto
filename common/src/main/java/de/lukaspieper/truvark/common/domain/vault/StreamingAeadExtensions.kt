/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault

import com.google.crypto.tink.StreamingAead
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

internal val emptyAssociatedData: ByteArray = ByteArray(0)

internal inline fun <R> StreamingAead.useEncryptingStream(outputStream: OutputStream, block: (OutputStream) -> R): R {
    return newEncryptingStream(outputStream, emptyAssociatedData).use(block)
}

internal inline fun <R> StreamingAead.useDecryptingStream(inputStream: InputStream, block: (InputStream) -> R): R {
    return newDecryptingStream(inputStream, emptyAssociatedData).use(block)
}

internal fun StreamingAead.encryptByteArray(bytes: ByteArray): ByteArray {
    val outputStream = ByteArrayOutputStream()
    useEncryptingStream(outputStream) { it.write(bytes) }
    return outputStream.toByteArray()
}

internal fun StreamingAead.decryptByteArray(encrypted: ByteArray): ByteArray {
    return useDecryptingStream(ByteArrayInputStream(encrypted)) { it.readBytes() }
}
