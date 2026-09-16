package xyz.tyiu.satsprice

/**
 * English-only string constants mirroring shared/src/commonMain/moko-resources/base/strings.xml.
 * moko-resources-compose's `stringResource()` pulls in Compose Foundation/UI (for its
 * `LocalComposition`/`Modifier` plumbing) transitively, which would drag Skia-oriented
 * dependencies into a module whose whole point is avoiding them, so this DOM build skips
 * moko-resources rather than fight that dependency and stays English-only as a result.
 */
object Strings {
    const val addCurrency = "Add currency"
    const val bitcoinSectionTitle = "Bitcoin"
    const val btcLabel = "BTC"
    fun btcToCurrency(code: String) = "BTC to $code"
    const val cancel = "Cancel"
    const val clearSearch = "Clear search"
    const val currenciesSectionTitle = "Currencies"
    fun currenciesSelectedCount(count: Int) = "$count selected"
    const val currentCurrencySectionTitle = "Current Currency"
    fun currencyNotPriced(sourceName: String) = "Not priced by $sourceName"
    fun currencyOptions(code: String) = "Options for $code"
    fun currencyOptionLabel(code: String, displayName: String) = "$code - $displayName"
    fun currencyToSats(code: String) = "$code to Sats"
    const val done = "Done"
    fun exceedsMaxSupply(maxSupplyText: String) = "Exceeds the $maxSupplyText maximum supply"
    const val loadingRatesStatus = "Loading rates…"
    const val moveDown = "Move down"
    const val moveToBottom = "Move to bottom"
    const val moveToTop = "Move to top"
    const val moveUp = "Move up"
    const val priceSource = "Price Source"
    const val pricedCurrenciesSectionTitle = "Priced Currencies"
    const val refresh = "Refresh"
    const val remove = "Remove"
    const val resetButton = "Reset"
    fun resetConfirmationMessage(code: String) =
        "This keeps $code, but removes every other currency you've added."
    const val resetConfirmationTitle = "Remove all selected currencies?"
    const val retry = "Retry"
    const val satsLabel = "Sats"
    const val searchCurrenciesPlaceholder = "Search currencies"
    const val selectedCurrenciesSectionTitle = "Selected Currencies"
    const val unpricedCurrenciesSectionTitle = "Unpriced Currencies"
    fun updatedStatus(time: String) = "Updated $time"
}
