@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package xyz.tyiu.satsprice

private fun jsSupportedCurrencyCodes(): JsArray<JsString> = js("Intl.supportedValuesOf('currency')")

// A chained `new Foo(x).bar()` inside js() can misparse (`new` binding to the whole chain rather
// than just the constructor call), so the constructor result is bound to a variable first.
// `undefined` (rather than a fixed locale like 'en') uses the browser's own locale — code always
// comes from jsSupportedCurrencyCodes() itself, so .of(code) is guaranteed to resolve.
private fun jsCurrencyDisplayName(code: String): String = js(
    "(function() { var names = new Intl.DisplayNames(undefined, { type: 'currency' }); return names.of(code); })()",
)

/**
 * `Intl.supportedValuesOf('currency')` includes a lot of codes for currencies that have actually
 * been withdrawn/superseded since — which ones depends on the browser's ICU data, so this list is
 * necessarily incomplete and grows over time as more turn up. Unlike JVM/Android/Apple, there's
 * no web API to derive "currently assigned to some country" the way `java.util.Currency`/
 * `NSLocale` do (see [localeCurrencyCode] below), so this is a manually maintained exclusion list
 * instead, cross-checked against
 * [Wikipedia's historical ISO 4217 codes table](https://en.wikipedia.org/wiki/ISO_4217#Historical_codes)
 * (2026-09-10). Needs a new entry whenever another currency is retired, or another old one turns
 * up. Deliberately excludes "BGN" despite Wikipedia listing a fourth, withdrawn Bulgarian lev
 * (→ EUR): the JVM's own live-derived "currently used" set (the reference this list is checked
 * against) still resolves BGN as Bulgaria's current currency, and matching that beats matching
 * Wikipedia if the two disagree — this file's whole point is currency data this app can't get
 * from a web API of its own.
 */
private val WITHDRAWN_CURRENCY_CODES = setOf(
    "ANG", // Netherlands Antillean Guilder — replaced by XCG (Caribbean Guilder), April 2025
    "CUC", // Cuban Convertible Peso — unified into CUP, January 2021
    "HRK", // Croatian Kuna — replaced by EUR, January 2023
    "SLL", // Sierra Leonean Leone (old) — redenominated to SLE, 2022
    "ZWL", // Zimbabwean Dollar (old) — replaced by ZWG (Zimbabwe Gold), April 2024

    // Old Angolan Kwanza, through several redenominations — replaced by AOA
    "AOK", "AON", "AOR",
    "AFA", // Afghan Afghani (old) — replaced by AFN
    "ALK", // Albanian Lek (old) — replaced by ALL

    // Pre-euro legacy currencies, withdrawn on adoption of the euro
    "ADP", // Andorran Peseta
    "ATS", // Austrian Schilling
    "BEF", // Belgian Franc
    "CYP", // Cypriot Pound
    "DEM", // Deutsche Mark
    "EEK", // Estonian Kroon
    "ESP", // Spanish Peseta
    "FIM", // Finnish Markka
    "FRF", // French Franc
    "GRD", // Greek Drachma
    "IEP", // Irish Pound
    "ITL", // Italian Lira
    "LTL", // Lithuanian Litas
    "LUF", // Luxembourg Franc
    "LVL", // Latvian Lats
    "MTL", // Maltese Lira
    "NLG", // Dutch Guilder
    "PTE", // Portuguese Escudo
    "SIT", // Slovenian Tolar
    "SKK", // Slovak Koruna

    // The rest of Wikipedia's historical codes table not already covered above.
    "ARA", "ARP", "ARY", // Argentine austral/peso argentino/peso ley — replaced by ARS
    "AYM", "AZM", // Azerbaijani manat (old) — replaced by AZN
    "BAD", // Bosnia and Herzegovina dinar — replaced by BAM
    "BEC", "BEL", // Belgian convertible/financial franc (funds codes)
    "BGJ", "BGK", "BGL", // Bulgarian lev (first/second/third) — replaced by BGN
    "BOP", // Bolivian peso — replaced by BOB
    "BRB", "BRC", "BRE", "BRN", "BRR", // Brazilian cruzeiro/cruzado, through several redenominations — replaced by BRL
    "BUK", // Burmese kyat — replaced by MMK
    "BYB", // Belarusian ruble (first) — replaced by BYR, itself replaced by BYN
    "CHC", // WIR franc (electronic)
    "CSD", // Serbian dinar (old) — replaced by RSD
    "CSJ", "CSK", // Czechoslovak koruna — split into CZK/SKK
    "DDM", // East German mark — replaced by DEM
    "ECS", "ECV", // Ecuadorian sucre / Unit of Constant Value — replaced by USD
    "ESA", "ESB", // Spanish peseta (account A/B)
    "GEK", // Georgian kuponi — replaced by GEL
    "GHC", "GHP", // Ghanaian cedi (old) — replaced by GHS
    "GNE", "GNS", // Guinean syli — replaced by GNF
    "GQE", // Equatorial Guinean ekwele — replaced by XAF
    "GWE", "GWP", // Guinean/Guinea-Bissau escudo/peso — replaced by XOF
    "HRD", // Croatian dinar — replaced by HRK, itself replaced by EUR
    "ILP", "ILR", // Israeli pound/shekel (old) — replaced by ILS
    "ISJ", // Icelandic króna (first) — replaced by ISK
    "LAJ", // Lao kip (old) — replaced by LAK
    "LSM", // Lesotho loti (old)
    "LTT", // Lithuanian talonas — replaced by LTL, itself replaced by EUR
    "LUC", "LUL", // Luxembourg convertible/financial franc
    "LVR", // Latvian rublis — replaced by LVL, itself replaced by EUR
    "MGF", // Malagasy franc — replaced by MGA
    "MLF", // Malian franc — replaced by XOF
    "MRO", // Mauritanian ouguiya (old) — replaced by MRU
    "MTP", // Maltese pound — replaced by MTL, itself replaced by EUR
    "MVQ", // Maldivian rupee (old) — replaced by MVR
    "MXP", // Mexican peso (old) — replaced by MXN
    "MZE", "MZM", // Mozambican escudo/metical (old) — replaced by MZN
    "NIC", // Nicaraguan córdoba (old) — replaced by NIO
    "PEH", "PEI", "PES", // Peruvian sol/inti, through several redenominations — replaced by PEN
    "PLZ", // Polish zloty (old) — replaced by PLN
    "RHD", // Rhodesian dollar
    "ROK", "ROL", // Romanian leu (old) — replaced by RON
    "RUR", // Russian ruble (old) — replaced by RUB
    "SDD", "SDP", // Sudanese dinar/old pound — replaced by SDG
    "SRG", // Surinamese guilder — replaced by SRD
    "STD", // São Tomé and Príncipe dobra (old) — replaced by STN
    "SUR", // Soviet Union ruble
    "TJR", // Tajikistani ruble — replaced by TJS
    "TMM", // Turkmenistani manat (old) — replaced by TMT
    "TPE", // Portuguese Timorese escudo — replaced by USD
    "TRL", // Turkish lira (old) — replaced by TRY
    "UAK", // Ukrainian karbovanets — replaced by UAH
    "UGS", "UGW", // Ugandan shilling (old)
    "USS", // US dollar (same-day settlement)
    "UYN", "UYP", // Uruguayan peso (old) — replaced by UYU
    "VEB", "VEF", // Venezuelan bolívar (old) — replaced by VES
    "VNC", // Vietnamese đồng (old) — replaced by VND
    "XEU", // European Currency Unit — replaced by EUR
    "XFO", "XFU", // Gold franc/UIC franc (settlement codes)
    "XRE", // RINET funds code
    "YDD", // South Yemeni dinar — replaced by YER
    "YUD", "YUM", "YUN", // Yugoslav dinar, through several redenominations
    "ZAL", // South African financial rand
    "ZMK", // Zambian kwacha (old) — replaced by ZMW
    "ZRN", "ZRZ", // Zairean zaire/new zaire — replaced by CDF
    "ZWC", "ZWD", "ZWN", "ZWR", // Zimbabwean dollar (first through third) — replaced by ZWL, itself replaced by ZWG
)

/**
 * CLDR appends a "(1927–2002)" or "(2009–2024)" style validity-period annotation to a currency's
 * display name when it's a historical/superseded one — a broader, data-driven signal than
 * [WITHDRAWN_CURRENCY_CODES] above, catching ones not yet added there. Not a full replacement for
 * it, though: some withdrawn currencies (pre-euro ones especially) don't get this treatment in
 * every ICU dataset and still need an explicit entry — e.g. the year-annotated "Afghan Afghani
 * (1927–2002)" is how AFA shows up, but "German Mark" carries no such hint that DEM is withdrawn.
 * Dashes vary (an en dash in most of these, an em dash in at least one seen in practice), so the
 * character class covers hyphen-minus and both.
 */
private val YEAR_ANNOTATION_REGEX = Regex("""\(\d{4}([-–—]\d{4})?\)$""")

actual fun systemCurrencies(): List<CurrencyInfo> =
    jsSupportedCurrencyCodes()
        .toList()
        .map { it.toString() }
        .filterNot { it in WITHDRAWN_CURRENCY_CODES }
        .map { code -> CurrencyInfo(code, jsCurrencyDisplayName(code)) }
        .filterNot { YEAR_ANNOTATION_REGEX.containsMatchIn(it.displayName) }
        .sortedBy { it.code }

/**
 * No web API infers a currency from a locale: `Intl.NumberFormat`/`Intl.Locale` require an
 * explicit `currency` option rather than deriving one, and there's no TC39 proposal that adds
 * this. Doing it reliably would mean maintaining our own region-to-currency table, so this is
 * left undetected on web rather than guessing.
 */
actual fun localeCurrencyCode(): String? = null

// A currency's decimal-digit count is a property of the currency, not the locale (e.g. JPY's is
// always 0), so — unlike jsCurrencyDisplayName/jsRegionDisplayName above — locale is only a
// required constructor argument here, not something the result actually depends on; 'en' is as
// good as any other.
private fun jsCurrencyFractionDigits(code: String): Int = js(
    """(function() { var fmt = new Intl.NumberFormat('en', { style: 'currency', currency: code }); return fmt.resolvedOptions().maximumFractionDigits; })()""",
)

actual fun currencyDecimalDigits(code: String): Int = try {
    jsCurrencyFractionDigits(code)
} catch (e: Exception) {
    2
}

// `undefined` (rather than a fixed locale like 'en') uses the browser's own locale, matching the
// other platforms' regionDisplayName(). Falls back to the region code itself (rather than a JS
// null/undefined) when unrecognized, since a plain `-> String?` return type doesn't reliably
// round-trip through js() interop here; that fallback is filtered back out to null actual-side
// below, same as the other platforms.
private fun jsRegionDisplayName(regionCode: String): String = js(
    """(function() {
        try {
            var names = new Intl.DisplayNames(undefined, { type: 'region' });
            return names.of(regionCode) || regionCode;
        } catch (e) {
            return regionCode;
        }
    })()""",
)

actual fun regionDisplayName(regionCode: String): String? =
    jsRegionDisplayName(regionCode).takeIf { it != regionCode }

// Compose for Web renders everything through Skia onto a <canvas> rather than through the
// browser's own text stack, so it can't fall back to the browser/OS's color-emoji font the way
// native targets can — a flag's regional-indicator codepoints draw as empty boxes instead.
actual fun supportsFlagEmoji(): Boolean = false
