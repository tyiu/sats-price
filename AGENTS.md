# AGENTS.md

Guidance for AI coding agents working in this repository. See `README.md` for
the user-facing project description and build/run/test commands — this file
covers the things that aren't obvious from reading the code, or that took
real trial-and-error to get right.

## What this is

SatsPrice is a Kotlin Multiplatform app, currently released on iOS, macOS,
and Android (see README's Supported Platforms/Download and Install). Web and
Desktop/JVM are real future release targets too — `desktopApp` already has
full native packaging configured (DMG/MSI/DEB, see its `build.gradle.kts`) —
but neither has a release/CI pipeline set up yet, so treat them as
not-yet-shipped rather than dev-only. The app converts between BTC, Sats,
and fiat currencies using live exchange rates (Coinbase, CoinGecko, or a
manually typed-in rate).

This is a from-scratch rewrite of an earlier Skip-based (Swift-transpiled-to-
Kotlin) implementation — `main` is now this Kotlin Multiplatform project.

## Module layout

- `shared/` — the actual app: UI, view models, data sources, domain logic.
  Almost everything happens here.
- `androidApp/`, `desktopApp/`, `webApp/` — thin launcher shells around
  `shared`'s Compose UI. Rarely need changes.
- `iosApp/` — a native SwiftUI app (shared across iOS and macOS via
  `#if os(macOS)`), **not** Compose. It talks to `shared` through a
  hand-written bridge (see "Core architecture" below), not by rendering
  Compose directly.

Inside `shared/src`, source sets follow standard KMP conventions:
`commonMain` (all platforms), `androidMain`, `jvmMain` (Desktop),
`appleMain` (iOS + macOS, shared), `webMain` (JS + Wasm, shared),
`jsMain`/`wasmJsMain` (JS/Wasm-specific only), plus per-target `*Main`
folders for things that need exact target granularity.

`expect`/`actual` pairs are named `Foo.kt` (common) / `Foo.jvm.kt` /
`Foo.android.kt` / `Foo.apple.kt` / `Foo.web.kt` — follow that convention for
new ones. Ignore `Platform.kt`/`getPlatform()` in every source set: it's
unused KMP-wizard scaffolding, not load-bearing.

## Core architecture

- `ui/PriceViewModel.kt` — the single state holder (`ConverterUiState`),
  shared by every platform. All business logic (rate fetching, currency
  selection, amount conversion, manual-rate math) lives here.
- `ui/ConverterUiStateDisplay.kt` — pure derived-display helpers
  (`ConverterUiState.xyz()` extension functions) shared between Compose and
  the iOS bridge. **Read the localization note below before adding
  anything here that produces user-facing text.**
- `ui/PriceScreen.kt` — the actual Compose UI. Used as-is by Android,
  Desktop, and Web (same composable, three renderers).
- iOS/macOS **doesn't** use Compose. The chain is:
  `PriceViewModel` (Kotlin) → `IosPriceViewModel`/`IosConverterState`
  (`shared/src/appleMain/.../IosPriceViewModel.kt`, a flattened Map-free
  snapshot of the state, since Kotlin/Native's Swift export handles
  `List<DataClass>` far better than `Map<K,V>`) → `ConverterViewModel.swift`
  (a thin `ObservableObject` wrapper) → `ContentView.swift` /
  `CurrencyPickerSheet.swift` (the actual SwiftUI views). When you change
  something in `PriceViewModel`/`ConverterUiState`, check whether
  `IosPriceViewModel.kt` needs the same field/method added, and whether
  `ConverterViewModel.swift` needs a forwarding method.

## Localization — read this before touching display strings

Strings live in `shared/src/commonMain/moko-resources/base/strings.xml`
(moko-resources; only a `base/` — no other locales exist yet). There is
**no synchronous, cross-platform way to resolve a moko-resources string
from plain shared Kotlin code**:

- Compose can resolve one via `stringResource()`, but only inside a
  `@Composable` function.
- Apple (`ui/IosLocalization.kt`'s `localizedString`/`localizedFormattedString`)
  can resolve one synchronously outside Compose — but that's Apple-only.
- Android has no platform `Context` available in the shared `ViewModel`.
- Web resource loading is `fetch()`-based, i.e. asynchronous — fundamentally
  incompatible with a plain synchronous function call.

So shared, non-Composable code (`ConverterUiStateDisplay.kt`,
`PriceViewModel.kt`) must never build a user-facing sentence out of English
literals. Return **data** instead (a formatted number, a formatted
date/time, a boolean, an enum) and let each UI layer (`PriceScreen.kt` via
`stringResource`, `ContentView.swift` via `IosLocalizationKt`) assemble the
localized sentence around it. `ConverterUiState.lastUpdatedDateTime()` is
the pattern to copy: it returns a locale-formatted `String?` (using
`domain/DateFormat.kt`, which *is* fine to call from shared code — it's
locale-aware formatting via platform APIs, not translated text), and each
UI layer wraps it with its own localized "Updated %1$s" string.

## Locale-aware formatting

`domain/NumberFormat.kt` (digit grouping, decimal separator) and
`domain/DateFormat.kt` (date/time) are `expect`/`actual` wrappers around
each platform's native locale APIs (`java.text`/`java.time` on JVM/Android,
`NSLocale`/`NSDateFormatter` on Apple, `Intl.*` on web). These are safe to
call from anywhere in shared code — they format using the platform's
locale, they don't translate UI text.

## Kotlin/Native ↔ Swift interop conventions

Functions called from Swift are written as **plain top-level functions**,
not `CurrencyInfo`/`ConverterUiState` extension functions — e.g.
`matchesCurrencySearch(info, query)` rather than `info.matchesCurrencySearch(query)`,
`currencyFlagEmoji(code)`. Kotlin/Native's Objective-C/Swift export handles
extension-function receivers unpredictably (the exported Swift signature's
argument label for the receiver isn't obvious ahead of time); plain
functions export as `FileNameKt.functionName(label: ...)` with predictable,
positional argument labels. Follow this for anything new that Swift needs
to call.

## Currency data is inherently messy — expect platform divergence

- `SystemCurrencies.kt` / `CurrencyFlag.kt`: `issuingCountryCodes()` derives
  a currency's issuing country/countries from its ISO 4217 code (plus a
  hardcoded map for the handful of currencies shared by multiple countries
  with no single issuer). `currencyFlagEmoji()` returns `null` on web
  (`supportsFlagEmoji()` is `false` there) because Compose for Web renders
  through Skia onto a `<canvas>`, not the browser's text stack — no
  system/browser color-emoji font to fall back to, so a flag's
  regional-indicator codepoints would draw as empty boxes.
- `SystemCurrencies.web.kt` needs its own withdrawn-currency filtering
  (`WITHDRAWN_CURRENCY_CODES` + a year-annotation regex, e.g. CLDR naming a
  retired currency "Afghan Afghani (1927–2002)"): `Intl.supportedValuesOf
  ('currency')`'s result is ICU-version-dependent per browser and includes
  currencies retired decades ago. JVM/Android/Apple instead derive
  "currently used" live from each platform's own per-country currency
  lookup (`java.util.Currency`/`NSLocale`), so they don't need a
  maintained list. If you need ground truth for "is this currency still
  active," a quick JVM snippet iterating `Locale.getISOCountries()` +
  `Currency.getInstance(Locale)` is the most reliable source available in
  this environment — more reliable than trusting any single browser's
  `Intl` data or assuming a Wikipedia snapshot is current.
- Region/country display names (used by currency search) are cached
  (`regionDisplayNameCache` in `SystemCurrencies.kt`) and warmed at
  `PriceViewModel` startup (`warmRegionDisplayNameCache`), since some
  platforms' lookups (especially constructing `Intl.DisplayNames` on web)
  are too expensive to redo on every keystroke.

## Persistence

SQLDelight (`data/db/SqlDelightStores.kt` + platform-specific driver
`actual`s) backs `ExchangeRateStore`/`SelectedCurrenciesStore`/
`SelectedSourceStore`, each exposed via an `expect fun createXStore()`
factory. Web has no real SQLDelight driver — SQLDelight's web driver needs a
worker plus a wasm sqlite binary — so it gets a `localStorage`-backed
fallback instead (`data/db/LocalStorageStores.kt`), which does survive page
reloads, just scoped to the browser profile/origin (cleared by clearing
site data, not shared across browsers/devices).

## Testing and verification

- `shared/src/commonTest` holds the real unit test suite (pure Kotlin,
  runs identically across targets). `./gradlew :shared:jvmTest` is the
  fast one to run during iteration; it's usually sufficient since
  `commonTest` code doesn't touch platform actuals directly.
- After any shared-code change, also compile the other targets to catch
  platform-specific breakage even without running their tests:
  `:shared:compileAndroidMain`, `:shared:compileKotlinWasmJs`,
  `:shared:compileKotlinJs`, `:shared:compileKotlinMacosArm64`.
- For iOS/macOS-touching changes, build both
  `xcodebuild -scheme iosApp -destination 'platform=macOS'` and
  `-destination 'generic/platform=iOS Simulator'` — they catch different
  Swift/bridge mismatches.
- **Compose for Web renders to a `<canvas>`, not real DOM** — there's no
  text to inspect or click via DOM selectors. To actually see a Compose UI
  change, run `./gradlew :webApp:wasmJsBrowserDevelopmentRun --continuous`
  and screenshot it with headless Chrome. Plain `--headless=new` alone
  fails to get a WebGL context (Skia needs one); use:
  `--headless=new --disable-gpu-sandbox --use-gl=angle --use-angle=swiftshader --enable-unsafe-swiftshader --ignore-gpu-blocklist`.
  This is also the most practical way to verify a Compose UI change at
  all, since there's no headless Android/Desktop runner set up here.
- Interactive verification of the native macOS build via AppleScript/System
  Events GUI scripting works but is genuinely flaky — stale processes can
  linger across launches, the accessibility tree doesn't always reflect
  sheets/dialogs, and clicks sometimes don't land. It's worth one clean
  attempt for a behavior a screenshot alone can't confirm (e.g. confirming
  a click actually triggers an action), but don't rabbit-hole on it — a
  successful build plus a static screenshot plus careful code review is
  often the more reliable combination.
