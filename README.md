<div align="center">

<img src="./docs/assets/satsprice-logo.png" alt="SatsPrice Logo" title="SatsPrice logo" width="256"/>

# SatsPrice

This app fetches the price of Bitcoin relative to common fiat currencies from multiple sources, and converts inputted amounts between Sats, BTC, and the selected fiat currency.

[![GitHub downloads](https://img.shields.io/github/downloads/tyiu/sats-price/total?label=Downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/tyiu/sats-price/releases)

[![Last Version](https://img.shields.io/github/release/tyiu/sats-price?maxAge=3600&label=Stable&labelColor=06599d&color=043b69)](https://github.com/tyiu/sats-price)
[![License: GPL-3.0](https://img.shields.io/github/license/tyiu/sats-price?labelColor=27303D&color=0877d2)](/LICENSE)

## Download and Install

[<img src="./docs/assets/download_on_apple.svg"
alt="Download on the Apple App Store"
height="70">](https://apps.apple.com/app/satsprice/id6478230475)
[<img src="./docs/assets/download_on_zapstore.svg"
alt="Get it on Zap Store"
height="70">](https://github.com/zapstore/zapstore/releases)
[<img src="./docs/assets/download_on_obtainium.png"
alt="Get it on Obtaininum"
height="70">](https://github.com/ImranR98/Obtainium)
[<img src="./docs/assets/download_on_github.svg" alt="Get it on GitHub"
height="70">](https://github.com/tyiu/sats-price/releases)

## Supported Platforms

iOS 16.0+ • macOS 13.0+ • Android 10.0+

</div>

## Building

This is a free [Skip](https://skip.tools) dual-platform app project.
It builds a native app for both iOS and Android.

This project is both a stand-alone Swift Package Manager module,
as well as an Xcode project that builds and transpiles the project
into a Kotlin Gradle project for Android using the Skip plugin.

Building the module requires that Skip be installed using
[Homebrew](https://brew.sh) with `brew install skiptools/skip/skip`.

This will also install the necessary transpiler prerequisites:
Kotlin, Gradle, and the Android build tools.

Installation prerequisites can be confirmed by running `skip checkup`.

## Testing

The module can be tested using the standard `swift test` command
or by running the test target for the macOS destination in Xcode,
which will run the Swift tests as well as the transpiled
Kotlin JUnit tests in the Robolectric Android simulation environment.

Parity testing can be performed with `skip test`,
which will output a table of the test results for both platforms.

## Running

Xcode and Android Studio must be downloaded and installed in order to
run the app in the iOS simulator / Android emulator.
An Android emulator must already be running, which can be launched from 
Android Studio's Device Manager.

To run both the Swift and Kotlin apps simultaneously, 
launch the SatsPriceApp target from Xcode.
A build phases runs the "Launch Android APK" script that
will deploy the transpiled app a running Android emulator or connected device.
Logging output for the iOS app can be viewed in the Xcode console, and in
Android Studio's logcat tab for the transpiled Kotlin app.

## Attribution

This project depends on [Skip](https://skip.tools) to build as a multi-platform app.

The [Bitcoin Calculator](https://www.flaticon.com/free-icons/bitcoin-calculator) icon was created by Icon home and licensed as free for personal and commercial use with attribution.

The following free APIs are used:
- Coinbase
  - [Get Exchange Rates](https://docs.cdp.coinbase.com/coinbase-app/docs/api-exchange-rates#get-exchange-rates)
  - [Get Spot Price](https://docs.cdp.coinbase.com/coinbase-app/docs/api-prices#get-spot-price)
- CoinGecko
  - [Coin Price by IDs](https://docs.coingecko.com/reference/simple-price)