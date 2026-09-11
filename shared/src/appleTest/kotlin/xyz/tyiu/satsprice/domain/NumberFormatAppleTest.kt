package xyz.tyiu.satsprice.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression coverage for [localizedGroupedInteger] not routing through `NSDecimalNumber`, whose
 * ~38-significant-digit precision ceiling used to silently replace a longer digit string's excess
 * trailing digits with zeros. The app doesn't cap manually typed Sats amounts at the real max BTC
 * supply, so a determined user can reach this. No locale override hook exists here (unlike the JVM
 * `actual`, which can force `Locale.setDefault`), so this only asserts what holds regardless of the
 * test runner's locale: grouping a long digit string round-trips back to the exact original digits.
 */
class NumberFormatAppleTest {

    @Test
    fun groupDigits_preservesEveryDigitOfAVeryLongInteger() {
        val digits = "1".repeat(60)

        val grouped = groupDigits(digits)
        val ungrouped = grouped.filter { it.isDigit() }

        assertEquals(digits, ungrouped)
    }

    @Test
    fun groupDigits_preservesEveryDigitPastFiftyDigitsWithVariedDigits() {
        val digits = (1..60).joinToString("") { (it % 10).toString() }

        val grouped = groupDigits(digits)
        val ungrouped = grouped.filter { it.isDigit() }

        assertEquals(digits, ungrouped)
    }
}
