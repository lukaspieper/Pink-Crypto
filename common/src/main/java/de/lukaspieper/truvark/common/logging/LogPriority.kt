/*
 * SPDX-FileCopyrightText: 2021 Square Inc.
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// https://github.com/square/logcat/blob/main/logcat/src/main/java/logcat/LogPriority.kt

package de.lukaspieper.truvark.common.logging

/**
 * An enum for log priorities that map to [android.util.Log] priority constants
 * without a direct import.
 */
@Suppress("MagicNumber")
public enum class LogPriority(
    public val priorityInt: Int
) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6),
    ASSERT(7)
}
