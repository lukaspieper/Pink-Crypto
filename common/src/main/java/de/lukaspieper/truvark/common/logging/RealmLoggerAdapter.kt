/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark.common.logging

import io.realm.kotlin.log.LogCategory
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.log.RealmLogger

/**
 * A [RealmLogger] that redirects all log messages to the app internal logger.
 */
public class RealmLoggerAdapter(
    private val tag: String = "REALM"
) : RealmLogger {

    override fun log(
        category: LogCategory,
        level: LogLevel,
        throwable: Throwable?,
        message: String?,
        vararg args: Any?
    ) {
        val priority = convertLogLevelToLogPriority(level)

        message?.let {
            logcat(priority, tag) { it }
        }

        throwable?.let {
            logcat(priority, tag) { it.asLog() }
        }
    }

    private fun convertLogLevelToLogPriority(level: LogLevel): LogPriority {
        return when (level) {
            LogLevel.ALL -> LogPriority.VERBOSE
            LogLevel.TRACE -> LogPriority.VERBOSE
            LogLevel.DEBUG -> LogPriority.DEBUG
            LogLevel.INFO -> LogPriority.INFO
            LogLevel.WARN -> LogPriority.WARN
            LogLevel.ERROR -> LogPriority.ERROR
            LogLevel.WTF -> LogPriority.ERROR
            LogLevel.NONE -> LogPriority.ERROR
        }
    }
}
