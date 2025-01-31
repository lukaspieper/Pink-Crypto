/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.data.io

/**
 * Information about a directory. These information are like a snapshot and may get outdated.
 */
public data class DirectoryInfo(
    public val uri: Any,
    public val name: String,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
    }
}
