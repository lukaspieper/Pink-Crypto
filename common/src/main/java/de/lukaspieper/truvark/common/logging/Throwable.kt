/*
 * SPDX-FileCopyrightText: 2021 Square Inc.
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// https://github.com/square/logcat/blob/main/logcat/src/main/java/logcat/Throwables.kt

package de.lukaspieper.truvark.common.logging

import java.io.PrintWriter
import java.io.StringWriter

/**
 * Utility to turn a [Throwable] into a loggable string.
 *
 * The implementation is based on Timber.getStackTraceString(). It's different
 * from [android.util.Log.getStackTraceString] in the following ways:
 * - No silent swallowing of UnknownHostException.
 * - The buffer size is 256 bytes instead of the default 16 bytes.
 */
@Suppress("MagicNumber")
public fun Throwable.asLog(): String {
    val stringWriter = StringWriter(256)
    val printWriter = PrintWriter(stringWriter, false)
    printStackTrace(printWriter)
    printWriter.flush()
    return stringWriter.toString()
}
