/*
 * SPDX-FileCopyrightText: 2024 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark

import kotlinx.serialization.Serializable

@Serializable
sealed interface Page {
    @Serializable
    data object Launcher : Page

    @Serializable
    data object Browser : Page

    @Serializable
    data class Presenter(val folderId: String, val fileId: String) : Page

    @Serializable
    data object SettingsHome : Page
}
