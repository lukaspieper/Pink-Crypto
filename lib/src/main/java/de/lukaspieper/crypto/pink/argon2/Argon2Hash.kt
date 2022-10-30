// Copyright (c) 2021 Lukas Pieper
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package de.lukaspieper.crypto.pink.argon2

import java.nio.ByteBuffer

internal class Argon2Hash(val rawHash: ByteBuffer, val encodedOutput: ByteBuffer) {

    fun toRaw(): ByteArray {
        return rawHash.toByteArray()
    }

    fun toEncodedConfigAndSalt(): String {
        val outputString = encodedOutput.toByteArray().toString(charset = Charsets.US_ASCII)
        return removeHashFromEncoded(outputString)
    }

    private fun removeHashFromEncoded(encoded: String): String {
        return encoded.dropLastWhile { it != '$' }.dropLast(1)
    }
}
