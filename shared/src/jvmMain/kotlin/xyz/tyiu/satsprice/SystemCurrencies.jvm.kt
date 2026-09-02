package xyz.tyiu.satsprice

import java.util.Currency
import java.util.Locale

actual fun systemCurrencies(): List<CurrencyInfo> =
    Currency.getAvailableCurrencies()
        .map { CurrencyInfo(it.currencyCode, it.getDisplayName(Locale.getDefault())) }
        .sortedBy { it.code }

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
