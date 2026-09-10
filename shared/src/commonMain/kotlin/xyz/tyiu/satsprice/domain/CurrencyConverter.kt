package xyz.tyiu.satsprice.domain

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import xyz.tyiu.satsprice.data.ExchangeRates

/**
 * Pure conversion math between BTC, Sats, and a fiat currency, given a base-BTC
 * [ExchangeRates] snapshot (1 BTC = rates[currency] units of that currency).
 *
 * Uses arbitrary-precision [BigDecimal] throughout (rather than Double) so
 * conversions don't lose precision or overflow for very large amounts.
 */
object CurrencyConverter {
    val SATS_PER_BTC: BigDecimal = BigDecimal.fromLong(100_000_000)

    /** The hard-capped maximum number of bitcoin that will ever exist. */
    val MAX_BTC_SUPPLY: BigDecimal = BigDecimal.fromLong(21_000_000)

    /** [MAX_BTC_SUPPLY] expressed in Sats. */
    val MAX_SATS_SUPPLY: BigDecimal = MAX_BTC_SUPPLY.multiply(SATS_PER_BTC)

    /** Divisions aren't guaranteed to terminate, so cap at a generous 50 significant digits. */
    private val DIVISION_MODE = DecimalMode(
        decimalPrecision = 50,
        roundingMode = RoundingMode.ROUND_HALF_AWAY_FROM_ZERO,
    )

    fun btcToSats(btc: BigDecimal): BigDecimal = btc.multiply(SATS_PER_BTC)

    fun satsToBtc(sats: BigDecimal): BigDecimal = sats.divide(SATS_PER_BTC, DIVISION_MODE)

    fun btcToFiat(btc: BigDecimal, rates: ExchangeRates, currency: String): BigDecimal? =
        rates.rates[currency]?.let { rate -> btc.multiply(rate) }

    fun fiatToBtc(fiat: BigDecimal, rates: ExchangeRates, currency: String): BigDecimal? =
        rates.rates[currency]
            ?.takeIf { rate -> !rate.isZero() }
            ?.let { rate -> fiat.divide(rate, DIVISION_MODE) }

    fun satsToFiat(sats: BigDecimal, rates: ExchangeRates, currency: String): BigDecimal? =
        btcToFiat(satsToBtc(sats), rates, currency)

    fun fiatToSats(fiat: BigDecimal, rates: ExchangeRates, currency: String): BigDecimal? =
        fiatToBtc(fiat, rates, currency)?.let { btc -> btcToSats(btc) }

    /** Sats worth 1 unit of a currency whose "1 BTC = ?" rate is [rate], or null if [rate] is zero. */
    fun satsPerCurrencyUnit(rate: BigDecimal): BigDecimal? =
        if (rate.isZero()) null else SATS_PER_BTC.divide(rate, DIVISION_MODE)
}
