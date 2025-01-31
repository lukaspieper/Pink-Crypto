/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.lukaspieper.truvark.common.crypto.Argon2
import de.lukaspieper.truvark.common.data.io.FileSystem
import de.lukaspieper.truvark.common.domain.IdGenerator
import de.lukaspieper.truvark.common.domain.ThumbnailProvider
import de.lukaspieper.truvark.common.domain.vault.VaultFactory
import de.lukaspieper.truvark.common.work.Scheduler
import de.lukaspieper.truvark.data.database.DatabaseFileSynchronization
import de.lukaspieper.truvark.data.io.AndroidFileSystem
import de.lukaspieper.truvark.data.preferences.PersistentPreferences
import de.lukaspieper.truvark.domain.AndroidThumbnailProvider
import de.lukaspieper.truvark.domain.crypto.AndroidArgon2
import de.lukaspieper.truvark.domain.crypto.BiometricCryptoProvider
import de.lukaspieper.truvark.work.WorkScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Reusable
    @Provides
    fun provideAndroidFileSystem(@ApplicationContext appContext: Context): AndroidFileSystem {
        return AndroidFileSystem(appContext)
    }

    @Provides
    fun provideFileSystem(fileSystem: AndroidFileSystem): FileSystem {
        return fileSystem
    }

    @Singleton
    @Provides
    fun providePersistentPreferences(@ApplicationContext appContext: Context): PersistentPreferences {
        return PersistentPreferences(appContext)
    }

    @Singleton
    @Provides
    fun provideWorkScheduler(@ApplicationContext appContext: Context): WorkScheduler {
        return WorkScheduler(appContext)
    }

    @Provides
    fun provideScheduler(workScheduler: WorkScheduler): Scheduler {
        return workScheduler
    }

    @Singleton
    @Provides
    fun provideThumbnailProvider(@ApplicationContext appContext: Context): ThumbnailProvider {
        return AndroidThumbnailProvider(appContext)
    }

    @Reusable
    @Provides
    fun provideBiometricCryptoProvider(@ApplicationContext appContext: Context): BiometricCryptoProvider {
        return BiometricCryptoProvider(appContext)
    }

    @Provides
    fun provideIdGenerator(): IdGenerator {
        return IdGenerator.Default
    }

    @Reusable
    @Provides
    fun provideArgon2(): Argon2 {
        return AndroidArgon2()
    }

    @Provides
    fun provideVaultFactory(
        argon2: Argon2,
        fileSystem: FileSystem,
        idGenerator: IdGenerator,
        thumbnailProvider: ThumbnailProvider,
        scheduler: Scheduler
    ): VaultFactory {
        return VaultFactory(
            argon2 = argon2,
            fileSystem = fileSystem,
            idGenerator = idGenerator,
            thumbnailProvider = thumbnailProvider,
            scheduler = scheduler
        )
    }

    @Provides
    fun provideDatabaseFileSynchronization(
        workScheduler: WorkScheduler,
        fileSystem: FileSystem
    ): DatabaseFileSynchronization {
        return DatabaseFileSynchronization(workScheduler, fileSystem)
    }
}
