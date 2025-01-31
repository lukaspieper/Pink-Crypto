/*
 * SPDX-FileCopyrightText: 2023 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain.entities

import de.lukaspieper.truvark.common.domain.vault.Vault

/**
 * Represents the root folder of a [Vault].
 */
internal class RootCipherFolderEntity(
    override val displayName: String,
) : CipherFolderEntity {
    // The root folder does not exist in the database, its ID is statically defined as an empty string.
    override val id: String = ""
}
