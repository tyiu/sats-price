package xyz.tyiu.satsprice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.icerock.moko.resources.compose.stringResource
import xyz.tyiu.satsprice.CurrencyInfo
import xyz.tyiu.satsprice.currencyFlagEmoji
import xyz.tyiu.satsprice.domain.CurrencyConverter
import xyz.tyiu.satsprice.domain.formatAmount
import xyz.tyiu.satsprice.domain.groupDigits
import xyz.tyiu.satsprice.domain.localizedDecimalSeparator
import xyz.tyiu.satsprice.shared.MR

private val SectionColors
    @Composable get() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

private val SectionHeaderStyle
    @Composable get() = MaterialTheme.typography.labelLarge.copy(
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp,
    )

/** Displays a plain-digit amount field's value with locale-appropriate digit grouping, without touching it. */
private object DigitGroupingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val grouped = groupDigits(text.text)
        val decimalSeparator = localizedDecimalSeparator()
        // Everything grouping inserts is neither a digit, the sign, nor the (possibly localized,
        // e.g. "," in de-DE) decimal point — so anything else is an inserted grouping separator
        // to skip, whatever character the locale actually uses for it.
        fun isGroupingSeparator(char: Char) = !char.isDigit() && char != '-' && char.toString() != decimalSeparator

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var originalSeen = 0
                for ((index, char) in grouped.withIndex()) {
                    if (originalSeen == offset) return index
                    if (!isGroupingSeparator(char)) originalSeen++
                }
                return grouped.length
            }

            override fun transformedToOriginal(offset: Int): Int =
                grouped.take(offset.coerceIn(0, grouped.length)).count { !isGroupingSeparator(it) }
        }
        return TransformedText(AnnotatedString(grouped), offsetMapping)
    }
}

@Composable
fun PriceScreen(
    viewModel: PriceViewModel = viewModel { PriceViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCurrencyPicker by remember { mutableStateOf(false) }

    if (showCurrencyPicker) {
        CurrencyPickerScreen(
            state = state,
            onToggle = viewModel::onFiatCurrencyToggled,
            onDone = { showCurrencyPicker = false },
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = screenHorizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text("SatsPrice", style = MaterialTheme.typography.headlineMedium)
                val statusLine = state.statusLine()
                if (statusLine.isNotEmpty()) {
                    Text(
                        text = statusLine,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = SectionColors) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(MR.strings.price_source), style = MaterialTheme.typography.bodyMedium)
                        DropdownSelector(
                            selectedLabel = state.sourceName,
                            options = viewModel.availableSources,
                            optionLabel = { it },
                            onSelected = viewModel::onSourceSelected,
                        )
                    }

                    Text(
                        text = stringResource(MR.strings.btc_to_currency, state.defaultCurrencyCode),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.isManualSource) {
                            OutlinedTextField(
                                value = state.manualRateInput,
                                onValueChange = viewModel::onManualRateChanged,
                                label = { Text(stringResource(MR.strings.rate_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                visualTransformation = DigitGroupingTransformation,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            val rate = state.defaultCurrencyRate()
                            if (rate.isNotEmpty()) {
                                Text(
                                    text = groupDigits(rate),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        if (!state.isManualSource) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                IconButton(onClick = { viewModel.refresh() }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(MR.strings.refresh_content_description),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let { message ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = { viewModel.refresh() }) { Text(stringResource(MR.strings.retry)) }
                    }
                }
            }

            val exceedsMaxSupply = state.exceedsMaxSupply()

            Card(modifier = Modifier.fillMaxWidth(), colors = SectionColors) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(MR.strings.bitcoin_section_title), style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = state.satsAmount,
                        onValueChange = viewModel::onSatsAmountChanged,
                        label = { Text(stringResource(MR.strings.sats_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = exceedsMaxSupply,
                        visualTransformation = DigitGroupingTransformation,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = state.btcAmount,
                        onValueChange = viewModel::onBtcAmountChanged,
                        label = { Text(stringResource(MR.strings.btc_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = exceedsMaxSupply,
                        visualTransformation = DigitGroupingTransformation,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (exceedsMaxSupply) {
                        // Locale-grouped (e.g. "21,000,000 BTC" in en-US, "21.000.000 BTC" in
                        // de-DE) since it's substituted into the string, then searched for
                        // verbatim below to turn it into a link — the two always match exactly.
                        val maxSupplyText = groupDigits(formatAmount(CurrencyConverter.MAX_BTC_SUPPLY, 0)) + " BTC"
                        val warning = stringResource(MR.strings.exceeds_max_supply, maxSupplyText)
                        val linkStart = warning.indexOf(maxSupplyText)
                        val annotatedWarning = if (linkStart < 0) {
                            AnnotatedString(warning)
                        } else {
                            // Set explicitly (and identically) for every interaction state, since
                            // LinkAnnotation otherwise renders in the theme's link/accent color
                            // rather than inheriting the surrounding warning text's color.
                            val linkStyle = SpanStyle(
                                color = MaterialTheme.colorScheme.error,
                                textDecoration = TextDecoration.Underline,
                            )
                            buildAnnotatedString {
                                append(warning.substring(0, linkStart))
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = "max_supply",
                                        styles = TextLinkStyles(
                                            style = linkStyle,
                                            focusedStyle = linkStyle,
                                            hoveredStyle = linkStyle,
                                            pressedStyle = linkStyle,
                                        ),
                                    ) {
                                        viewModel.onBtcAmountChanged(formatAmount(CurrencyConverter.MAX_BTC_SUPPLY, 0))
                                    },
                                ) {
                                    append(maxSupplyText)
                                }
                                append(warning.substring(linkStart + maxSupplyText.length))
                            }
                        }
                        Text(
                            text = annotatedWarning,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = SectionColors) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(MR.strings.currencies_section_title), style = MaterialTheme.typography.titleMedium)
                        if (!state.isManualSource) {
                            OutlinedButton(onClick = { showCurrencyPicker = true }) {
                                Text(
                                    if (state.selectedFiatCurrencies.size <= 1) {
                                        stringResource(MR.strings.add_currency)
                                    } else {
                                        stringResource(MR.strings.currencies_selected_count, state.selectedFiatCurrencies.size)
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
                        key(code) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val isPriced = state.isPriced(code)
                                val fieldLabel = currencyFlagEmoji(code)?.let { flag -> "$flag $code" } ?: code
                                OutlinedTextField(
                                    value = if (isPriced) state.fiatAmounts[code].orEmpty() else "",
                                    onValueChange = { viewModel.onFiatAmountChanged(code, it) },
                                    label = { Text(fieldLabel) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    enabled = isPriced,
                                    isError = !isPriced && !state.isManualSource,
                                    supportingText = if (isPriced || state.isManualSource) {
                                        null
                                    } else {
                                        { Text(stringResource(MR.strings.currency_not_priced, state.sourceName)) }
                                    },
                                    visualTransformation = DigitGroupingTransformation,
                                    modifier = Modifier.weight(1f),
                                )
                                CurrencyRowMenu(
                                    code = code,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < displayedCurrencies.lastIndex,
                                    canRemove = code != state.defaultCurrencyCode,
                                    onMoveUp = {
                                        viewModel.onFiatCurrenciesReordered(
                                            state.selectedFiatCurrencies.moved(index, index - 1),
                                        )
                                    },
                                    onMoveDown = {
                                        viewModel.onFiatCurrenciesReordered(
                                            state.selectedFiatCurrencies.moved(index, index + 1),
                                        )
                                    },
                                    onMoveToTop = {
                                        viewModel.onFiatCurrenciesReordered(
                                            state.selectedFiatCurrencies.moved(index, 0),
                                        )
                                    },
                                    onMoveToBottom = {
                                        viewModel.onFiatCurrenciesReordered(
                                            state.selectedFiatCurrencies.moved(index, state.selectedFiatCurrencies.lastIndex),
                                        )
                                    },
                                    onRemove = { viewModel.onFiatCurrencyToggled(code) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<String>.moved(fromIndex: Int, toIndex: Int): List<String> =
    toMutableList().apply { add(toIndex, removeAt(fromIndex)) }

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
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(MR.strings.currency_options_content_description, code),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.move_currency_to_top_content_description)) },
                leadingIcon = { Icon(Icons.Default.KeyboardDoubleArrowUp, contentDescription = null) },
                enabled = canMoveUp,
                onClick = {
                    expanded = false
                    onMoveToTop()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.move_currency_up_content_description)) },
                leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                enabled = canMoveUp,
                onClick = {
                    expanded = false
                    onMoveUp()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.move_currency_down_content_description)) },
                leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                enabled = canMoveDown,
                onClick = {
                    expanded = false
                    onMoveDown()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.move_currency_to_bottom_content_description)) },
                leadingIcon = { Icon(Icons.Default.KeyboardDoubleArrowDown, contentDescription = null) },
                enabled = canMoveDown,
                onClick = {
                    expanded = false
                    onMoveToBottom()
                },
            )
            if (canRemove) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.remove_currency_content_description)) },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onRemove()
                    },
                )
            }
        }
    }
}

/**
 * A full-screen takeover (rather than a dropdown) matching the previous Skip-based app's
 * dedicated currency selection screen: a pinned, non-removable "Current Currency", the other
 * currencies the user added (removable), and the remaining ones available to add.
 */
@Composable
private fun CurrencyPickerScreen(
    state: ConverterUiState,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = screenHorizontalPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(MR.strings.currencies_section_title), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDone) { Text(stringResource(MR.strings.done)) }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = screenHorizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val selectedOthers = state.selectedOtherCurrencies()

                Column {
                    Text(
                        stringResource(MR.strings.current_currency_section_title).uppercase(),
                        style = SectionHeaderStyle,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    CurrencyRow(
                        info = state.currentCurrency(),
                        isSelected = true,
                        isPriced = state.isPriced(state.currentCurrency().code),
                        sourceName = state.sourceName,
                        localeCurrencyCode = state.localeCurrencyCode,
                        onClick = null,
                    )
                }

                if (selectedOthers.isNotEmpty()) {
                    Column {
                        Text(
                            stringResource(MR.strings.selected_currencies_section_title).uppercase(),
                            style = SectionHeaderStyle,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        selectedOthers.forEach { info ->
                            key(info.code) {
                                CurrencyRow(
                                    info = info,
                                    isSelected = true,
                                    isPriced = state.isPriced(info.code),
                                    sourceName = state.sourceName,
                                    localeCurrencyCode = state.localeCurrencyCode,
                                    onClick = { onToggle(info.code) },
                                )
                            }
                        }
                    }
                }

                Column {
                    Text(
                        stringResource(MR.strings.currencies_section_title).uppercase(),
                        style = SectionHeaderStyle,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    state.unselectedCurrencies().forEach { info ->
                        key(info.code) {
                            CurrencyRow(
                                info = info,
                                isSelected = false,
                                isPriced = state.isPriced(info.code),
                                sourceName = state.sourceName,
                                localeCurrencyCode = state.localeCurrencyCode,
                                onClick = { onToggle(info.code) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    info: CurrencyInfo,
    isSelected: Boolean,
    isPriced: Boolean,
    sourceName: String,
    localeCurrencyCode: String?,
    onClick: (() -> Unit)?,
) {
    val text = if (info.code == localeCurrencyCode) {
        stringResource(MR.strings.currency_option_label_local, info.code, info.displayName)
    } else {
        stringResource(MR.strings.currency_option_label, info.code, info.displayName)
    }
    val label = currencyFlagEmoji(info.code)?.let { flag -> "$flag $text" } ?: text
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label)
            if (!isPriced) {
                Text(
                    stringResource(MR.strings.currency_not_priced, sourceName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun <T> DropdownSelector(
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedLabel)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
