package xyz.tyiu.satsprice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import xyz.tyiu.satsprice.ui.ConverterUiState
import xyz.tyiu.satsprice.ui.currentCurrency
import xyz.tyiu.satsprice.ui.isPriced
import xyz.tyiu.satsprice.ui.selectedOtherCurrencies
import xyz.tyiu.satsprice.ui.unselectedCurrencies

@Composable
internal fun HtmlCurrencyPicker(
    state: ConverterUiState,
    onToggle: (String) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        Div({ classes("modal-overlay") }) {
            Div({ classes("modal") }) {
                H2 { Text(Strings.resetConfirmationTitle) }
                Div { Text(Strings.resetConfirmationMessage(state.defaultCurrencyCode)) }
                Div({ classes("row", "modal-actions") }) {
                    Button(attrs = { onClick { showResetConfirmation = false } }) { Text(Strings.cancel) }
                    Button(attrs = {
                        classes("destructive-button")
                        onClick {
                            showResetConfirmation = false
                            onReset()
                        }
                    }) { Text(Strings.resetButton) }
                }
            }
        }
    }

    Div({ classes("screen") }) {
        Div({ classes("row", "section-title-row") }) {
            H2 { Text(Strings.currenciesSectionTitle) }
            Div({ classes("row") }) {
                if (state.selectedFiatCurrencies.size > 1) {
                    Button(attrs = { onClick { showResetConfirmation = true } }) { Text(Strings.resetButton) }
                }
                Button(attrs = { onClick { onDone() } }) { Text(Strings.done) }
            }
        }

        var searchQuery by remember { mutableStateOf("") }
        Input(type = InputType.Text, attrs = {
            classes("search-input")
            attr("placeholder", Strings.searchCurrenciesPlaceholder)
            value(searchQuery)
            onInput { event -> searchQuery = event.value }
        })

        val currentCurrency = state.currentCurrency()
        val selectedOthers = state.selectedOtherCurrencies().filter { matchesCurrencySearch(it, searchQuery) }
        val unselected = state.unselectedCurrencies().filter { matchesCurrencySearch(it, searchQuery) }
        val (unselectedPriced, unselectedUnpriced) = unselected.partition { state.isPriced(it.code) }

        if (matchesCurrencySearch(currentCurrency, searchQuery)) {
            CurrencySection(Strings.currentCurrencySectionTitle) {
                CurrencyRow(currentCurrency, isSelected = true, isPriced = state.isPriced(currentCurrency.code), state, onClick = null)
            }
        }
        if (selectedOthers.isNotEmpty()) {
            CurrencySection(Strings.selectedCurrenciesSectionTitle) {
                selectedOthers.forEach { info ->
                    CurrencyRow(info, isSelected = true, isPriced = state.isPriced(info.code), state) { onToggle(info.code) }
                }
            }
        }
        if (unselectedPriced.isNotEmpty()) {
            CurrencySection(Strings.pricedCurrenciesSectionTitle) {
                unselectedPriced.forEach { info ->
                    CurrencyRow(info, isSelected = false, isPriced = true, state) { onToggle(info.code) }
                }
            }
        }
        if (unselectedUnpriced.isNotEmpty()) {
            CurrencySection(Strings.unpricedCurrenciesSectionTitle) {
                unselectedUnpriced.forEach { info ->
                    CurrencyRow(info, isSelected = false, isPriced = false, state) { onToggle(info.code) }
                }
            }
        }
    }
}

@Composable
private fun CurrencySection(title: String, content: @Composable () -> Unit) {
    Div({ classes("currency-section") }) {
        Div({ classes("section-header") }) { Text(title.uppercase()) }
        content()
    }
}

@Composable
private fun CurrencyRow(
    info: CurrencyInfo,
    isSelected: Boolean,
    isPriced: Boolean,
    state: ConverterUiState,
    onClick: (() -> Unit)?,
) {
    val text = Strings.currencyOptionLabel(info.code, info.displayName)
    val label = currencyFlagEmoji(info.code, supportsFlagEmoji = true)?.let { flag -> "$flag $text" } ?: text
    Div({
        classes("row", "picker-row")
        if (onClick != null) {
            classes("clickable")
            onClick { onClick() }
        }
    }) {
        Div {
            Div { Text(label) }
            if (!isPriced) {
                Div({ classes("supporting-text", "error-text") }) {
                    Text(Strings.currencyNotPriced(state.sourceName))
                }
            }
        }
        if (isSelected) Span({ classes("check-mark") }) { Text("✓") }
    }
}
