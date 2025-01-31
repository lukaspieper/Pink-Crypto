/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface SettingsMenuItem {
    val icon: ImageVector
    val title: Int
    val description: Int?

    enum class Key {
        VAULT,
        APP,
        OSS_LICENSES,
        PRIVACY_POLICY,
        SOURCE_CODE,
    }

    data class Internal(
        override val icon: ImageVector,
        @StringRes override val title: Int,
        @StringRes override val description: Int? = null
    ) : SettingsMenuItem

    data class External(
        override val icon: ImageVector,
        @StringRes override val title: Int,
        @StringRes override val description: Int? = null,
        val uri: Uri
    ) : SettingsMenuItem
}
