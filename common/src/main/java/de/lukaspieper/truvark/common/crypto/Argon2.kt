/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.crypto

import com.google.crypto.tink.subtle.Base64
import com.google.crypto.tink.subtle.Random

public abstract class Argon2(
    private val defaultConfig: Config = Config()
) {

    public fun hashPassword(password: ByteArray): Hash {
        val salt = Random.randBytes(SALT_SIZE)
        return hashPassword(password, salt, defaultConfig)
    }

    public fun hashPassword(password: ByteArray, encodedConfigAndSalt: String): Hash {
        val encodedConfigAndSaltParts = encodedConfigAndSalt.split('$')

        val type = encodedConfigAndSaltParts[1]
        val version = encodedConfigAndSaltParts[2].removePrefix("v=").toInt()

        val parameters = encodedConfigAndSaltParts[3].split(',')
        val memoryCost = parameters[0].removePrefix("m=").toInt()
        val iterations = parameters[1].removePrefix("t=").toInt()
        val parallelism = parameters[2].removePrefix("p=").toInt()

        val argon2Config = Config(type, version, memoryCost, iterations, parallelism)
        val salt = Base64.decode(encodedConfigAndSaltParts[4])

        return hashPassword(password, salt, argon2Config)
    }

    public abstract fun hashPassword(password: ByteArray, salt: ByteArray, config: Config): Hash

    public data class Config(
        public val type: String = "argon2id",
        public val version: Int = 0x13,
        public val memoryCostInKibibyte: Int = 65536,
        public val iterations: Int = 3,
        public val parallelism: Int = 1
    )

    public interface Hash {
        public fun toRaw(): ByteArray
        public fun toEncodedConfigAndSalt(): String
    }

    public companion object {
        public const val HASH_SIZE: Int = 32
        public const val SALT_SIZE: Int = 16
    }
}
