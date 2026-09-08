package xyz.tyiu.satsprice.ui

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import xyz.tyiu.satsprice.data.ExchangeRateSource
import xyz.tyiu.satsprice.data.ExchangeRates
import xyz.tyiu.satsprice.data.db.ExchangeRateStore
import xyz.tyiu.satsprice.data.db.SelectedCurrenciesStore
import xyz.tyiu.satsprice.data.db.SelectedSourceStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

private class FakeExchangeRateSource(
    override val id: String,
    override val displayName: String,
    private val rates: Map<String, String>,
) : ExchangeRateSource {
    override suspend fun getRates(base: String): ExchangeRates = ExchangeRates(
        base = base,
        rates = rates.mapValues { (_, value) -> BigDecimal.parseString(value) },
        fetchedAt = Clock.System.now(),
    )
}

private class InMemoryExchangeRateStore : ExchangeRateStore {
    override suspend fun loadLastKnownRates(sourceId: String): ExchangeRates? = null
    override suspend fun saveRates(sourceId: String, rates: ExchangeRates) = Unit
}

private class InMemorySelectedCurrenciesStore : SelectedCurrenciesStore {
    override suspend fun loadSelectedCurrencies(): List<String> = emptyList()
    override suspend fun saveSelectedCurrencies(codes: List<String>) = Unit
}

private class InMemorySelectedSourceStore : SelectedSourceStore {
    override suspend fun loadSelectedSourceId(): String? = null
    override suspend fun saveSelectedSourceId(sourceId: String) = Unit
}

/**
 * A currency the active source doesn't price (like CoinGecko not quoting BTC in Albanian Lek)
 * must still be offered rather than hidden — only its "unpriced" status should reflect that.
 *
 * Uses [UnconfinedTestDispatcher] directly (not `runTest`) so the ViewModel's `init` block — which
 * launches a `while (isActive) { refresh(); delay(...) }` loop that runs for the ViewModel's whole
 * lifetime — runs its first iteration eagerly and synchronously, then simply parks at `delay()`
 * without a scheduler driving it further. `runTest`'s automatic advance-to-idle at scope exit would
 * otherwise try to drain that loop forever, since it never completes on its own.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PriceViewModelCurrencyAvailabilityTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun currencyWithoutARateStillAppearsButIsMarkedUnpriced() {
        val limitedSource = FakeExchangeRateSource(
            id = "coingecko",
            displayName = "CoinGecko",
            rates = mapOf("USD" to "65000"),
        )
        val viewModel = PriceViewModel(
            coinbaseSource = limitedSource,
            coinGeckoSource = limitedSource,
            exchangeRateStore = InMemoryExchangeRateStore(),
            selectedCurrenciesStore = InMemorySelectedCurrenciesStore(),
            selectedSourceStore = InMemorySelectedSourceStore(),
        )

        val state = viewModel.uiState.value
        assertTrue(state.availableFiatCurrencies.any { it.code == "ALL" }, "ALL should still be listed")
        assertFalse(state.isPriced("ALL"), "ALL has no rate from this source, so it should be marked unpriced")
        assertTrue(state.isPriced("USD"), "USD does have a rate from this source")
    }
}
