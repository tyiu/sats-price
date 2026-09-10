package xyz.tyiu.satsprice

import java.util.Currency
import java.util.Locale

actual fun systemCurrencies(): List<CurrencyInfo> =
    Currency.getAvailableCurrencies()
        .filter { it.currencyCode in currentlyUsedCurrencyCodes() }
        .map { CurrencyInfo(it.currencyCode, it.getDisplayName(Locale.getDefault())) }
        .sortedBy { it.code }

/**
 * `Currency.getAvailableCurrencies()` includes every ISO 4217 code the JDK has ever known about —
 * historical currencies (Deutsche Mark, French Franc, ...) and test/placeholder codes (XTS, XXX)
 * included. The currency currently assigned to each ISO country is a reliable proxy for "still in
 * use" without needing a maintained exclusion list; precious metals are added back in since
 * they're actively traded but aren't tied to any country.
 */
@Suppress("DEPRECATION") // Locale(language, country) still works fine; Locale.of() needs newer Android API levels.
private fun currentlyUsedCurrencyCodes(): Set<String> =
    Locale.getISOCountries().mapNotNullTo(mutableSetOf()) { country ->
        try {
            Currency.getInstance(Locale("", country))?.currencyCode
        } catch (e: IllegalArgumentException) {
            null
        }
    } + PRECIOUS_METAL_CURRENCY_CODES

actual fun localeCurrencyCode(): String? = try {
    Currency.getInstance(Locale.getDefault())?.currencyCode
} catch (e: IllegalArgumentException) {
    null
}

actual fun currencyDecimalDigits(code: String): Int = try {
    Currency.getInstance(code).defaultFractionDigits.takeIf { it >= 0 } ?: 2
} catch (e: IllegalArgumentException) {
    2
}

@Suppress("DEPRECATION") // Locale(language, country) still works fine; Locale.of() needs newer Android API levels.
actual fun regionDisplayName(regionCode: String): String? {
    if (regionCode !in Locale.getISOCountries()) return null
    return Locale("", regionCode).getDisplayCountry(Locale.getDefault())
}
