package de.lukaspieper.crypto.pink.argon2

import org.signal.argon2.Argon2

internal class Argon2Hash(argon2Result: Argon2.Result) {

    val raw: ByteArray = argon2Result.hash
    val encodedConfigAndSalt: String = removeHashFromEncoded(argon2Result.encoded)

    private fun removeHashFromEncoded(encoded: String): String {
        return encoded.dropLastWhile { it != '$' }.dropLast(1)
    }
}
