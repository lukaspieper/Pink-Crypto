/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.domain

import com.google.crypto.tink.subtle.Random

public interface IdGenerator {

    public fun createStringId(length: Int): String

    public object Default : IdGenerator {
        public const val ALLOWED_CHARS: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"

        override fun createStringId(length: Int): String {
            val randomString = StringBuilder()

            repeat(length) {
                val index = Random.randInt(ALLOWED_CHARS.length)
                randomString.append(ALLOWED_CHARS[index])
            }

            return randomString.toString()
        }
    }
}
