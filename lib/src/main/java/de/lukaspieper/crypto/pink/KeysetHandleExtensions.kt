package de.lukaspieper.crypto.pink

import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.subtle.AesGcmJce
import de.lukaspieper.crypto.pink.argon2.Argon2id
import java.io.ByteArrayOutputStream

/**
 * Encrypts the [KeysetHandle] with the given [password] and returns a [PasswordEncryptedKeyset].
 */
public fun KeysetHandle.encryptWithPassword(password: ByteArray): PasswordEncryptedKeyset {
    val hash = Argon2id.hashPassword(password)
    val passwordBasedKey = AesGcmJce(hash.raw)

    val outputStream = ByteArrayOutputStream()
    val binaryWriter = BinaryKeysetWriter.withOutputStream(outputStream)
    write(binaryWriter, passwordBasedKey)
    val keyData = outputStream.toByteArray()

    return PasswordEncryptedKeyset(keyData, hash.encodedConfigAndSalt)
}
