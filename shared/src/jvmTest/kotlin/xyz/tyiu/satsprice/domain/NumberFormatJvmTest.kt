package xyz.tyiu.satsprice.domain

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** [groupDigits]'s separators follow [Locale.getDefault], not a hardcoded "," and ".". */
class NumberFormatJvmTest {

    private val originalLocale: Locale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun groupDigits_usesCommaGroupingAndPeriodDecimalInUsEnglish() {
        Locale.setDefault(Locale.US)

        assertEquals("1,000,000", groupDigits("1000000"))
        assertEquals("1,000,000.5", groupDigits("1000000.5"))
        assertEquals("-1,234", groupDigits("-1234"))
    }

    @Test
    fun groupDigits_usesPeriodGroupingAndCommaDecimalInGerman() {
        Locale.setDefault(Locale.GERMANY)

        assertEquals("1.000.000", groupDigits("1000000"))
        assertEquals("1.000.000,5", groupDigits("1000000.5"))
        assertEquals("-1.234", groupDigits("-1234"))
    }

    @Test
    fun sanitizeDecimalInput_acceptsTheLocaleDecimalSeparatorInGerman() {
        Locale.setDefault(Locale.GERMANY)

        // A locale-aware decimal keypad's key sends ",", not ".", in German.
        assertEquals("123.45", sanitizeDecimalInput("123,45"))
    }

    @Test
    fun sanitizeDecimalInput_stillAcceptsPeriodEvenInGerman() {
        Locale.setDefault(Locale.GERMANY)

        assertEquals("123.45", sanitizeDecimalInput("123.45"))
    }
}
