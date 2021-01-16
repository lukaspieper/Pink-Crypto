package de.lukaspieper.crypto.pink

import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.subtle.AesGcmJce
import com.google.crypto.tink.subtle.Base64
import de.lukaspieper.crypto.pink.argon2.Argon2id

/**
 * TODO
 */
public class PasswordEncryptedKeyset internal constructor(
    private val encryptedKeyData: ByteArray,
    private val argon2ConfigAndSalt: String
) {
    public companion object {
        private const val base64Flags = Base64.NO_PADDING or Base64.NO_WRAP

        /**
         * TODO
         */
        public fun importFromString(input: String): PasswordEncryptedKeyset {
            val (encodedEncryptedKeyData, encodedConfigAndSalt) = input.splitAtIndexOf('$')
            val encryptedKeyData = Base64.decode(encodedEncryptedKeyData, base64Flags)

            return PasswordEncryptedKeyset(encryptedKeyData, encodedConfigAndSalt)
        }
    }

    /**
     * TODO
     */
    public fun exportAsString(): String {
        return Base64.encodeToString(encryptedKeyData, base64Flags) + argon2ConfigAndSalt
    }

    /**
     * TODO
     */
    public fun decryptWithPassword(password: ByteArray): KeysetHandle {
        val hash = Argon2id.hashPassword(password, argon2ConfigAndSalt)
        val passwordBasedKey = AesGcmJce(hash.raw)

        val keysetReader = BinaryKeysetReader.withBytes(encryptedKeyData)
        return KeysetHandle.read(keysetReader, passwordBasedKey)
    }
}

/**
 * Splits this char sequence into two strings at the first index of the specified [char].
 */
private fun String.splitAtIndexOf(char: Char): Pair<String, String> {
    val indexOfChar = indexOf(char)
    return take(indexOfChar) to substring(indexOfChar)
}
