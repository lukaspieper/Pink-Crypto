// Copyright (c) 2021 Lukas Pieper
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

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
    val passwordBasedKey = AesGcmJce(hash.toRaw())

    val outputStream = ByteArrayOutputStream()
    val binaryWriter = BinaryKeysetWriter.withOutputStream(outputStream)
    write(binaryWriter, passwordBasedKey)
    val keyData = outputStream.toByteArray()

    return PasswordEncryptedKeyset(keyData, hash.toEncodedConfigAndSalt())
}
