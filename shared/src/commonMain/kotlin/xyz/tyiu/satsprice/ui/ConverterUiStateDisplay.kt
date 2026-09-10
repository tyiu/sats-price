package xyz.tyiu.satsprice.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.tyiu.satsprice.CurrencyInfo
import xyz.tyiu.satsprice.domain.CurrencyConverter
import xyz.tyiu.satsprice.domain.formatAmount
import xyz.tyiu.satsprice.domain.toBigDecimalOrNull
import kotlin.time.Instant

/** Derived display strings/flags shared between the Compose UI and the iOS SwiftUI bridge. */

fun ConverterUiState.statusLine(): String {
    if (isManualSource) return ""
    val updated = lastUpdated?.let { "updated ${it.toDateTimeString()}" } ?: "loading rates…"
    return if (sourceName.isEmpty()) updated else "via $sourceName, $updated"
}

fun ConverterUiState.exceedsMaxSupply(): Boolean {
    val btcOverCap = btcAmount.toBigDecimalOrNull()?.let { it > CurrencyConverter.MAX_BTC_SUPPLY } ?: false
    val satsOverCap = satsAmount.toBigDecimalOrNull()?.let { it > CurrencyConverter.MAX_SATS_SUPPLY } ?: false
    return btcOverCap || satsOverCap
}

/** The current "1 BTC = ?" rate in [ConverterUiState.defaultCurrencyCode], as a plain number. */
fun ConverterUiState.defaultCurrencyRate(): String =
    rateDisplays[defaultCurrencyCode]?.takeIf { it.isNotEmpty() } ?: ""

/** The current "1 [ConverterUiState.defaultCurrencyCode] = ? Sats" rate, as a plain number. */
fun ConverterUiState.oneCurrencyToSats(): String {
    val rate = rateDisplays[defaultCurrencyCode]?.toBigDecimalOrNull() ?: return ""
    return CurrencyConverter.satsPerCurrencyUnit(rate)?.let { formatAmount(it, 2) } ?: ""
}

/** The pinned, non-removable "Current Currency" shown in the currency picker's own section. */
fun ConverterUiState.currentCurrency(): CurrencyInfo =
    availableFiatCurrencies.find { it.code == defaultCurrencyCode } ?: CurrencyInfo(defaultCurrencyCode, defaultCurrencyCode)

/** Selected currencies other than [ConverterUiState.defaultCurrencyCode], in the user's chosen order. */
fun ConverterUiState.selectedOtherCurrencies(): List<CurrencyInfo> =
    selectedFiatCurrencies.filter { it != defaultCurrencyCode }
        .mapNotNull { code -> availableFiatCurrencies.find { it.code == code } }

/** Currencies not yet added, offered in the currency picker's "Currencies" section. */
fun ConverterUiState.unselectedCurrencies(): List<CurrencyInfo> =
    availableFiatCurrencies.filterNot { it.code in selectedFiatCurrencies }

/** Whether the active price source quotes a rate for [code] — every currency is listed regardless. */
fun ConverterUiState.isPriced(code: String): Boolean = code in pricedCurrencyCodes

private fun Instant.toDateTimeString(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    return local.toString().substringBefore('.').replace('T', ' ')
}
