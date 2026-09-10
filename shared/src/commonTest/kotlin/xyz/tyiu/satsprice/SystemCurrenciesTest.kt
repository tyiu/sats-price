package xyz.tyiu.satsprice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun matchesCurrencySearch_matchesByCodeOrDisplayNameCaseInsensitively() {
        val usd = CurrencyInfo("USD", "US Dollar")

        assertTrue(matchesCurrencySearch(usd, ""))
        assertTrue(matchesCurrencySearch(usd, "usd"))
        assertTrue(matchesCurrencySearch(usd, "USD"))
        assertTrue(matchesCurrencySearch(usd, "dollar"))
        assertTrue(matchesCurrencySearch(usd, "US Doll"))
        assertFalse(matchesCurrencySearch(usd, "EUR"))
        assertFalse(matchesCurrencySearch(usd, "Euro"))
    }

    @Test
    fun matchesCurrencySearch_matchesByIssuingCountryName() {
        // "Canadian Dollar" doesn't contain "Canada" as a substring, so this only passes if the
        // country-name path (rather than just code/display-name matching) is actually consulted.
        val cad = CurrencyInfo("CAD", "Canadian Dollar")
        val countryName = regionDisplayName("CA")
        assertTrue(countryName != null && countryName.isNotBlank(), "expected a display name for CA")

        assertTrue(matchesCurrencySearch(cad, countryName))
        assertTrue(matchesCurrencySearch(cad, countryName.lowercase()))
        assertFalse(matchesCurrencySearch(cad, "Definitely not a matching country name"))
    }

    @Test
    fun matchesCurrencySearch_matchesEurozoneByEuropeanUnionName() {
        // "EU" isn't a real ISO 3166-1 country code, but every platform's locale data resolves it
        // correctly anyway (CLDR defines it as a grouping in its own right) — regionDisplayName()
        // isn't special-cased for it.
        val eur = CurrencyInfo("EUR", "Euro")
        val euName = regionDisplayName("EU")
        assertTrue(euName != null && euName.isNotBlank(), "expected a display name for EU")

        assertTrue(matchesCurrencySearch(eur, euName))
        assertTrue(matchesCurrencySearch(eur, euName.lowercase()))
        assertFalse(matchesCurrencySearch(eur, "Definitely not a matching country name"))
    }
}
