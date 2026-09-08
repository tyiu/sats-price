package xyz.tyiu.satsprice

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `java.util.Currency.getAvailableCurrencies()` includes every ISO 4217 code the JDK has ever
 * known about — withdrawn currencies and test/placeholder codes included — so [systemCurrencies]
 * filters those out, while keeping precious metals, which are still actively traded.
 */
class SystemCurrenciesJvmTest {

    @Test
    fun systemCurrencies_excludesWithdrawnAndNonCountryCodes() {
        val codes = systemCurrencies().map { it.code }.toSet()

        assertFalse("DEM" in codes, "Deutsche Mark was withdrawn when Germany adopted the Euro")
        assertFalse("FRF" in codes, "French Franc was withdrawn when France adopted the Euro")
        assertFalse("XTS" in codes, "XTS is reserved for testing, not a real currency")
        assertFalse("XXX" in codes, "XXX represents 'no currency', not a real currency")

        assertTrue("USD" in codes)
        assertTrue("EUR" in codes)
        assertTrue("ALL" in codes)
        assertTrue("XAU" in codes, "Gold is still actively traded even though it isn't tied to a country")
        assertTrue("XAG" in codes, "Silver is still actively traded even though it isn't tied to a country")
    }
}
