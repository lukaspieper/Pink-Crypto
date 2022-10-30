// Copyright (c) 2021 Lukas Pieper
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package de.lukaspieper.crypto.pink.argon2

internal interface Argon2Config {
    val type: String
    val version: Int
    val memoryCostInKiBit: Int
    val iterations: Int
    val parallelism: Int

    companion object {
        fun fromEncodedConfig(encodedConfig: String): Argon2Config {
            val configParts = encodedConfig.trim('$').split('$')
            val type = configParts[0]
            val version = configParts[1].removePrefix("v=").toInt()

            val parameters = configParts[2].split(',')
            val memoryCost = parameters[0].removePrefix("m=").toInt()
            val iterations = parameters[1].removePrefix("t=").toInt()
            val parallelism = parameters[2].removePrefix("p=").toInt()

            return Custom(type, version, memoryCost, iterations, parallelism)
        }
    }

    private class Custom(
        override val type: String,
        override val version: Int,
        override val memoryCostInKiBit: Int,
        override val iterations: Int,
        override val parallelism: Int
    ) : Argon2Config

    object Default : Argon2Config {
        override val type: String = "argon2id"
        override val version: Int = 0x13
        override val memoryCostInKiBit: Int = 65536
        override val iterations: Int = 3
        override val parallelism: Int = 1
    }
}
