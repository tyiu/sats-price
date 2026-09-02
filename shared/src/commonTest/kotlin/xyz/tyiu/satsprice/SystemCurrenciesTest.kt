package xyz.tyiu.satsprice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemCurrenciesTest {

    @Test
    fun systemCurrencies_includesUsdWithADisplayName() {
        val currencies = systemCurrencies()

        assertTrue(currencies.isNotEmpty())
        val usd = currencies.firstOrNull { it.code == "USD" }
        assertTrue(usd != null, "expected USD to be present")
        assertTrue(usd.displayName.isNotBlank())
    }

    @Test
    fun currencyDecimalDigits_matchesKnownConventions() {
        assertEquals(2, currencyDecimalDigits("USD"))
        assertEquals(0, currencyDecimalDigits("JPY"))
        assertEquals(3, currencyDecimalDigits("BHD"))
    }
}
