/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

buildscript {
    dependencies {
        classpath(libs.google.oss.licenses)
    }
}

plugins {
    alias(libs.plugins.android.gradle) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.android.hilt) apply false

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.realm.kotlin) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.compose.compiler) apply false

    alias(libs.plugins.detekt)
}

dependencies {
    detektPlugins(libs.detekt.rules.formatting)
    detektPlugins(libs.detekt.rules.compose)
}

tasks.detekt {
    config.from(file(".detekt/detekt.yml"))
    baseline = file(".detekt/detekt-baseline.xml")
    buildUponDefaultConfig = true
    parallel = true
    setSource(files("android", "desktop", "common"))
}

tasks.detektBaseline {
    config.from(file(".detekt/detekt.yml"))
    baseline = file(".detekt/detekt-baseline.xml")
    buildUponDefaultConfig = true
    parallel = true
    setSource(files("android", "desktop", "common"))
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
