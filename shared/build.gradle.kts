import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.mokoResources)
    alias(libs.plugins.sqldelight)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64()
    ).forEach { appleTarget ->
        appleTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(libs.moko.resources)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "xyz.tyiu.satsprice.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.androidDriver)
        }
        commonMain.dependencies {
            api(libs.moko.resources)
            // PriceViewModel's own supertype and CurrencyConverter's BigDecimal parameters are
            // part of :shared's public API, so consumers that touch those types directly (e.g.
            // webHtmlApp's Compose HTML screens) need them on their own compile classpath too -
            // api() (not implementation()) is what makes Gradle expose them transitively.
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.bignum)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.sqldelight.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.ktor.client.mock)
        }
        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.nativeDriver)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.sqldelight.sqliteDriver)
        }
        webMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}

multiplatformResources {
    resourcesPackage.set("xyz.tyiu.satsprice.shared")
}

sqldelight {
    databases {
        register("AppDatabase") {
            packageName.set("xyz.tyiu.satsprice.db")
        }
    }
}
