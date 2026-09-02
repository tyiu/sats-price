package xyz.tyiu.satsprice

import platform.Foundation.*

actual fun systemCurrencies(): List<CurrencyInfo> {
    @Suppress("UNCHECKED_CAST")
    val codes = NSLocale.Companion.ISOCurrencyCodes as List<String>
    return codes
        .map { code -> CurrencyInfo(code, NSLocale.currentLocale.localizedStringForCurrencyCode(code) ?: code) }
        .sortedBy { it.code }
}

actual fun localeCurrencyCode(): String? = NSLocale.currentLocale.currencyCode

actual fun currencyDecimalDigits(code: String): Int {
    val formatter = NSNumberFormatter()
    formatter.numberStyle = NSNumberFormatterCurrencyStyle
    formatter.currencyCode = code
    return formatter.maximumFractionDigits.toInt()
}
