package xyz.tyiu.satsprice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import xyz.tyiu.satsprice.domain.CurrencyConverter
import xyz.tyiu.satsprice.domain.formatAmount
import xyz.tyiu.satsprice.domain.groupDigits
import xyz.tyiu.satsprice.ui.PriceViewModel
import xyz.tyiu.satsprice.ui.defaultCurrencyRate
import xyz.tyiu.satsprice.ui.exceedsMaxSupply
import xyz.tyiu.satsprice.ui.isPriced
import xyz.tyiu.satsprice.ui.lastUpdatedDateTime
import xyz.tyiu.satsprice.ui.oneCurrencyToSats

@Composable
fun HtmlApp() {
    val viewModel = remember { PriceViewModel() }
    val state by viewModel.uiState.collectAsState()
    var showCurrencyPicker by remember { mutableStateOf(false) }

    if (showCurrencyPicker) {
        HtmlCurrencyPicker(
            state = state,
            onToggle = viewModel::onFiatCurrencyToggled,
            onReset = viewModel::onSelectedCurrenciesReset,
            onDone = { showCurrencyPicker = false },
        )
        return
    }

    Div({ classes("screen") }) {
        H1({ classes("app-title") }) { Text("SatsPrice") }

        // Price source
        Div({ classes("card") }) {
            Div({ classes("row") }) {
                Span({ classes("field-inline-label") }) { Text(Strings.priceSource) }
                Select(attrs = {
                    onChange { event -> viewModel.onSourceSelected(event.target.value) }
                }) {
                    viewModel.availableSources.forEach { name ->
                        Option(value = name, attrs = { if (name == state.sourceName) attr("selected", "") }) { Text(name) }
                    }
                }
            }

            Div({ classes("row") }) {
                LabeledInput(
                    label = Strings.btcToCurrency(state.defaultCurrencyCode),
                    value = if (state.isManualSource) state.manualRateInput else state.defaultCurrencyRate(),
                    readOnly = !state.isManualSource,
                    onValueChange = viewModel::onManualRateChanged,
                    modifierClass = "grow",
                )
                if (!state.isManualSource) {
                    if (state.isLoading) {
                        Span({ classes("spinner") })
                    } else {
                        Button(attrs = {
                            classes("icon-button")
                            attr("aria-label", Strings.refresh)
                            attr("title", Strings.refresh)
                            onClick { viewModel.refresh() }
                        }) { Text("↻") }
                    }
                }
            }

            if (state.isManualSource) {
                LabeledInput(
                    label = Strings.currencyToSats(state.defaultCurrencyCode),
                    value = state.manualSatsPerCurrencyInput,
                    onValueChange = viewModel::onManualSatsPerCurrencyChanged,
                )
            } else {
                LabeledInput(
                    label = Strings.currencyToSats(state.defaultCurrencyCode),
                    value = state.oneCurrencyToSats(),
                    readOnly = true,
                    onValueChange = {},
                )
            }

            val statusLine = if (state.isManualSource) {
                null
            } else {
                state.lastUpdatedDateTime()?.let { Strings.updatedStatus(it) } ?: Strings.loadingRatesStatus
            }
            statusLine?.let { Div({ classes("status") }) { Text(it) } }
        }

        state.errorMessage?.let { message ->
            Div({ classes("card", "error-banner") }) {
                Span { Text(message) }
                Button(attrs = { onClick { viewModel.refresh() } }) { Text(Strings.retry) }
            }
        }

        val exceedsMaxSupply = state.exceedsMaxSupply()

        // Bitcoin
        Div({ classes("card") }) {
            Div({ classes("section-title") }) { Text(Strings.bitcoinSectionTitle) }
            LabeledInput(
                label = Strings.satsLabel,
                value = state.satsAmount,
                isError = exceedsMaxSupply,
                onValueChange = viewModel::onSatsAmountChanged,
            )
            LabeledInput(
                label = Strings.btcLabel,
                value = state.btcAmount,
                isError = exceedsMaxSupply,
                onValueChange = viewModel::onBtcAmountChanged,
            )
            if (exceedsMaxSupply) {
                val maxSupplyText = groupDigits(formatAmount(CurrencyConverter.MAX_BTC_SUPPLY, 0)) + " BTC"
                Div({ classes("warning") }) { Text(Strings.exceedsMaxSupply(maxSupplyText)) }
                Button(attrs = {
                    classes("link-button")
                    onClick { viewModel.onBtcAmountChanged(formatAmount(CurrencyConverter.MAX_BTC_SUPPLY, 0)) }
                }) { Text("Set BTC to $maxSupplyText") }
            }
        }

        // Currencies
        Div({ classes("card") }) {
            Div({ classes("row", "section-title-row") }) {
                Span({ classes("section-title") }) { Text(Strings.currenciesSectionTitle) }
                if (!state.isManualSource) {
                    Button(attrs = { classes("outlined-button"); onClick { showCurrencyPicker = true } }) {
                        Text(
                            if (state.selectedFiatCurrencies.size <= 1) {
                                Strings.addCurrency
                            } else {
                                Strings.currenciesSelectedCount(state.selectedFiatCurrencies.size)
                            },
                        )
                    }
                }
            }

            val displayedCurrencies = if (state.isManualSource) {
                listOf(state.defaultCurrencyCode)
            } else {
                state.selectedFiatCurrencies
            }
            displayedCurrencies.forEachIndexed { index, code ->
                val isPriced = state.isPriced(code)
                val fieldLabel = currencyFlagEmoji(code, supportsFlagEmoji = true)?.let { flag -> "$flag $code" } ?: code
                Div({ classes("row", "currency-row") }) {
                    LabeledInput(
                        label = fieldLabel,
                        value = if (isPriced) state.fiatAmounts[code].orEmpty() else "",
                        enabled = isPriced,
                        isError = !isPriced && !state.isManualSource,
                        supportingText = if (!isPriced && !state.isManualSource) {
                            Strings.currencyNotPriced(state.sourceName)
                        } else {
                            null
                        },
                        onValueChange = { viewModel.onFiatAmountChanged(code, it) },
                        modifierClass = "grow",
                    )
                    if (displayedCurrencies.size > 1) {
                        val selection = state.selectedFiatCurrencies
                        CurrencyRowMenu(
                            code = code,
                            canMoveUp = index > 0,
                            canMoveDown = index < selection.lastIndex,
                            canRemove = code != state.defaultCurrencyCode,
                            onMoveUp = { viewModel.onFiatCurrenciesReordered(selection.moved(index, index - 1)) },
                            onMoveDown = { viewModel.onFiatCurrenciesReordered(selection.moved(index, index + 1)) },
                            onMoveToTop = { viewModel.onFiatCurrenciesReordered(selection.moved(index, 0)) },
                            onMoveToBottom = {
                                viewModel.onFiatCurrenciesReordered(selection.moved(index, selection.lastIndex))
                            },
                            onRemove = { viewModel.onFiatCurrencyToggled(code) },
                        )
                    }
                }
            }
        }
    }
}

internal fun List<String>.moved(fromIndex: Int, toIndex: Int): List<String> =
    toMutableList().apply { add(toIndex, removeAt(fromIndex)) }

/**
 * An anchored popup menu for reordering/removing a currency row, matching Material3's
 * DropdownMenu behavior. Compose HTML has no built-in menu primitive, so this is plain DOM: a
 * relatively-positioned trigger, an absolutely-positioned popup anchored under it (CSS handles
 * the anchoring - no JS position math needed), and a full-viewport backdrop that closes the menu
 * on any outside click, matching how the Material3 version dismisses.
 */
@Composable
private fun CurrencyRowMenu(
    code: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Div({ classes("menu-anchor") }) {
        Button(attrs = {
            classes("icon-button")
            attr("aria-label", Strings.currencyOptions(code))
            attr("aria-haspopup", "true")
            attr("aria-expanded", if (expanded) "true" else "false")
            onClick { expanded = true }
        }) { Text("⋮") }

        if (expanded) {
            Div({
                classes("menu-backdrop")
                onClick { expanded = false }
            })
            Div({ classes("menu-popup") }) {
                MenuItem(Strings.moveToTop, enabled = canMoveUp) { expanded = false; onMoveToTop() }
                MenuItem(Strings.moveUp, enabled = canMoveUp) { expanded = false; onMoveUp() }
                MenuItem(Strings.moveDown, enabled = canMoveDown) { expanded = false; onMoveDown() }
                MenuItem(Strings.moveToBottom, enabled = canMoveDown) { expanded = false; onMoveToBottom() }
                if (canRemove) {
                    MenuItem(Strings.remove, destructive = true) { expanded = false; onRemove() }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, enabled: Boolean = true, destructive: Boolean = false, onClick: () -> Unit) {
    Button(attrs = {
        classes("menu-item")
        if (destructive) classes("menu-item-destructive")
        if (!enabled) attr("disabled", "")
        onClick { onClick() }
    }) { Text(label) }
}

@Composable
internal fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    modifierClass: String? = null,
) {
    Div({
        classes("field")
        modifierClass?.let { classes(it) }
        if (isError) classes("field-error")
    }) {
        Span({ classes("field-label") }) { Text(label) }
        Input(type = InputType.Text, attrs = {
            value(value)
            if (readOnly) attr("readonly", "")
            if (!enabled) attr("disabled", "")
            onInput { event -> onValueChange(event.value) }
        })
        supportingText?.let { Div({ classes("supporting-text") }) { Text(it) } }
    }
}
