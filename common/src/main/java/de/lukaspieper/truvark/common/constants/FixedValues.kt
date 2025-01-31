/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.constants

public object FixedValues {
    public const val MIN_PASSWORD_LENGTH: Int = 8

    // The value is arbitrary and the still too high to display the name in the UI.
    public const val MAX_VAULT_NAME_LENGTH: Int = 64

    public const val VAULT_ID_LENGTH: Int = 6
    public const val FILENAME_LENGTH: Int = 16

    // Changing ENCRYPTED_FILE_HEADER_SIZE value will cause decryption of already encrypted files to fail.
    public const val ENCRYPTED_FILE_HEADER_SIZE: Int = 2048
}
