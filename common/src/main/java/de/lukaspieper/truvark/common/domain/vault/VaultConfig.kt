/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.vault

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
public data class VaultConfig(
    @SerialName("Id")
    public val id: String,

    @SerialName("DisplayName")
    public val displayName: String,

    @SerialName("EncryptedKeyset")
    internal val encryptedKeyset: String
) {
    @SerialName("EncryptedDatabaseKey")
    internal var encryptedDatabaseKey: ByteArray = ByteArray(0)

    @Throws(Exception::class)
    internal fun toByteArray(): ByteArray {
        return Json.encodeToString(serializer(), this).toByteArray()
    }

    internal companion object {

        @Throws(Exception::class)
        fun fromByteArray(byteArray: ByteArray): VaultConfig {
            val jsonText = String(byteArray)
            val vaultConfig = Json.decodeFromString(serializer(), jsonText)

            check(vaultConfig.id.isNotBlank())
            check(vaultConfig.encryptedKeyset.isNotBlank())

            return vaultConfig
        }
    }
}
