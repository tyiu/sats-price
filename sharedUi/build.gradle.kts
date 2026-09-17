import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// The Skia-based Compose Multiplatform UI (App()/PriceScreen()) that androidApp, desktopApp,
// and webApp render. Split out of :shared so :shared itself stays free of compose.ui - the
// Compose Multiplatform Gradle plugin bundles the several-MB Skiko web runtime into *any*
// js/wasmJs target whose resolved dependency graph contains org.jetbrains.compose.ui:ui
// anywhere, and webHtmlApp's DOM-only build needs to avoid that.
kotlin {
    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "xyz.tyiu.satsprice.sharedui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            api(project(":shared"))

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.moko.resourcesCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
