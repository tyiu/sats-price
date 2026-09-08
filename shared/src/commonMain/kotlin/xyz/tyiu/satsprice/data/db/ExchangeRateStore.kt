package xyz.tyiu.satsprice.data.db

import xyz.tyiu.satsprice.data.ExchangeRates

/** Persists the last known [ExchangeRates] fetched from each price source, keyed by [ExchangeRateSource.id]. */
interface ExchangeRateStore {
    suspend fun loadLastKnownRates(sourceId: String): ExchangeRates?
    suspend fun saveRates(sourceId: String, rates: ExchangeRates)
}

expect fun createExchangeRateStore(): ExchangeRateStore
