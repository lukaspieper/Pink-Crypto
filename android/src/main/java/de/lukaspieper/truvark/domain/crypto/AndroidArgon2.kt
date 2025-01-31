/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.domain.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import de.lukaspieper.truvark.common.crypto.Argon2

class AndroidArgon2 : Argon2() {
    private val argon2Kt = Argon2Kt()

    @Throws(IllegalArgumentException::class)
    override fun hashPassword(password: ByteArray, salt: ByteArray, config: Config): Hash {
        val hashResult = argon2Kt.hash(
            mode = when (config.type) {
                "argon2id" -> Argon2Mode.ARGON2_ID
                "argon2i" -> Argon2Mode.ARGON2_I
                "argon2d" -> Argon2Mode.ARGON2_D
                else -> throw IllegalArgumentException("Unknown Argon2 type: ${config.type}")
            },
            password = password,
            salt = salt,
            tCostInIterations = config.iterations,
            mCostInKibibyte = config.memoryCostInKibibyte,
            parallelism = config.parallelism,
            hashLengthInBytes = HASH_SIZE,
            version = when (config.version) {
                0x10 -> Argon2Version.V10
                0x13 -> Argon2Version.V13
                else -> throw IllegalArgumentException("Unknown Argon2 version: ${config.version}")
            }
        )

        return object : Hash {
            override fun toRaw(): ByteArray {
                return hashResult.rawHashAsByteArray()
            }

            override fun toEncodedConfigAndSalt(): String {
                return hashResult.encodedOutputAsString()
                    .substring(0, hashResult.encodedOutputAsString().lastIndexOf('$'))
            }
        }
    }
}
