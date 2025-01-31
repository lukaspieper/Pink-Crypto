/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.entities

import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.asLog
import de.lukaspieper.truvark.common.logging.logcat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/***
 * Bundle of information about the original file.
 */
public interface OriginalFileMetadata {
    public var name: String
    public var fileExtension: String
    public var mimeType: String
    public var fileSize: Long

    public companion object {

        public fun fromJsonOrNull(jsonText: String): OriginalFileMetadata? {
            try {
                val cleanedJsonText = jsonText.replace(0.toChar().toString(), "")
                return Json.decodeFromString(OriginalFileMetadataImpl.serializer(), cleanedJsonText)
            } catch (e: Exception) {
                logcat(LogPriority.WARN) { e.asLog() }
            }

            return null
        }
    }

    /**
     * Serializes only the properties defined in [OriginalFileMetadata] and ignores every other property or method.
     */
    public fun toJson(): String {
        val minimalImplementation = OriginalFileMetadataImpl().also {
            it.name = name
            it.fileExtension = fileExtension
            it.mimeType = mimeType
            it.fileSize = fileSize
        }

        return Json.encodeToString(OriginalFileMetadataImpl.serializer(), minimalImplementation)
    }

    /**
     * Minimal implementation of [OriginalFileMetadata] used for (de-)serialization.
     */
    @Serializable
    private class OriginalFileMetadataImpl : OriginalFileMetadata {
        @SerialName("OriginalName")
        override var name: String = ""

        @SerialName("FileExtension")
        override var fileExtension = ""

        @SerialName("MimeType")
        override var mimeType: String = ""

        @SerialName("FileSize")
        override var fileSize: Long = 0
    }
}
