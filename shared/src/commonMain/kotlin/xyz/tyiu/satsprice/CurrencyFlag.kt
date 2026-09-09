package xyz.tyiu.satsprice

/**
 * ISO 3166-1 alpha-2 codes of every country that issues each ISO 4217 currency code shared by
 * multiple countries with no single issuer, so all of their flags can be shown side by side.
 */
private val MULTI_COUNTRY_CURRENCY_REGION_CODES: Map<String, List<String>> = mapOf(
    "XAF" to listOf("CF", "CG", "CM", "GA", "GQ", "TD"),
    "XCD" to listOf("AG", "AI", "DM", "GD", "KN", "LC", "MS", "VC"),
    "XCG" to listOf("CW", "SX"),
    "XOF" to listOf("BF", "BJ", "CI", "GW", "ML", "NE", "SN", "TG"),
    "XPF" to listOf("NC", "PF", "WF"),
)

/**
 * A country flag emoji for [code], derived from the first two letters of the ISO 4217 code —
 * which double as the issuing country's ISO 3166-1 alpha-2 code for ordinary national
 * currencies — or null when there's no flag to show. [MULTI_COUNTRY_CURRENCY_REGION_CODES]
 * lists every country sharing a currency with no single issuer, so those show all their flags
 * side by side; the remaining "X"-prefixed codes are precious metals, testing codes, and other
 * non-national codes (XAU, XTS, XXX, ...), none of which has a country to show. EUR is a special
 * case handled separately, using the EU's own flag.
 */
fun currencyFlagEmoji(code: String): String? {
    MULTI_COUNTRY_CURRENCY_REGION_CODES[code]?.let { regionCodes ->
        return regionCodes.joinToString(" ") { regionFlagEmoji(it) }
    }
    if (code.startsWith("X")) return null
    val regionCode = if (code == "EUR") "EU" else code.take(2)
    if (regionCode.length != 2 || regionCode.any { it !in 'A'..'Z' }) return null
    return regionFlagEmoji(regionCode)
}

/** The flag emoji for the 2-letter ISO 3166-1 alpha-2 [regionCode]. */
private fun regionFlagEmoji(regionCode: String): String =
    regionCode.map { regionalIndicatorSymbol(it) }.joinToString("")

/** The Unicode Regional Indicator Symbol for [letter] ('A'..'Z'), as a surrogate pair. */
private fun regionalIndicatorSymbol(letter: Char): String {
    val codePoint = 0x1F1E6 + (letter - 'A')
    val offset = codePoint - 0x10000
    val high = 0xD800 + (offset shr 10)
    val low = 0xDC00 + (offset and 0x3FF)
    return "${high.toChar()}${low.toChar()}"
}
