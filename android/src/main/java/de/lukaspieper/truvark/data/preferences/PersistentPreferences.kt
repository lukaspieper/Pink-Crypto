/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.data.preferences

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.lukaspieper.truvark.domain.crypto.BiometricConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "AppPreferences")

/**
 * Persistent preferences based on [DataStore].
 */
class PersistentPreferences(context: Context) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    companion object {
        val LAST_USED_VAULT_ROOT_URI = stringPreferencesKey("PREF_VAULT_ROOT_URI")
        val BIOMETRIC_CONFIG = stringPreferencesKey("PREF_BIOMETRY_CONFIG")
        val LOGGING_ALLOWED = booleanPreferencesKey("PREF_LOGGING_ALLOWED")
        val IS_LIST_LAYOUT = booleanPreferencesKey("PREF_IS_LIST_LAYOUT")
        val IMAGES_FIT_SCREEN = booleanPreferencesKey("PREF_IMAGES_FIT_SCREEN")
    }

    suspend fun saveLastUsedVaultRootUri(uri: Uri) {
        dataStore.edit { preferences ->
            preferences[LAST_USED_VAULT_ROOT_URI] = uri.toString()
        }
    }

    val lastUsedVaultRootUri: Flow<Uri> = dataStore.data.map { preferences ->
        val lastUsedVaultRootUri = preferences[LAST_USED_VAULT_ROOT_URI]

        when {
            lastUsedVaultRootUri.isNullOrBlank() -> Uri.EMPTY
            else -> Uri.parse(lastUsedVaultRootUri)
        }
    }

    suspend fun saveBiometricConfig(config: BiometricConfig) {
        dataStore.edit { preferences ->
            preferences[BIOMETRIC_CONFIG] = config.toJson()
        }
    }

    val biometricConfig: Flow<BiometricConfig?> = dataStore.data.map { preferences ->
        val json = preferences[BIOMETRIC_CONFIG]

        when {
            json.isNullOrBlank() -> null
            else -> BiometricConfig.fromJson(json)
        }
    }

    suspend fun saveLoggingAllowed(allowed: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOGGING_ALLOWED] = allowed
        }
    }

    val loggingAllowed: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LOGGING_ALLOWED] ?: false
    }

    suspend fun saveIsListLayout(isListLayout: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LIST_LAYOUT] = isListLayout
        }
    }

    val isListLayout: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LIST_LAYOUT] ?: false
    }

    suspend fun saveImagesFitScreen(fitScreen: Boolean) {
        dataStore.edit { preferences ->
            preferences[IMAGES_FIT_SCREEN] = fitScreen
        }
    }

    val imagesFitScreen: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IMAGES_FIT_SCREEN] ?: true
    }
}
