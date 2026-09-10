@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package xyz.tyiu.satsprice

private fun jsSupportedCurrencyCodes(): JsArray<JsString> = js("Intl.supportedValuesOf('currency')")

// A chained `new Foo(x).bar()` inside js() can misparse (`new` binding to the whole chain rather
// than just the constructor call), so the constructor result is bound to a variable first.
// `undefined` (rather than a fixed locale like 'en') uses the browser's own locale — code always
// comes from jsSupportedCurrencyCodes() itself, so .of(code) is guaranteed to resolve.
private fun jsCurrencyDisplayName(code: String): String = js(
    "(function() { var names = new Intl.DisplayNames(undefined, { type: 'currency' }); return names.of(code); })()",
)

/**
 * `Intl.supportedValuesOf('currency')` includes a lot of codes for currencies that have actually
 * been withdrawn/superseded since — which ones depends on the browser's ICU data, so this list is
 * necessarily incomplete and grows over time as more turn up. Unlike JVM/Android/Apple, there's
 * no web API to derive "currently assigned to some country" the way `java.util.Currency`/
 * `NSLocale` do (see [localeCurrencyCode] below), so this is a manually maintained exclusion list
 * instead. Needs a new entry whenever another currency is retired, or another old one turns up.
 */
private val WITHDRAWN_CURRENCY_CODES = setOf(
    "ANG", // Netherlands Antillean Guilder — replaced by XCG (Caribbean Guilder), April 2025
    "CUC", // Cuban Convertible Peso — unified into CUP, January 2021
    "HRK", // Croatian Kuna — replaced by EUR, January 2023
    "SLL", // Sierra Leonean Leone (old) — redenominated to SLE, 2022
    "ZWL", // Zimbabwean Dollar (old) — replaced by ZWG (Zimbabwe Gold), April 2024

    // Old Angolan Kwanza, through several redenominations — replaced by AOA
    "AOK", "AON", "AOR",
    "AFA", // Afghan Afghani (old) — replaced by AFN
    "ALK", // Albanian Lek (old) — replaced by ALL

    // Pre-euro legacy currencies, withdrawn on adoption of the euro
    "ADP", // Andorran Peseta
    "ATS", // Austrian Schilling
    "BEF", // Belgian Franc
    "CYP", // Cypriot Pound
    "DEM", // Deutsche Mark
    "EEK", // Estonian Kroon
    "ESP", // Spanish Peseta
    "FIM", // Finnish Markka
    "FRF", // French Franc
    "GRD", // Greek Drachma
    "IEP", // Irish Pound
    "ITL", // Italian Lira
    "LTL", // Lithuanian Litas
    "LUF", // Luxembourg Franc
    "LVL", // Latvian Lats
    "MTL", // Maltese Lira
    "NLG", // Dutch Guilder
    "PTE", // Portuguese Escudo
    "SIT", // Slovenian Tolar
    "SKK", // Slovak Koruna
)

actual fun systemCurrencies(): List<CurrencyInfo> =
    jsSupportedCurrencyCodes()
        .toList()
        .map { it.toString() }
        .filterNot { it in WITHDRAWN_CURRENCY_CODES }
        .map { code -> CurrencyInfo(code, jsCurrencyDisplayName(code)) }
        .sortedBy { it.code }

/**
 * No web API infers a currency from a locale: `Intl.NumberFormat`/`Intl.Locale` require an
 * explicit `currency` option rather than deriving one, and there's no TC39 proposal that adds
 * this. Doing it reliably would mean maintaining our own region-to-currency table, so this is
 * left undetected on web rather than guessing.
 */
actual fun localeCurrencyCode(): String? = null

// A currency's decimal-digit count is a property of the currency, not the locale (e.g. JPY's is
// always 0), so — unlike jsCurrencyDisplayName/jsRegionDisplayName above — locale is only a
// required constructor argument here, not something the result actually depends on; 'en' is as
// good as any other.
private fun jsCurrencyFractionDigits(code: String): Int = js(
    """(function() { var fmt = new Intl.NumberFormat('en', { style: 'currency', currency: code }); return fmt.resolvedOptions().maximumFractionDigits; })()""",
)

actual fun currencyDecimalDigits(code: String): Int = try {
    jsCurrencyFractionDigits(code)
} catch (e: Exception) {
    2
}

// `undefined` (rather than a fixed locale like 'en') uses the browser's own locale, matching the
// other platforms' regionDisplayName(). Falls back to the region code itself (rather than a JS
// null/undefined) when unrecognized, since a plain `-> String?` return type doesn't reliably
// round-trip through js() interop here; that fallback is filtered back out to null actual-side
// below, same as the other platforms.
private fun jsRegionDisplayName(regionCode: String): String = js(
    """(function() {
        try {
            var names = new Intl.DisplayNames(undefined, { type: 'region' });
            return names.of(regionCode) || regionCode;
        } catch (e) {
            return regionCode;
        }
    })()""",
)

actual fun regionDisplayName(regionCode: String): String? =
    jsRegionDisplayName(regionCode).takeIf { it != regionCode }

// Compose for Web renders everything through Skia onto a <canvas> rather than through the
// browser's own text stack, so it can't fall back to the browser/OS's color-emoji font the way
// native targets can — a flag's regional-indicator codepoints draw as empty boxes instead.
actual fun supportsFlagEmoji(): Boolean = false
