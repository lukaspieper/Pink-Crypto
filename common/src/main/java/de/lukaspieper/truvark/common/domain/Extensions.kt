/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain

public fun ByteArray.toHexString(): String {
    return joinToString("") {
        (0xFF and it.toInt()).toString(16).padStart(2, '0')
    }
}
