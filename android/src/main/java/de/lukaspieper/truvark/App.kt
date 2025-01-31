/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import dagger.hilt.android.HiltAndroidApp
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.logcat
import de.lukaspieper.truvark.data.preferences.PersistentPreferences
import de.lukaspieper.truvark.logging.AndroidLogcatLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var preferences: PersistentPreferences

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // StrictMode.enableDefaults();

            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        initLogging()
        initTink()
    }

    private fun initLogging() {
        runBlocking {
            val isLoggingAllowed = preferences.loggingAllowed.first()

            if (isLoggingAllowed) {
                AndroidLogcatLogger.installWithDefaultPriority()
            }
        }

        // Obviously, this message is not printed when NoLog is active.
        logcat(LogPriority.INFO) { "Logging is enabled." }
    }

    private fun initTink() {
        StreamingAeadConfig.register()
    }
}
