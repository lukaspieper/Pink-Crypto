package de.lukaspieper.crypto.pink.argon2

import com.google.crypto.tink.subtle.Base64
import com.google.crypto.tink.subtle.Random
import org.signal.argon2.*

internal object Argon2id {
    const val keySize = 32
    private const val saltSize = 16

    private val defaultArgon2 = Argon2Config.Default.buildArgon2()

    @Throws(Argon2Exception::class)
    internal fun hashPassword(password: ByteArray): Argon2Hash {
        val argon2Result = defaultArgon2.hash(password, generateSalt())
        return Argon2Hash(argon2Result)
    }

    private fun generateSalt(): ByteArray = Random.randBytes(saltSize)

    fun hashPassword(password: ByteArray, encodedConfigAndSalt: String): Argon2Hash {
        val saltBeginningIndex = encodedConfigAndSalt.lastIndexOf('$') + 1
        val encodedSalt = encodedConfigAndSalt.substring(saltBeginningIndex)
        val salt = Base64.decode(encodedSalt)

        val encodedConfig = encodedConfigAndSalt.take(saltBeginningIndex)
        val argon2 = Argon2Config.fromEncodedConfig(encodedConfig).buildArgon2()
        val argon2Result = argon2.hash(password, salt)
        return Argon2Hash(argon2Result)
    }
}
