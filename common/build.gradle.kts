/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.realm.kotlin)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    explicitApi()
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}

dependencies {
    api(libs.kotlin.coroutines.core)
    api(libs.kotlin.serialization.json)
    api(libs.tink.android)
    implementation(libs.realm.kotlin)
    implementation(compose.runtime)

    testImplementation(libs.argon2.jvm)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.json)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}
