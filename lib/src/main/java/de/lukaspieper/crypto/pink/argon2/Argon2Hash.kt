// Copyright (c) 2021 Lukas Pieper
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package de.lukaspieper.crypto.pink.argon2

import org.signal.argon2.Argon2

internal class Argon2Hash(argon2Result: Argon2.Result) {

    val raw: ByteArray = argon2Result.hash
    val encodedConfigAndSalt: String = removeHashFromEncoded(argon2Result.encoded)

    private fun removeHashFromEncoded(encoded: String): String {
        return encoded.dropLastWhile { it != '$' }.dropLast(1)
    }
}
