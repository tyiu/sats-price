@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package xyz.tyiu.satsprice

private fun jsSupportedCurrencyCodes(): JsArray<JsString> = js("Intl.supportedValuesOf('currency')")

private fun jsCurrencyDisplayName(code: String): String =
    js("new Intl.DisplayNames(['en'], { type: 'currency' }).of(code)")

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

private fun jsCurrencyFractionDigits(code: String): Int =
    js("new Intl.NumberFormat('en', { style: 'currency', currency: code }).resolvedOptions().maximumFractionDigits")

actual fun currencyDecimalDigits(code: String): Int = try {
    jsCurrencyFractionDigits(code)
} catch (e: Exception) {
    2
}
