/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.entities

public interface CipherFileEntity : OriginalFileMetadata {
    public val id: String
    public val thumbnail: ByteArray?
    public val folder: CipherFolderEntity?
    public val mediaDurationSeconds: Long?

    public fun fullName(): String {
        if (fileExtension.isBlank()) {
            return name
        }

        return "$name.$fileExtension"
    }
}
