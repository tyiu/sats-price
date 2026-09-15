import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mokoResources)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))

            implementation(libs.compose.ui)

            implementation(npm("@js-joda/timezone", "2.25.1"))
        }
    }
}

multiplatformResources {
    resourcesPackage.set("xyz.tyiu.satsprice.webApp")
}

// moko-resources loads string/plural/image/file resources at runtime via fetch('./localization/...'),
// not via a webpack import, so webpack's production bundler has no reason to know that directory
// exists and never copies it into the distribution. (The dev server happens to serve it anyway, since
// it serves the whole compiled package directory as static files — which is why this only breaks in
// production.) Copy it into the actual distribution output ourselves so a static production build is
// self-contained.
val npmPackageName = "${rootProject.name}-${project.name}"
mapOf(
    "wasmJsBrowserDistribution" to "wasm",
    "jsBrowserDistribution" to "js",
).forEach { (taskName, targetDir) ->
    tasks.named<Sync>(taskName) {
        val localizationSrc = rootProject.layout.buildDirectory
            .dir("$targetDir/packages/$npmPackageName/kotlin/localization")
        doLast {
            val src = localizationSrc.get().asFile
            if (src.exists()) {
                src.copyRecursively(outputs.files.singleFile.resolve("localization"), overwrite = true)
            }

            // Compose Multiplatform renders via Skia compiled to WebAssembly (several MB), which is
            // loaded through a dynamic import() from inside webApp.js. Without a hint, the browser only
            // discovers those .wasm files after it has downloaded, parsed, and started executing the JS
            // bundle — a fully serial waterfall. Preloading lets the browser fetch the .wasm in parallel
            // with the JS instead. The filenames are content-hashed by webpack, so they're discovered
            // here rather than hardcoded in the static index.html source.
            val distDir = outputs.files.singleFile
            val indexHtml = distDir.resolve("index.html")
            if (indexHtml.exists()) {
                val wasmFiles = distDir.listFiles { f -> f.extension == "wasm" }.orEmpty().sortedBy { it.name }
                if (wasmFiles.isNotEmpty()) {
                    val preloadLinks = wasmFiles.joinToString("\n") { f ->
                        """    <link rel="preload" href="${f.name}" as="fetch" type="application/wasm" crossorigin>"""
                    }
                    val html = indexHtml.readText()
                    if ("rel=\"preload\"" !in html) {
                        indexHtml.writeText(html.replaceFirst("</head>", "$preloadLinks\n</head>"))
                    }
                }
            }
        }
    }
}