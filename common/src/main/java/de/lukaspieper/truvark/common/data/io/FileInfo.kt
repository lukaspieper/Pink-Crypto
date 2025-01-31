/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.data.io

import kotlin.time.Duration

/**
 * Information about a file. These information are like a snapshot and may get outdated. For example, the size is not
 * updated when data is written to the file.
 */
public data class FileInfo(
    public val uri: Any,
    public val fullName: String,
    public val size: Long,
    public val mimeType: String,
    public val mediaDuration: Duration? = null,
) {
    init {
        require(fullName.isNotBlank()) { "fullName must not be blank" }
        require(mimeType.isNotBlank()) { "mimeType must not be blank" }
        require(mimeType == mimeType.lowercase()) { "mimeType must be lowercase" }
    }

    val name: String by lazy { fullName.substringBeforeLast(".") }
    val extension: String by lazy { fullName.substringAfterLast(".", "") }
}
