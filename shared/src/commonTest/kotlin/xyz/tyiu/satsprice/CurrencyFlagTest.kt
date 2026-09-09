package xyz.tyiu.satsprice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurrencyFlagTest {

    @Test
    fun currencyFlagEmoji_derivesFlagFromCountryPrefix() {
        assertEquals("🇺🇸", currencyFlagEmoji("USD")) // US
        assertEquals("🇬🇧", currencyFlagEmoji("GBP")) // GB
        assertEquals("🇯🇵", currencyFlagEmoji("JPY")) // JP
    }

    @Test
    fun currencyFlagEmoji_usesEuFlagForEuro() {
        assertEquals("🇪🇺", currencyFlagEmoji("EUR")) // EU
    }

    @Test
    fun currencyFlagEmoji_showsAllFlagsForCurrenciesSharedByMultipleCountries() {
        assertEquals("🇦🇬 🇦🇮 🇩🇲 🇬🇩 🇰🇳 🇱🇨 🇲🇸 🇻🇨", currencyFlagEmoji("XCD"))
        assertEquals("🇧🇫 🇧🇯 🇨🇮 🇬🇼 🇲🇱 🇳🇪 🇸🇳 🇹🇬", currencyFlagEmoji("XOF"))
        assertEquals("🇨🇫 🇨🇬 🇨🇲 🇬🇦 🇬🇶 🇹🇩", currencyFlagEmoji("XAF"))
        assertEquals("🇳🇨 🇵🇫 🇼🇫", currencyFlagEmoji("XPF"))
    }

    @Test
    fun currencyFlagEmoji_returnsNullForNonNationalCodes() {
        // ISO 4217 reserves the remaining "X"-prefixed codes for precious metals, testing codes,
        // and other non-national codes with no country to show.
        assertNull(currencyFlagEmoji("XAU"))
        assertNull(currencyFlagEmoji("XXX"))
    }

    @Test
    fun currencyFlagEmoji_returnsNullForInvalidCodes() {
        assertNull(currencyFlagEmoji(""))
        assertNull(currencyFlagEmoji("U"))
        assertNull(currencyFlagEmoji("123"))
    }
}
