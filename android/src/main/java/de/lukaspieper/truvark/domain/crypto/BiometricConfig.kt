/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.domain.crypto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BiometricConfig(
    @SerialName("VaultId")
    val vaultId: String,

    @SerialName("IV")
    val iv: ByteArray,

    @SerialName("AccessKey")
    val accessKey: ByteArray
) {
    companion object {

        fun fromJson(json: String): BiometricConfig {
            return Json.decodeFromString(serializer(), json)
        }
    }

    fun toJson(): String {
        return Json.encodeToString(serializer(), this)
    }

    // generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BiometricConfig

        if (vaultId != other.vaultId) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!accessKey.contentEquals(other.accessKey)) return false

        return true
    }

    // generated
    override fun hashCode(): Int {
        var result = vaultId.hashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + accessKey.contentHashCode()
        return result
    }
}
