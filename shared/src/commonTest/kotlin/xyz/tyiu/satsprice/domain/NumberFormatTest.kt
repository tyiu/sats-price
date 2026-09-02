package xyz.tyiu.satsprice.domain

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumberFormatTest {

    @Test
    fun formatAmount_trimsTrailingZeros() {
        assertEquals("1.5", formatAmount(BigDecimal.parseString("1.5"), 8))
    }

    @Test
    fun formatAmount_dropsDecimalPointWhenWhole() {
        assertEquals("100000000", formatAmount(BigDecimal.fromLong(100_000_000), 0))
        assertEquals("1", formatAmount(BigDecimal.fromLong(1), 8))
    }

    @Test
    fun formatAmount_roundsToRequestedPrecision() {
        assertEquals("0.12346", formatAmount(BigDecimal.parseString("0.123456"), 5))
    }

    @Test
    fun formatAmount_handlesAmountsBeyondLongRange() {
        val huge = BigDecimal.parseString("123456789012345678901234567890.5")
        assertEquals("123456789012345678901234567890.5", formatAmount(huge, 8))
    }

    @Test
    fun toBigDecimalOrNull_parsesPlainDecimals() {
        assertEquals(BigDecimal.parseString("1.5"), "1.5".toBigDecimalOrNull())
        assertEquals(BigDecimal.fromLong(100), "100".toBigDecimalOrNull())
    }

    @Test
    fun toBigDecimalOrNull_rejectsIncompleteOrInvalidInput() {
        assertNull("".toBigDecimalOrNull())
        assertNull("-".toBigDecimalOrNull())
        assertNull("1.".toBigDecimalOrNull())
        assertNull("1.2.3".toBigDecimalOrNull())
        assertNull("1e10".toBigDecimalOrNull())
    }

    @Test
    fun sanitizeDecimalInput_stripsNonDigitsAndKeepsOnlyFirstDot() {
        assertEquals("123.45", sanitizeDecimalInput("123.45"))
        assertEquals("123.45", sanitizeDecimalInput("abc123.45xyz"))
        assertEquals("1.2345", sanitizeDecimalInput("1.2.3.45"))
        assertEquals("", sanitizeDecimalInput("-"))
        assertEquals("1", sanitizeDecimalInput("-1"))
    }

    @Test
    fun sanitizeIntegerInput_stripsEverythingButDigits() {
        assertEquals("12345", sanitizeIntegerInput("12345"))
        assertEquals("12345", sanitizeIntegerInput("1a2b3c4d5"))
        assertEquals("100", sanitizeIntegerInput("1.00"))
        assertEquals("", sanitizeIntegerInput("-"))
    }

    @Test
    fun formatAmountFixed_padsWithTrailingZeros() {
        assertEquals("100.00", formatAmountFixed(BigDecimal.fromLong(100), 2))
        assertEquals("100.50", formatAmountFixed(BigDecimal.parseString("100.5"), 2))
    }

    @Test
    fun formatAmountFixed_roundsToRequestedPrecision() {
        assertEquals("0.12", formatAmountFixed(BigDecimal.parseString("0.1234"), 2))
    }

    @Test
    fun formatAmountFixed_dropsDecimalPointWhenZeroDecimals() {
        assertEquals("100", formatAmountFixed(BigDecimal.fromLong(100), 0))
    }
}
