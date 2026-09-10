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
 * Which currency codes `Intl.supportedValuesOf('currency')` returns is ICU-version-dependent, and
 * some browsers' ICU data includes codes for currencies retired decades ago (AFA, ALK, AOK, AON,
 * AOR, ...) alongside genuinely current ones — a moving target, and not one an exclusion list can
 * keep up with. So rather than trying to exclude every historical code some browser might expose,
 * this allowlists the ones actually still in use instead: unlike JVM/Android/Apple, there's no web
 * API to derive "currently assigned to some country" the way `java.util.Currency`/`NSLocale` do
 * (see [localeCurrencyCode] below), so this is a manually maintained list, taken directly from the
 * JVM's own live-derived set (checked 2026-09-10). Needs a new entry whenever a currency changes.
 */
private val ACTIVE_CURRENCY_CODES = setOf(
    "AED", "AFN", "ALL", "AMD", "AOA", "ARS", "AUD", "AWG", "AZN", "BAM", "BBD", "BDT", "BGN",
    "BHD", "BIF", "BMD", "BND", "BOB", "BRL", "BSD", "BTN", "BWP", "BYN", "BZD", "CAD", "CDF",
    "CHF", "CLP", "CNY", "COP", "CRC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD", "EGP",
    "ERN", "ETB", "EUR", "FJD", "FKP", "GBP", "GEL", "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD",
    "HKD", "HNL", "HTG", "HUF", "IDR", "ILS", "INR", "IQD", "IRR", "ISK", "JMD", "JOD", "JPY",
    "KES", "KGS", "KHR", "KMF", "KPW", "KRW", "KWD", "KYD", "KZT", "LAK", "LBP", "LKR", "LRD",
    "LSL", "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK",
    "MXN", "MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB", "PEN", "PGK",
    "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR", "SBD", "SCR", "SDG",
    "SEK", "SGD", "SHP", "SLE", "SOS", "SRD", "SSP", "STN", "SVC", "SYP", "SZL", "THB", "TJS",
    "TMT", "TND", "TOP", "TRY", "TTD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU", "UZS", "VES",
    "VND", "VUV", "WST", "XAF", "XCD", "XCG", "XOF", "XPF", "YER", "ZAR", "ZMW", "ZWG",
) + PRECIOUS_METAL_CURRENCY_CODES

actual fun systemCurrencies(): List<CurrencyInfo> =
    jsSupportedCurrencyCodes()
        .toList()
        .map { it.toString() }
        .filter { it in ACTIVE_CURRENCY_CODES }
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
