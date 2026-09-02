package xyz.tyiu.satsprice

data class CurrencyInfo(val code: String, val displayName: String)

/** All ISO 4217 currencies the current platform knows about, with localized display names. */
expect fun systemCurrencies(): List<CurrencyInfo>

/** The currency associated with the user's current locale, if the platform can determine one. */
expect fun localeCurrencyCode(): String?

/** The conventional number of decimal places for [code] (e.g. 2 for USD, 0 for JPY, 3 for BHD). */
expect fun currencyDecimalDigits(code: String): Int
