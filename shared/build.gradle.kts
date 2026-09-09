import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.androidDriver)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            api(libs.moko.resources)
            api(libs.moko.resourcesCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.bignum)
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

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
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
