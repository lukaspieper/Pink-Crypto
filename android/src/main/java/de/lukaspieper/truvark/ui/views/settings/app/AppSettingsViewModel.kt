/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.ui.views.settings.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lukaspieper.truvark.common.logging.LogcatLogger
import de.lukaspieper.truvark.data.preferences.PersistentPreferences
import de.lukaspieper.truvark.logging.AndroidLogcatLogger
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val preferences: PersistentPreferences
) : ViewModel() {
    val isLoggingEnabled = preferences.loggingAllowed
    val imagesFitScreen = preferences.imagesFitScreen

    fun applyLogging(enabled: Boolean) = runBlocking {
        if (enabled) {
            AndroidLogcatLogger.installWithDefaultPriority()
        } else {
            LogcatLogger.uninstall()
        }

        preferences.saveLoggingAllowed(enabled)
    }

    fun applyImagesFitScreen(enabled: Boolean) = runBlocking {
        preferences.saveImagesFitScreen(enabled)
    }
}
