// Copyright (c) 2021 Lukas Pieper
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package de.lukaspieper.crypto.pink.argon2

import com.google.crypto.tink.subtle.Base64
import com.google.crypto.tink.subtle.Random
import java.nio.ByteBuffer

internal object Argon2id {
    const val keySize = 32
    private const val saltSize = 16

    init {
        System.loadLibrary("argon2id")
    }

    @Throws(Argon2Exception::class)
    internal fun hashPassword(password: ByteArray, encodedConfigAndSalt: String): Argon2Hash {
        val saltBeginningIndex = encodedConfigAndSalt.lastIndexOf('$') + 1
        val encodedSalt = encodedConfigAndSalt.substring(saltBeginningIndex)
        val salt = Base64.decode(encodedSalt)

        val encodedConfig = encodedConfigAndSalt.take(saltBeginningIndex)
        val argon2Config = Argon2Config.fromEncodedConfig(encodedConfig)

        return hash(password, salt, argon2Config)
    }

    @Throws(Argon2Exception::class)
    internal fun hashPassword(password: ByteArray): Argon2Hash {
        val salt = generateSalt()
        return hash(password, salt, Argon2Config.Default)
    }

    private fun generateSalt(): ByteArray {
        return Random.randBytes(saltSize)
    }

    @Throws(Argon2Exception::class)
    private fun hash(password: ByteArray, salt: ByteArray, config: Argon2Config): Argon2Hash {
        val passwordBuffer = ByteBuffer.allocateDirect(password.size).put(password)
        val saltBuffer = ByteBuffer.allocateDirect(salt.size).put(salt)

        try {
            val hashTarget = ByteBufferTarget()
            val encodedTarget = ByteBufferTarget()

            val returnCode = nativeArgon2Hash(
                mode = 2, // Argon2id
                version = config.version,
                t_cost = config.iterations,
                m_cost = config.memoryCostInKibibyte,
                parallelism = config.parallelism,
                password = passwordBuffer,
                salt = saltBuffer,
                hash_length = keySize,
                hash_destination = hashTarget,
                encoded_destination = encodedTarget
            )

            if (returnCode != 0 || !hashTarget.hasByteBufferSet()) {
                throw Argon2Exception()
            }

            return Argon2Hash(
                rawHash = hashTarget.getByteBuffer(),
                // ignore trailing \0 byte
                encodedOutput = encodedTarget.dropLastN(1).getByteBuffer()
            )
        } finally {
            passwordBuffer.wipeDirectBuffer()
            saltBuffer.wipeDirectBuffer()
        }
    }

    private external fun nativeArgon2Hash(
        mode: Int,
        version: Int,
        t_cost: Int,
        m_cost: Int,
        parallelism: Int,
        password: ByteBuffer,
        salt: ByteBuffer,
        hash_length: Int,
        hash_destination: ByteBufferTarget,
        encoded_destination: ByteBufferTarget
    ): Int
}
