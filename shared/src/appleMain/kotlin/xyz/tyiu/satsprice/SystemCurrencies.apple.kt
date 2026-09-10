package xyz.tyiu.satsprice

import platform.Foundation.*

actual fun systemCurrencies(): List<CurrencyInfo> {
    @Suppress("UNCHECKED_CAST")
    val codes = NSLocale.Companion.ISOCurrencyCodes as List<String>
    val usedCodes = currentlyUsedCurrencyCodes()
    return codes
        .filter { it in usedCodes }
        .map { code -> CurrencyInfo(code, NSLocale.currentLocale.localizedStringForCurrencyCode(code) ?: code) }
        .sortedBy { it.code }
}

/**
 * `NSLocale.ISOCurrencyCodes` includes withdrawn ISO 4217 codes (Deutsche Mark, French Franc,
 * ...) alongside currently-circulating ones. The currency each ISO country is assigned today is
 * a reliable proxy for "still in use" without needing a maintained exclusion list; precious
 * metals are added back in since they're actively traded but aren't tied to any country.
 */
private fun currentlyUsedCurrencyCodes(): Set<String> {
    @Suppress("UNCHECKED_CAST")
    val countryCodes = NSLocale.Companion.ISOCountryCodes as List<String>
    return countryCodes.mapNotNullTo(mutableSetOf()) { country ->
        NSLocale(localeIdentifier = "_$country").currencyCode
    } + PRECIOUS_METAL_CURRENCY_CODES
}

actual fun localeCurrencyCode(): String? = NSLocale.currentLocale.currencyCode

actual fun currencyDecimalDigits(code: String): Int {
    val formatter = NSNumberFormatter()
    formatter.numberStyle = NSNumberFormatterCurrencyStyle
    formatter.currencyCode = code
    return formatter.maximumFractionDigits.toInt()
}

actual fun regionDisplayName(regionCode: String): String? =
    NSLocale.currentLocale.localizedStringForCountryCode(regionCode)

actual fun supportsFlagEmoji(): Boolean = true
