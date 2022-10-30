// Copyright (c) 2021 Lukas Pieper
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package de.lukaspieper.crypto.pink

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import de.lukaspieper.crypto.pink.argon2.Argon2id
import org.junit.Test

public class Argon2Tests {
    private val anyPasswordBytes = "1234567890".toByteArray()

    @Test
    public fun hashPassword_validPassword_correctConfig() {
        // Act
        val hash = Argon2id.hashPassword(anyPasswordBytes)

        // Assert
        assertThat(hash.encodedConfigAndSalt).startsWith("\$argon2id\$v=19\$m=65536,t=3,p=1")
    }

    @Test
    public fun hashPassword_returnsKey() {
        // Act
        val hash = Argon2id.hashPassword(anyPasswordBytes)

        // Assert
        assertThat(hash.raw.size).isEqualTo(Argon2id.keySize)
    }

    // This test is only to ensure that the same hash is not returned for each call.
    @Test
    public fun hashPassword_bulkExecution_noDuplicates() {
        // Act
        val hashes = (1..10).map {
            Argon2id.hashPassword(anyPasswordBytes)
        }

        // Assert
        assertThat(hashes).isEqualTo(hashes.distinct())
    }
}
