package xyz.tyiu.satsprice.data

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManualExchangeRateSourceTest {

    @Test
    fun getRates_throwsWhenNoRateHasBeenSet() = runTest {
        val source = ManualExchangeRateSource()
        assertFailsWith<IllegalStateException> { source.getRates("BTC") }
    }

    @Test
    fun getRates_returnsTheConfiguredRate() = runTest {
        val source = ManualExchangeRateSource()
        source.rate = BigDecimal.parseString("65000.5")

        val result = source.getRates("BTC")

        assertEquals("BTC", result.base)
        assertEquals(BigDecimal.parseString("65000.5"), result.rates["USD"])
        assertEquals(setOf("USD"), result.rates.keys)
    }

    @Test
    fun getRates_usesTheConfiguredCurrencyCode() = runTest {
        val source = ManualExchangeRateSource()
        source.currencyCode = "EUR"
        source.rate = BigDecimal.parseString("60000")

        val result = source.getRates("BTC")

        assertEquals(BigDecimal.parseString("60000"), result.rates["EUR"])
        assertEquals(setOf("EUR"), result.rates.keys)
    }
}
