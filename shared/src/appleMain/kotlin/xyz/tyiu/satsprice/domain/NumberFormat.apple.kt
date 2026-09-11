package xyz.tyiu.satsprice.domain

import platform.Foundation.*

/**
 * `NSDecimalNumber` has a ~38-significant-digit precision ceiling: a longer digit string gets
 * silently rounded, with the excess trailing digits replaced by zeros — invisible on a single
 * pass, but since every field re-groups on each edit (see `NumericField` in ContentView.swift),
 * a Sats amount typed past that length would re-corrupt through this on every keystroke, visibly
 * changing without the user typing anything new. Real Sats amounts never approach 38 digits (the
 * max BTC supply is only 16 digits in Sats), but the app doesn't cap manual input at that
 * maximum, so grouping via `NSNumberFormatter.stringFromNumber(_:)` isn't safe for arbitrary
 * input. Groups the digit string directly instead, using only the locale's grouping metadata
 * (separator + primary/secondary group sizes) — pure string manipulation, no numeric precision
 * limit, and verified to match `NSNumberFormatter`'s own output exactly, including irregular
 * groupings like hi-IN's "12,34,567".
 */
actual fun localizedGroupedInteger(digits: String): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = NSLocale.currentLocale
        usesGroupingSeparator = true
    }
    val separator = formatter.groupingSeparator
    val primary = formatter.groupingSize.toInt()
    if (primary <= 0 || digits.length <= primary) return digits
    val secondary = formatter.secondaryGroupingSize.toInt().let { if (it > 0) it else primary }

    val groups = mutableListOf<String>()
    var end = digits.length - primary
    groups.add(digits.substring(end))
    while (end > secondary) {
        val start = end - secondary
        groups.add(digits.substring(start, end))
        end = start
    }
    groups.add(digits.substring(0, end))

    return groups.asReversed().joinToString(separator)
}

actual fun localizedDecimalSeparator(): String = NSLocale.currentLocale.decimalSeparator
