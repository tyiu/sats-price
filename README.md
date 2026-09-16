<img src="./docs/assets/satsprice-logo.png" alt="SatsPrice Logo" title="SatsPrice logo" width="256"/>

# SatsPrice

This app fetches the price of Bitcoin relative to common fiat currencies from multiple sources, and converts inputted amounts between Sats, BTC, and the selected fiat currency.

**[satsprice.app Website](https://satsprice.app)**

[![GitHub downloads](https://img.shields.io/github/downloads/tyiu/sats-price/total?label=Downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/tyiu/sats-price/releases)

[![Last Version](https://img.shields.io/github/release/tyiu/sats-price?maxAge=3600&label=Stable&labelColor=06599d&color=043b69)](https://github.com/tyiu/sats-price)
[![License: GPL-3.0](https://img.shields.io/github/license/tyiu/sats-price?labelColor=27303D&color=0877d2)](/LICENSE)

## Download and Install

[<img src="./docs/assets/download_on_apple.svg"
alt="Download on the App Store"
height="70">](https://apps.apple.com/app/satsprice/id6478230475)
[<img src="./docs/assets/get-it-on-zapstore.svg"
alt="Get it on Zap Store"
height="70">](https://zapstore.dev/apps/xyz.tyiu.SatsPrice)
[<img src="./docs/assets/download_on_obtainium.png"
alt="Get it on Obtaininum"
height="70">](https://github.com/ImranR98/Obtainium)
[<img src="./docs/assets/badge_github.png" alt="Get it on GitHub"
height="70">](https://github.com/tyiu/sats-price/releases)

## Supported Platforms

iOS 16.0+ • macOS 13.0+ • Android 7.0+ • [Web](https://satsprice.app/app/) (any modern browser)

## Screenshots

### Android

<img src="./docs/assets/screenshots/android/android-1.png" width="200"> <img src="./docs/assets/screenshots/android/android-2.png" width="200"> <img src="./docs/assets/screenshots/android/android-3.png" width="200"> <img src="./docs/assets/screenshots/android/android-4.png" width="200"> <img src="./docs/assets/screenshots/android/android-5.png" width="200"> <img src="./docs/assets/screenshots/android/android-6.png" width="200"> <img src="./docs/assets/screenshots/android/android-7.png" width="200"> <img src="./docs/assets/screenshots/android/android-8.png" width="200">

### iPhone

<img src="./docs/assets/screenshots/iphone/iphone-1.png" width="200"> <img src="./docs/assets/screenshots/iphone/iphone-2.png" width="200"> <img src="./docs/assets/screenshots/iphone/iphone-3.png" width="200"> <img src="./docs/assets/screenshots/iphone/iphone-4.png" width="200"> <img src="./docs/assets/screenshots/iphone/iphone-5.png" width="200"> <img src="./docs/assets/screenshots/iphone/iphone-6.png" width="200"> <img src="./docs/assets/screenshots/iphone/iphone-7.png" width="200"> <img src="./docs/assets/screenshots/iphone/iphone-8.png" width="200">

### iPad

<img src="./docs/assets/screenshots/ipad/ipad-1.png" width="320"> <img src="./docs/assets/screenshots/ipad/ipad-2.png" width="320"> <img src="./docs/assets/screenshots/ipad/ipad-3.png" width="320"> <img src="./docs/assets/screenshots/ipad/ipad-4.png" width="320"> <img src="./docs/assets/screenshots/ipad/ipad-5.png" width="320"> <img src="./docs/assets/screenshots/ipad/ipad-6.png" width="320"> <img src="./docs/assets/screenshots/ipad/ipad-7.png" width="320"> <img src="./docs/assets/screenshots/ipad/ipad-8.png" width="320">

### macOS

<img src="./docs/assets/screenshots/macos/macos-1.png" width="320"> <img src="./docs/assets/screenshots/macos/macos-2.png" width="320"> <img src="./docs/assets/screenshots/macos/macos-3.png" width="320"> <img src="./docs/assets/screenshots/macos/macos-4.png" width="320"> <img src="./docs/assets/screenshots/macos/macos-5.png" width="320"> <img src="./docs/assets/screenshots/macos/macos-6.png" width="320">

## Kotlin Multiplatform

This is a Kotlin Multiplatform project targeting Android, iOS, macOS, Web, and Desktop (JVM).

* [/shared](./shared/src) contains almost the entire app — UI (built with Compose Multiplatform),
  view models, data sources, and domain logic. [commonMain](./shared/src/commonMain/kotlin) holds
  code shared by every target, alongside platform-specific source sets: `androidMain`, `jvmMain`
  (Desktop), `appleMain` (iOS and macOS), and `webMain` (JS and Wasm).

* [/androidApp](./androidApp), [/desktopApp](./desktopApp), and [/webApp](./webApp) are thin
  launcher shells around `shared`'s Compose UI.

* [/iosApp](./iosApp/iosApp) is a native SwiftUI app (shared across iOS and macOS), not Compose —
  it talks to `shared` through a hand-written bridge rather than rendering Compose directly.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and
options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
    - Hot reload: `./gradlew :desktopApp:hotRun --auto`
    - Standard run: `./gradlew :desktopApp:run`
- Web app (Compose Multiplatform UI, Skia-rendered - what's deployed to production):
    - Wasm target (faster, modern browsers): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
    - JS target (slower, supports older browsers): `./gradlew :webApp:jsBrowserDevelopmentRun`
- Web app, DOM alternative (Compose HTML UI, renders real DOM instead of Skia/canvas - not what's
  deployed to production): `./gradlew :webHtmlApp:jsBrowserDevelopmentRun`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.
- [Website](./website) with the web app built and served at `/app/` (the `webHtmlApp` DOM build,
  not the one production actually deploys): `./website/serve-local.sh` (serves at
  http://localhost:8000 by default; pass a port number to override)

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- Desktop tests: `./gradlew :shared:jvmTest`
- Web tests:
    - Wasm target: `./gradlew :shared:wasmJsTest`
    - JS target: `./gradlew :shared:jsTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

## Attribution

This project uses [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform), and
[Kotlin/Wasm](https://kotl.in/wasm/), along with the following libraries:

- [Ktor](https://github.com/ktorio/ktor) for networking
- [SQLDelight](https://github.com/cashapp/sqldelight) for local persistence
- [moko-resources](https://github.com/icerockdev/moko-resources) for multiplatform string/resource handling
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines), [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization), and [kotlinx.datetime](https://github.com/Kotlin/kotlinx-datetime)
- [kotlin-wrappers](https://github.com/JetBrains/kotlin-wrappers) for browser/JS interop on the Web target
- [kotlin-multiplatform-bignum](https://github.com/ionspin/kotlin-multiplatform-bignum) for arbitrary-precision arithmetic

The [Bitcoin Calculator](https://www.flaticon.com/free-icons/bitcoin-calculator) icon was created by Icon home and licensed as free for personal and commercial use with attribution.

The following free APIs are used:
- Coinbase
  - [Get Exchange Rates](https://docs.cdp.coinbase.com/coinbase-app/docs/api-exchange-rates#get-exchange-rates)
  - [Get Spot Price](https://docs.cdp.coinbase.com/coinbase-app/docs/api-prices#get-spot-price)
- CoinGecko
  - [Coin Price by IDs](https://docs.coingecko.com/reference/simple-price)