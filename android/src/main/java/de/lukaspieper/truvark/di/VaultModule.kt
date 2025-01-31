/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.di

import android.content.Context
import android.content.Intent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.lukaspieper.truvark.common.domain.vault.Vault

// TODO: Get rid of this module
@Module
@InstallIn(SingletonComponent::class)
object VaultModule {
    private var vault: Vault? = null

    fun initializeVaultModule(vault: Vault) {
        this.vault = vault
    }

    @Provides
    fun provideVault(@ApplicationContext appContext: Context): Vault {
        if (vault == null) {
            // I can't reproduce this case, and it should never happen, but crashes have been reported by Google Play
            // on devices known to kill apps in the background (https://dontkillmyapp.com/). Attempt app restart.
            appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)?.let { intent ->
                val mainIntent = Intent.makeRestartActivityTask(intent.component)
                appContext.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
        }

        return vault ?: error("Vault not initialized")
    }
}
