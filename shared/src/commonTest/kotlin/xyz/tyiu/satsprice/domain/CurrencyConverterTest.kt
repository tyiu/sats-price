package xyz.tyiu.satsprice.domain

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import xyz.tyiu.satsprice.data.ExchangeRates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class CurrencyConverterTest {

    private val rates = ExchangeRates(
        base = "BTC",
        rates = mapOf(
            "USD" to BigDecimal.fromLong(50_000),
            "ZERO" to BigDecimal.fromLong(0),
        ),
        fetchedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun btcToSats_convertsWholeBitcoin() {
        assertEquals(BigDecimal.fromLong(100_000_000), CurrencyConverter.btcToSats(BigDecimal.fromLong(1)))
    }

    @Test
    fun satsToBtc_isInverseOfBtcToSats() {
        assertEquals(BigDecimal.parseString("1.5"), CurrencyConverter.satsToBtc(BigDecimal.fromLong(150_000_000)))
    }

    @Test
    fun btcToFiat_usesRateForCurrency() {
        assertEquals(BigDecimal.fromLong(100_000), CurrencyConverter.btcToFiat(BigDecimal.fromLong(2), rates, "USD"))
    }

    @Test
    fun fiatToBtc_isInverseOfBtcToFiat() {
        assertEquals(BigDecimal.fromLong(2), CurrencyConverter.fiatToBtc(BigDecimal.fromLong(100_000), rates, "USD"))
    }

    @Test
    fun fiatToBtc_returnsNullForUnknownCurrency() {
        assertNull(CurrencyConverter.fiatToBtc(BigDecimal.fromLong(100), rates, "XYZ"))
    }

    @Test
    fun fiatToBtc_returnsNullForZeroRate() {
        assertNull(CurrencyConverter.fiatToBtc(BigDecimal.fromLong(100), rates, "ZERO"))
    }

    @Test
    fun satsToFiat_and_fiatToSats_roundTrip() {
        val fiat = CurrencyConverter.satsToFiat(BigDecimal.fromLong(100_000_000), rates, "USD")
        assertEquals(BigDecimal.fromLong(50_000), fiat)

        val sats = CurrencyConverter.fiatToSats(BigDecimal.fromLong(50_000), rates, "USD")
        assertEquals(BigDecimal.fromLong(100_000_000), sats)
    }

    @Test
    fun satsPerCurrencyUnit_isInverseOfTheBtcRate() {
        // 1 BTC = 50,000 USD, so 1 USD is worth 100,000,000 / 50,000 = 2,000 Sats.
        assertEquals(BigDecimal.fromLong(2_000), CurrencyConverter.satsPerCurrencyUnit(BigDecimal.fromLong(50_000)))
    }

    @Test
    fun satsPerCurrencyUnit_returnsNullForZeroRate() {
        assertNull(CurrencyConverter.satsPerCurrencyUnit(BigDecimal.fromLong(0)))
    }

    @Test
    fun handlesAmountsFarBeyondDoubleOrLongRange() {
        // 10^30 BTC: overflows both Double's exact-integer range and Long.MAX_VALUE.
        val hugeBtc = BigDecimal.parseString("1000000000000000000000000000000")
        val sats = CurrencyConverter.btcToSats(hugeBtc)
        assertEquals(BigDecimal.parseString("100000000000000000000000000000000000000"), sats)
    }
}
