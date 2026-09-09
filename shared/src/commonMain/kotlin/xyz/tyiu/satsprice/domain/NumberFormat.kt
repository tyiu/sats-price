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

/**
 * Strips a raw text-field edit down to digits and at most one decimal point. Recognizes either
 * "." or the current locale's own decimal separator (e.g. a locale-aware decimal keypad's key,
 * which shows "," in de-DE) as that decimal point — either way, the output always uses ".", the
 * canonical form [toBigDecimalOrNull] and the rest of this file expect.
 */
fun sanitizeDecimalInput(raw: String): String {
    val localizedSeparator = localizedDecimalSeparator()
    val sb = StringBuilder()
    var seenDot = false
    for (c in raw) {
        when {
            c.isDigit() -> sb.append(c)
            !seenDot && (c == '.' || c.toString() == localizedSeparator) -> {
                sb.append('.')
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

/**
 * Inserts locale-appropriate digit-grouping separators into [value]'s integer part (never the
 * fractional part), e.g. "1000000.5" -> "1,000,000.5" in en-US, or "1.000.000,5" in de-DE where
 * the roles of "," and "." are swapped — and not necessarily every 3 digits, since some locales
 * group differently (e.g. hi-IN's "12,34,567"). Display-only: [value] itself stays plain-digit
 * and '.'-decimal, parseable by [toBigDecimalOrNull], so this is applied on top of already-
 * formatted/stored amounts rather than baked into them — a partial value being typed (like "1."
 * or a bare "-") passes through with its structure intact.
 */
fun groupDigits(value: String): String {
    val negative = value.startsWith("-")
    val unsigned = if (negative) value.substring(1) else value
    val dotIndex = unsigned.indexOf('.')
    val integerPart = if (dotIndex >= 0) unsigned.substring(0, dotIndex) else unsigned
    val fractionPart = if (dotIndex >= 0) unsigned.substring(dotIndex + 1) else null
    if (integerPart.isEmpty()) return value

    return buildString {
        if (negative) append('-')
        append(localizedGroupedInteger(integerPart))
        if (fractionPart != null) {
            append(localizedDecimalSeparator())
            append(fractionPart)
        }
    }
}

/** Formats a plain non-negative integer digit string with the current locale's grouping. */
expect fun localizedGroupedInteger(digits: String): String

/** The current locale's decimal-point character, e.g. "." in en-US or "," in de-DE. */
expect fun localizedDecimalSeparator(): String
