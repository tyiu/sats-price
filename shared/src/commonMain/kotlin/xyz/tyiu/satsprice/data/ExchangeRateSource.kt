package xyz.tyiu.satsprice.data

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.time.Instant

/**
 * A source of BTC exchange rates. Implementations fetch rates for one unit of [base]
 * expressed in various currency codes, so new providers can be added without touching
 * conversion or UI code.
 */
interface ExchangeRateSource {
    val id: String
    val displayName: String

    suspend fun getRates(base: String = "BTC"): ExchangeRates
}

data class ExchangeRates(
    val base: String,
    val rates: Map<String, BigDecimal>,
    val fetchedAt: Instant,
)
