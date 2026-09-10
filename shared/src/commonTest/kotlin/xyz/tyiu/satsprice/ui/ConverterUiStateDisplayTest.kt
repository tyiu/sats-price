package xyz.tyiu.satsprice.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ConverterUiStateDisplayTest {

    @Test
    fun oneCurrencyToSats_isInverseOfTheBtcRate() {
        val state = ConverterUiState(defaultCurrencyCode = "USD", rateDisplays = mapOf("USD" to "50000.00"))

        // 1 BTC = 50,000 USD, so 1 USD is worth 100,000,000 / 50,000 = 2,000 Sats.
        assertEquals("2000", state.oneCurrencyToSats())
    }

    @Test
    fun oneCurrencyToSats_roundsToAWholeNumberOfSats() {
        // 1 BTC = 78,293.19 USD, so 1 USD is worth ~1,277.24 Sats — never shown fractionally.
        val state = ConverterUiState(defaultCurrencyCode = "USD", rateDisplays = mapOf("USD" to "78293.19"))

        assertEquals("1277", state.oneCurrencyToSats())
    }

    @Test
    fun oneCurrencyToSats_isEmptyWheneverDefaultCurrencyRateIs() {
        // No rate for the default currency at all (e.g. Manual with nothing typed in yet).
        val noRate = ConverterUiState(defaultCurrencyCode = "USD", rateDisplays = emptyMap())
        assertEquals("", noRate.defaultCurrencyRate())
        assertEquals("", noRate.oneCurrencyToSats())

        // A rate is known, just not for the default currency.
        val otherCurrencyOnly = ConverterUiState(defaultCurrencyCode = "USD", rateDisplays = mapOf("EUR" to "45000"))
        assertEquals("", otherCurrencyOnly.defaultCurrencyRate())
        assertEquals("", otherCurrencyOnly.oneCurrencyToSats())
    }
}
