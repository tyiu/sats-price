plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Compose HTML (org.jetbrains.compose.html:html-core) only publishes js and jvm target
// variants as of Compose Multiplatform 1.11.1 - no wasmJs artifact exists yet, unlike the
// Skia-based Compose Multiplatform UI toolkit webApp uses (which targets both). So this
// comparison module is js-only for now.
kotlin {
    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))

            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)
        }
    }
}
