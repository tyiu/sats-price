package xyz.tyiu.satsprice.domain

import java.math.BigInteger
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

actual fun localizedGroupedInteger(digits: String): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(BigInteger(digits))

actual fun localizedDecimalSeparator(): String =
    DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator.toString()
