package xyz.tyiu.satsprice.domain

import platform.Foundation.*

actual fun localizedGroupedInteger(digits: String): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = NSLocale.currentLocale
        usesGroupingSeparator = true
    }
    val number = NSDecimalNumber(string = digits)
    return formatter.stringFromNumber(number) ?: digits
}

actual fun localizedDecimalSeparator(): String = NSLocale.currentLocale.decimalSeparator
