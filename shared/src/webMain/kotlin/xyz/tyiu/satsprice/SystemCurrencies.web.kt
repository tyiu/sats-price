@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package xyz.tyiu.satsprice

private fun jsSupportedCurrencyCodes(): JsArray<JsString> = js("Intl.supportedValuesOf('currency')")

// A chained `new Foo(x).bar()` inside js() can misparse (`new` binding to the whole chain rather
// than just the constructor call), so the constructor result is bound to a variable first.
private fun jsCurrencyDisplayName(code: String): String = js(
    "(function() { var names = new Intl.DisplayNames(['en'], { type: 'currency' }); return names.of(code); })()",
)

actual fun systemCurrencies(): List<CurrencyInfo> =
    jsSupportedCurrencyCodes()
        .toList()
        .map { jsCode ->
            val code = jsCode.toString()
            CurrencyInfo(code, jsCurrencyDisplayName(code))
        }
        .sortedBy { it.code }

/**
 * No web API infers a currency from a locale: `Intl.NumberFormat`/`Intl.Locale` require an
 * explicit `currency` option rather than deriving one, and there's no TC39 proposal that adds
 * this. Doing it reliably would mean maintaining our own region-to-currency table, so this is
 * left undetected on web rather than guessing.
 */
actual fun localeCurrencyCode(): String? = null

private fun jsCurrencyFractionDigits(code: String): Int = js(
    """(function() { var fmt = new Intl.NumberFormat('en', { style: 'currency', currency: code }); return fmt.resolvedOptions().maximumFractionDigits; })()""",
)

actual fun currencyDecimalDigits(code: String): Int = try {
    jsCurrencyFractionDigits(code)
} catch (e: Exception) {
    2
}

// Falls back to the region code itself (rather than a JS null/undefined) when unrecognized, since
// a plain `-> String?` return type doesn't reliably round-trip through js() interop here; that
// fallback is filtered back out to null actual-side below, same as the other platforms.
private fun jsRegionDisplayName(regionCode: String): String = js(
    """(function() {
        try {
            var names = new Intl.DisplayNames(['en'], { type: 'region' });
            return names.of(regionCode) || regionCode;
        } catch (e) {
            return regionCode;
        }
    })()""",
)

actual fun regionDisplayName(regionCode: String): String? =
    jsRegionDisplayName(regionCode).takeIf { it != regionCode }
