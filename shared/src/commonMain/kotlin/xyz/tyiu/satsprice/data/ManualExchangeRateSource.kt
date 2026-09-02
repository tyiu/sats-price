package xyz.tyiu.satsprice.data

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.time.Clock

/** A source backed by a rate the user types in themselves, rather than a network fetch. */
class ManualExchangeRateSource : ExchangeRateSource {
    override val id: String = "manual"
    override val displayName: String = "Manual"

    var currencyCode: String = "USD"
    var rate: BigDecimal? = null

    override suspend fun getRates(base: String): ExchangeRates {
        val value = rate ?: throw IllegalStateException("Enter a BTC price in $currencyCode below")
        return ExchangeRates(
            base = "BTC",
            rates = mapOf(currencyCode to value),
            fetchedAt = Clock.System.now(),
        )
    }
}
