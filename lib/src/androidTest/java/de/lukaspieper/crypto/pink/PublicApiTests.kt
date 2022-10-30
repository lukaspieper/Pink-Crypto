// Copyright (c) 2021 Lukas Pieper
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package de.lukaspieper.crypto.pink

import assertk.assertThat
import assertk.assertions.isSuccess
import assertk.assertions.matches
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import org.junit.Test

public class PublicApiTests {

    private val passwordBytes = "password".toByteArray()

    @Test
    public fun encryptWithPassword_newKeysetHandle_exportAsString() {
        // Arrange
        StreamingAeadConfig.register()
        val keysetHandle = KeysetHandle.generateNew(AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate())

        // Act
        val encryptedKeyset = keysetHandle.encryptWithPassword(passwordBytes)
        val encryptedKeysetString = encryptedKeyset.exportAsString()

        // Assert
        val expectedKeysetStringFormat = Regex("[A-Za-z0-9+/]+(?:\\\$[^\$]+){3}\\\$[A-Za-z0-9+/]+")
        assertThat(encryptedKeysetString).matches(expectedKeysetStringFormat)
    }

    @Test
    public fun decryptWithPassword_exportedKeyset_workingKeysetHandle() {
        // Arrange
        StreamingAeadConfig.register()
        val encryptedKeysetString = "Ep4BQIAJCXSGHPpe2Ggxq7lhB46yLdhuQ+UhRjC1ERL8UpMMBIZdqktxnx2ZnckB0ihTAR3+ObiAeDbYOHbnSgH3PrM5FLnNtYMgqrC5gBjq7IUhbYFsI+TxtuqSALGBehMGNynRCpVrfToMwM9rCe3tTq9BuUyIezR2FK/6cu0nuYXxgNhcZpl0371TDgyEduT/WDnqSQDmOtrug4O0oycaUQjp+ZODBBJJCj10eXBlLmdvb2dsZWFwaXMuY29tL2dvb2dsZS5jcnlwdG8udGluay5BZXNHY21Ia2RmU3RyZWFtaW5nS2V5EAEY6fmTgwQgAw\$argon2id\$v=19\$m=65536,t=3,p=1\$/Pi6gyzEyuSoEJoRsdk+LQ"

        // Act
        val encryptedKeyset = PasswordEncryptedKeyset.importFromString(encryptedKeysetString)
        val keysetHandle = encryptedKeyset.decryptWithPassword(passwordBytes)

        // Assert
        assertThat {
            val streamingAead = keysetHandle.getPrimitive(StreamingAead::class.java)
            checkNotNull(streamingAead)
        }.isSuccess()
    }
}
