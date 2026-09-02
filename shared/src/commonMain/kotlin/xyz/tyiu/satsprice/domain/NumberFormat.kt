package xyz.tyiu.satsprice.domain

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.RoundingMode

private val PLAIN_DECIMAL_INPUT = Regex("""-?\d+(\.\d+)?""")

/** Parses a plain decimal string (as typed into an amount field), or null if incomplete/invalid. */
fun String.toBigDecimalOrNull(): BigDecimal? {
    if (!PLAIN_DECIMAL_INPUT.matches(this)) return null
    return try {
        BigDecimal.parseString(this)
    } catch (e: Exception) {
        null
    }
}

/** Strips a raw text-field edit down to digits and at most one decimal point. */
fun sanitizeDecimalInput(raw: String): String {
    val sb = StringBuilder()
    var seenDot = false
    for (c in raw) {
        when {
            c.isDigit() -> sb.append(c)
            c == '.' && !seenDot -> {
                sb.append(c)
                seenDot = true
            }
        }
    }
    return sb.toString()
}

/** Strips a raw text-field edit down to digits only (for whole-unit fields like Sats). */
fun sanitizeIntegerInput(raw: String): String = raw.filter { it.isDigit() }

/** Formats [value] with up to [maxDecimals] decimal places, trimming trailing zeros. */
fun formatAmount(value: BigDecimal, maxDecimals: Int): String {
    val rounded = value.roundToDigitPositionAfterDecimalPoint(
        digitPosition = maxDecimals.toLong(),
        roundingMode = RoundingMode.ROUND_HALF_AWAY_FROM_ZERO,
    )
    val text = rounded.toStringExpanded()

    if ('.' !in text) return text

    val trimmed = text.trimEnd('0').trimEnd('.')
    return when {
        trimmed.isEmpty() || trimmed == "-" -> "0"
        else -> trimmed
    }
}

/** Formats [value] with exactly [decimals] decimal places, zero-padded rather than trimmed. */
fun formatAmountFixed(value: BigDecimal, decimals: Int): String {
    val rounded = value.roundToDigitPositionAfterDecimalPoint(
        digitPosition = decimals.toLong(),
        roundingMode = RoundingMode.ROUND_HALF_AWAY_FROM_ZERO,
    )
    val text = rounded.toStringExpanded()
    if (decimals <= 0) return text.substringBefore('.')

    val whole = text.substringBefore('.')
    val fraction = text.substringAfter('.', "")
    return "$whole.${fraction.padEnd(decimals, '0')}"
}
