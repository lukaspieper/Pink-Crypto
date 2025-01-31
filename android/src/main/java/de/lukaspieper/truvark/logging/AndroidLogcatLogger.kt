/*
 * SPDX-FileCopyrightText: 2021 Square Inc.
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// https://github.com/square/logcat/blob/main/logcat/src/main/java/logcat/AndroidLogcatLogger.kt

package de.lukaspieper.truvark.logging

import android.util.Log
import de.lukaspieper.truvark.BuildConfig
import de.lukaspieper.truvark.common.logging.LogPriority
import de.lukaspieper.truvark.common.logging.LogPriority.*
import de.lukaspieper.truvark.common.logging.LogcatLogger
import de.lukaspieper.truvark.logging.AndroidLogcatLogger.Companion.installWithDefaultPriority
import kotlin.math.min

private const val MAX_LOG_LENGTH = 4000

/**
 * A [logcat] logger that delegates to [android.util.Log] for any log with a priority of
 * at least [minPriorityInt], and is otherwise a no-op.
 *
 * Handles special cases for [LogPriority.ASSERT] (which requires sending to Log.wtf) and
 * splitting logs to be at most 4000 characters per line (otherwise logcat just truncates).
 *
 * Call [installWithDefaultPriority] to make sure you never log in release builds.
 *
 * The implementation is based on Timber DebugTree.
 */
class AndroidLogcatLogger private constructor(minPriority: LogPriority) : LogcatLogger {

    private val minPriorityInt: Int = minPriority.priorityInt

    override fun isLoggable(priority: LogPriority): Boolean =
        priority.priorityInt >= minPriorityInt

    override fun log(
        priority: LogPriority,
        tag: String,
        message: String
    ) {
        if (message.length < MAX_LOG_LENGTH) {
            logToLogcat(priority.priorityInt, tag, message)
            return
        }

        // Split by line, then ensure each line can fit into Log's maximum length.
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = min(newline, i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                logToLogcat(priority.priorityInt, tag, part)
                i = end
            } while (i < newline)
            i++
        }
    }

    private fun logToLogcat(
        priority: Int,
        tag: String,
        part: String
    ) {
        if (priority == Log.ASSERT) {
            Log.wtf(tag, part)
        } else {
            Log.println(priority, tag, part)
        }
    }

    companion object {
        fun installWithDefaultPriority() {
            if (LogcatLogger.isInstalled) {
                LogcatLogger.uninstall()
            }

            val priority = if (BuildConfig.DEBUG) VERBOSE else INFO
            LogcatLogger.install(AndroidLogcatLogger(priority))
        }
    }
}
