/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.android.gradle)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.hilt)
    id("com.google.android.gms.oss-licenses-plugin")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "de.lukaspieper.truvark"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
        targetSdk = 34
        versionCode = 15
        versionName = "1.0.1"

        ndk {
            // Tink does not support 32-bit architectures (https://developers.google.com/tink/faq/support_for_32bit)
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-DEV"
            resValue("string", "app_name", "Truvark DEV")

            // Can be enabled for testing
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            resValue("string", "app_name", "Truvark")

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        sarifReport = true
        abortOnError = false
        checkDependencies = true
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}

dependencies {
    val composeBom = platform(libs.android.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(project(":common"))

    // Foundation
    implementation(libs.android.core.ktx)
    implementation(libs.android.core.splashscreen)
    implementation(libs.android.activity.compose)
    implementation(libs.android.lifecycle.process)

    // Coroutines
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.android.workmanager)

    // Data and storage
    implementation(libs.android.datastore.preferences)
    // Required for classes that inherit from @Serializable classes
    implementation(libs.kotlin.serialization.json)

    // Dependency Injection
    implementation(libs.dagger.hilt)
    kapt(libs.dagger.hilt.compiler)

    implementation(libs.android.hilt.workmanager)
    kapt(libs.android.hilt.compiler)
    implementation(libs.android.hilt.navigation.compose)

    // Compose
    implementation(libs.android.ui)
    implementation(libs.android.ui.graphics)
    implementation(libs.android.ui.tooling.preview)
    implementation(libs.android.material3)
    implementation(libs.android.lifecycle.runtime.compose)
    implementation(libs.android.compose.material.icons.extended)
    debugImplementation(libs.android.ui.tooling)

    implementation(libs.google.material)
    implementation(libs.google.accompanist.permissions)

    // Adaptive
    implementation(libs.android.compose.adaptive)
    implementation(libs.android.compose.adaptive.layout)
    implementation(libs.android.compose.adaptive.navigation)

    // Navigation
    implementation(libs.android.navigation.compose)

    // Media player
    implementation(libs.telephoto)
    implementation(libs.google.accompanist.drawablepainter)
    implementation(libs.bundles.coil)

    // Cryptography
    implementation(libs.argon2.android)
    implementation(libs.android.biometric.ktx)
}

kapt {
    correctErrorTypes = true
}

android.applicationVariants.configureEach {
    val variantName = name

    val copyLicensesTask = tasks.register<Copy>("${variantName}CopyLicenses") {
        from("$rootDir/LICENSES")
        // TODO: Don't depend on oss-licenses-plugin's directory. Haven't figured `registerGeneratedResFolders` out yet.
        into(layout.buildDirectory.dir("generated/third_party_licenses/$variantName/res/raw"))

        rename { fileName ->
            fileName.lowercase()
                .removeSuffix(".txt")
                .replace("-", "_")
                .replace(".", "_")
        }
    }

    tasks.named("preBuild") {
        dependsOn(copyLicensesTask)
    }
}
