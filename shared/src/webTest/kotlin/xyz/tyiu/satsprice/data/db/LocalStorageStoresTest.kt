package xyz.tyiu.satsprice.data.db

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.browser.localStorage
import kotlinx.coroutines.test.runTest
import xyz.tyiu.satsprice.data.ExchangeRates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/**
 * A fresh store instance stands in for a page reload: since these stores are backed by
 * localStorage rather than an in-memory field, one instance's writes must be visible to another.
 */
class LocalStorageStoresTest {

    @Test
    fun exchangeRateStorePersistsAcrossInstances() = runTest {
        localStorage.clear()
        assertNull(createExchangeRateStore().loadLastKnownRates("coinbase"))

        val rates = ExchangeRates(
            base = "BTC",
            rates = mapOf("USD" to BigDecimal.parseString("65000.12"), "EUR" to BigDecimal.parseString("60000.5")),
            fetchedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
        )
        createExchangeRateStore().saveRates("coinbase", rates)

        val reloaded = createExchangeRateStore().loadLastKnownRates("coinbase")
        assertEquals(rates.base, reloaded?.base)
        assertEquals(rates.rates, reloaded?.rates)
        assertEquals(rates.fetchedAt, reloaded?.fetchedAt)

        // A different source's cache stays independent.
        assertNull(createExchangeRateStore().loadLastKnownRates("coingecko"))
    }

    @Test
    fun selectedCurrenciesStorePersistsAcrossInstances() = runTest {
        localStorage.clear()
        assertEquals(emptyList(), createSelectedCurrenciesStore().loadSelectedCurrencies())

        createSelectedCurrenciesStore().saveSelectedCurrencies(listOf("USD", "EUR", "JPY"))
        assertEquals(listOf("USD", "EUR", "JPY"), createSelectedCurrenciesStore().loadSelectedCurrencies())
    }

    @Test
    fun selectedSourceStorePersistsAcrossInstances() = runTest {
        localStorage.clear()
        assertNull(createSelectedSourceStore().loadSelectedSourceId())

        createSelectedSourceStore().saveSelectedSourceId("coingecko")
        assertEquals("coingecko", createSelectedSourceStore().loadSelectedSourceId())
    }
}
