package xyz.tyiu.satsprice

data class CurrencyInfo(val code: String, val displayName: String)

/**
 * Whether [info]'s code, display name, or the localized name of any country that issues it (e.g.
 * "Japan" for JPY) contains [query], case-insensitively. A plain function (rather than a
 * `CurrencyInfo` extension) so Kotlin/Native exports a predictable, positionally clear Swift
 * signature, matching [currencyFlagEmoji]'s style.
 */
fun matchesCurrencySearch(info: CurrencyInfo, query: String): Boolean {
    if (query.isBlank()) return true
    if (info.code.contains(query, ignoreCase = true)) return true
    if (info.displayName.contains(query, ignoreCase = true)) return true
    return issuingCountryCodes(info.code).any { regionCode ->
        cachedRegionDisplayName(regionCode)?.contains(query, ignoreCase = true) == true
    }
}

// Region codes are static for the life of the app (they don't depend on [query]), but
// [matchesCurrencySearch] re-derives a name for every currency on every keystroke — caching here
// turns that back into a one-time-per-region-code cost. A plain Map (not getOrPut) since a cached
// null result — a region [regionDisplayName] doesn't recognize — must stay cached rather than
// being retried every time, which getOrPut would do for a null value.
private val regionDisplayNameCache = mutableMapOf<String, String?>()

private fun cachedRegionDisplayName(regionCode: String): String? {
    if (regionCode in regionDisplayNameCache) return regionDisplayNameCache[regionCode]
    val name = regionDisplayName(regionCode)
    regionDisplayNameCache[regionCode] = name
    return name
}

/**
 * The current platform's localized display name for ISO 3166-1 alpha-2 [regionCode], if known.
 * Also handles "EU" — [issuingCountryCodes]' own stand-in for EUR's region/flag, and not a real
 * ISO 3166-1 code — since every platform's locale data resolves it correctly regardless (e.g.
 * "European Union" in en, "Union européenne" in fr): CLDR, which all of their locale data is
 * ultimately sourced from, defines "EU" as a grouping in its own right.
 */
expect fun regionDisplayName(regionCode: String): String?

/**
 * Pre-populates [regionDisplayNameCache] for every region code [currencies] issue from, so the
 * first character typed into currency search doesn't pay for all of them at once. Intended to be
 * called once, asynchronously, before the user has a chance to reach the search field — e.g. from
 * a coroutine launched at startup, not blocking that launch's other work. Not thread-safe:
 * [regionDisplayNameCache] is a plain, unsynchronized map, so this (like [matchesCurrencySearch])
 * must only ever run on the single thread/dispatcher UI state changes are made from.
 */
fun warmRegionDisplayNameCache(currencies: List<CurrencyInfo>) {
    currencies.asSequence()
        .flatMap { issuingCountryCodes(it.code).asSequence() }
        .distinct()
        .forEach { regionCode -> cachedRegionDisplayName(regionCode) }
}

/**
 * ISO 4217 codes for the precious metals actively traded today. These aren't tied to any
 * country, so a currently-used-currency filter derived from country/locale data (as the
 * Android/JVM/Apple [systemCurrencies] implementations do, to drop long-withdrawn currencies
 * like the Deutsche Mark) would otherwise exclude them too.
 */
val PRECIOUS_METAL_CURRENCY_CODES: Set<String> = setOf("XAU", "XAG", "XPD", "XPT")

/** All ISO 4217 currencies the current platform knows about, with localized display names. */
expect fun systemCurrencies(): List<CurrencyInfo>

/** The currency associated with the user's current locale, if the platform can determine one. */
expect fun localeCurrencyCode(): String?

/** The conventional number of decimal places for [code] (e.g. 2 for USD, 0 for JPY, 3 for BHD). */
expect fun currencyDecimalDigits(code: String): Int

/**
 * Whether the current platform's text renderer can draw flag emoji. False on Compose for Web
 * (js/wasmJs): its canvas-based Skia renderer has no bundled or system color-emoji font to draw a
 * flag's regional-indicator codepoints with, so they'd otherwise render as empty boxes.
 */
expect fun supportsFlagEmoji(): Boolean
