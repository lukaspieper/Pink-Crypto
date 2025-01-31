/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.test.helpers

import de.lukaspieper.truvark.common.crypto.Argon2
import de.mkammerer.argon2.Argon2Factory
import de.mkammerer.argon2.Argon2Factory.Argon2Types
import de.mkammerer.argon2.Argon2Version

class JvmArgon2 : Argon2(
    // Lower memory cost and iterations for faster tests
    defaultConfig = Config(
        memoryCostInKibibyte = 128,
        iterations = 1
    )
) {

    @Throws(IllegalArgumentException::class)
    override fun hashPassword(password: ByteArray, salt: ByteArray, config: Config): Hash {
        val argon2 = Argon2Factory.createAdvanced(
            when (config.type) {
                "argon2id" -> Argon2Types.ARGON2id
                "argon2i" -> Argon2Types.ARGON2i
                "argon2d" -> Argon2Types.ARGON2d
                else -> throw IllegalArgumentException("Unknown Argon2 type: ${config.type}")
            }
        )

        val hashResult = argon2.hashAdvanced(
            config.iterations,
            config.memoryCostInKibibyte,
            config.parallelism,
            password,
            salt,
            HASH_SIZE,
            when (config.version) {
                0x10 -> Argon2Version.V10
                0x13 -> Argon2Version.V13
                else -> throw IllegalArgumentException("Unknown Argon2 version: ${config.version}")
            }
        )

        return object : Hash {
            override fun toRaw(): ByteArray {
                return hashResult.raw
            }

            override fun toEncodedConfigAndSalt(): String {
                return hashResult.encoded.substring(0, hashResult.encoded.lastIndexOf('$'))
            }
        }
    }
}
