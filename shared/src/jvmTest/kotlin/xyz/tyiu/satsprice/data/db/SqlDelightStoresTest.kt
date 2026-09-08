package xyz.tyiu.satsprice.data.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.test.runTest
import xyz.tyiu.satsprice.data.ExchangeRates
import xyz.tyiu.satsprice.db.AppDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SqlDelightStoresTest {

    private fun newDatabase(): AppDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return AppDatabase(driver)
    }

    @Test
    fun exchangeRateStoreRoundTripsRatesPerSource() = runTest {
        val store = SqlDelightExchangeRateStore(newDatabase())
        val rates = ExchangeRates(
            base = "BTC",
            rates = mapOf("USD" to BigDecimal.parseString("65000.12"), "EUR" to BigDecimal.parseString("60000.5")),
            fetchedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
        )

        assertNull(store.loadLastKnownRates("coinbase"))

        store.saveRates("coinbase", rates)
        val loaded = store.loadLastKnownRates("coinbase")
        assertEquals(rates.base, loaded?.base)
        assertEquals(rates.rates, loaded?.rates)
        assertEquals(rates.fetchedAt, loaded?.fetchedAt)

        // A different source's cache stays independent.
        assertNull(store.loadLastKnownRates("coingecko"))

        // Saving again for the same source replaces the prior rates entirely.
        val updated = rates.copy(rates = mapOf("USD" to BigDecimal.parseString("70000")))
        store.saveRates("coinbase", updated)
        assertEquals(updated.rates, store.loadLastKnownRates("coinbase")?.rates)
    }

    @Test
    fun selectedCurrenciesStoreRoundTripsOrder() = runTest {
        val store = SqlDelightSelectedCurrenciesStore(newDatabase())

        assertEquals(emptyList(), store.loadSelectedCurrencies())

        store.saveSelectedCurrencies(listOf("USD", "EUR", "JPY"))
        assertEquals(listOf("USD", "EUR", "JPY"), store.loadSelectedCurrencies())

        // Saving replaces the whole set, including order.
        store.saveSelectedCurrencies(listOf("JPY", "USD"))
        assertEquals(listOf("JPY", "USD"), store.loadSelectedCurrencies())
    }

    @Test
    fun selectedSourceStoreRoundTripsLastUsedSource() = runTest {
        val store = SqlDelightSelectedSourceStore(newDatabase())

        assertNull(store.loadSelectedSourceId())

        store.saveSelectedSourceId("coinbase")
        assertEquals("coinbase", store.loadSelectedSourceId())

        // Saving again replaces the previously selected source.
        store.saveSelectedSourceId("manual")
        assertEquals("manual", store.loadSelectedSourceId())
    }
}
