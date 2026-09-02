package xyz.tyiu.satsprice.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.tyiu.satsprice.domain.CurrencyConverter
import xyz.tyiu.satsprice.domain.toBigDecimalOrNull
import kotlin.time.Instant

/** Derived display strings/flags shared between the Compose UI and the iOS SwiftUI bridge. */

fun ConverterUiState.statusLine(): String {
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

private fun Instant.toDateTimeString(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    return local.toString().substringBefore('.').replace('T', ' ')
}
