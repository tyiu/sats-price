package xyz.tyiu.satsprice.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.icerock.moko.resources.compose.stringResource
import xyz.tyiu.satsprice.CurrencyInfo
import xyz.tyiu.satsprice.currencyFlagEmoji
import xyz.tyiu.satsprice.domain.CurrencyConverter
import xyz.tyiu.satsprice.domain.formatAmount
import xyz.tyiu.satsprice.domain.groupDigits
import xyz.tyiu.satsprice.domain.localizedDecimalSeparator
import xyz.tyiu.satsprice.matchesCurrencySearch
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
    val pageScrollState = rememberScrollState()

    if (showCurrencyPicker) {
        CurrencyPickerScreen(
            state = state,
            onToggle = viewModel::onFiatCurrencyToggled,
            onReset = viewModel::onSelectedCurrenciesReset,
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
                .verticalScroll(pageScrollState)
                .padding(horizontal = screenHorizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "SatsPrice",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        val rate = if (state.isManualSource) state.manualRateInput else state.defaultCurrencyRate()
                        OutlinedTextField(
                            value = rate,
                            onValueChange = if (state.isManualSource) viewModel::onManualRateChanged else { _ -> },
                            readOnly = !state.isManualSource,
                            enabled = true,
                            label = { Text(stringResource(MR.strings.btc_to_currency, state.defaultCurrencyCode)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            visualTransformation = DigitGroupingTransformation,
                            trailingIcon = if (state.isManualSource) {
                                null
                            } else {
                                {
                                    if (state.isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    } else {
                                        IconButton(onClick = viewModel::refresh) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = stringResource(MR.strings.refresh_content_description),
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )

                        val oneCurrencyToSats = if (state.isManualSource) {
                            state.manualSatsPerCurrencyInput
                        } else {
                            state.oneCurrencyToSats()
                        }
                        OutlinedTextField(
                            value = oneCurrencyToSats,
                            onValueChange = if (state.isManualSource) {
                                viewModel::onManualSatsPerCurrencyChanged
                            } else {
                                { _ -> }
                            },
                            readOnly = !state.isManualSource,
                            enabled = state.isManualSource || oneCurrencyToSats.isNotEmpty(),
                            label = { Text(stringResource(MR.strings.currency_to_sats, state.defaultCurrencyCode)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            visualTransformation = DigitGroupingTransformation,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    val statusLine = if (state.isManualSource) {
                        null
                    } else {
                        state.lastUpdatedDateTime()?.let { stringResource(MR.strings.updated_status, it) }
                            ?: stringResource(MR.strings.loading_rates_status)
                    }
                    if (statusLine != null) {
                        Text(
                            text = statusLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                    CurrencyAmountGrid(
                        state = state,
                        displayedCurrencies = displayedCurrencies,
                        onAmountChanged = viewModel::onFiatAmountChanged,
                        onReordered = viewModel::onFiatCurrenciesReordered,
                        onRemove = viewModel::onFiatCurrencyToggled,
                    )
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
        }
    }
}

private data class CurrencyDragSession(
    val code: String,
    val baselineOrder: List<String>,
    val origin: Offset,
    val offset: Offset = Offset.Zero,
    val targetGap: Int,
)

@Composable
private fun CurrencyAmountGrid(
    state: ConverterUiState,
    displayedCurrencies: List<String>,
    onAmountChanged: (String, String) -> Unit,
    onReordered: (List<String>) -> Unit,
    onRemove: (String) -> Unit,
) {
    val boundsByCode = remember { mutableStateMapOf<String, CurrencyGridCellBounds>() }
    var dragSession by remember { mutableStateOf<CurrencyDragSession?>(null) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    LaunchedEffect(displayedCurrencies) {
        boundsByCode.keys.retainAll(displayedCurrencies.toSet())
        if (dragSession?.baselineOrder != displayedCurrencies) dragSession = null
    }

    CurrencyGrid(modifier = Modifier.fillMaxWidth()) {
        displayedCurrencies.forEachIndexed { index, code ->
            key(code) {
                val session = dragSession
                val isDragged = session?.code == code
                val draggedIndex = session?.baselineOrder?.indexOf(session.code) ?: -1
                val isNoOpGap = session?.targetGap == draggedIndex || session?.targetGap == draggedIndex + 1
                val targetIndex = session?.targetGap?.let { gap ->
                    if (gap >= displayedCurrencies.size) displayedCurrencies.lastIndex else gap
                }
                val isDropTarget = session != null && !isNoOpGap && targetIndex == index && !isDragged
                val dropIsAfterTarget = session?.targetGap == displayedCurrencies.size
                val isPriced = state.isPriced(code)
                val fieldLabel = currencyFlagEmoji(code)?.let { flag -> "$flag $code" } ?: code
                val dropIndicatorColor = MaterialTheme.colorScheme.primary
                val moveToTopLabel = stringResource(MR.strings.move_currency_to_top_content_description)
                val moveUpLabel = stringResource(MR.strings.move_currency_up_content_description)
                val moveDownLabel = stringResource(MR.strings.move_currency_down_content_description)
                val moveToBottomLabel = stringResource(MR.strings.move_currency_to_bottom_content_description)
                val accessibilityActions = remember(
                    index,
                    displayedCurrencies,
                    moveToTopLabel,
                    moveUpLabel,
                    moveDownLabel,
                    moveToBottomLabel,
                    onReordered,
                ) {
                    buildList {
                        if (index > 0) {
                            add(CustomAccessibilityAction(moveToTopLabel) {
                                onReordered(displayedCurrencies.moved(index, 0))
                                true
                            })
                            add(CustomAccessibilityAction(moveUpLabel) {
                                onReordered(displayedCurrencies.moved(index, index - 1))
                                true
                            })
                        }
                        if (index < displayedCurrencies.lastIndex) {
                            add(CustomAccessibilityAction(moveDownLabel) {
                                onReordered(displayedCurrencies.moved(index, index + 1))
                                true
                            })
                            add(CustomAccessibilityAction(moveToBottomLabel) {
                                onReordered(displayedCurrencies.moved(index, displayedCurrencies.lastIndex))
                                true
                            })
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInParent()
                            val newBounds = CurrencyGridCellBounds(
                                left = bounds.left,
                                top = bounds.top,
                                right = bounds.right,
                                bottom = bounds.bottom,
                            )
                            if (boundsByCode[code] != newBounds) boundsByCode[code] = newBounds
                        }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationX = if (isDragged) session.offset.x else 0f
                            translationY = if (isDragged) session.offset.y else 0f
                            alpha = if (isDragged) 0.82f else 1f
                            shadowElevation = if (isDragged) 12.dp.toPx() else 0f
                        }
                        .drawBehind {
                            if (isDropTarget) {
                                val beforeOnLeft = !isRtl
                                val drawOnLeft = if (dropIsAfterTarget) !beforeOnLeft else beforeOnLeft
                                val inset = 2.dp.toPx()
                                val x = if (drawOnLeft) inset else size.width - inset
                                drawLine(
                                    color = dropIndicatorColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 4.dp.toPx(),
                                )
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedTextField(
                        value = if (isPriced) state.fiatAmounts[code].orEmpty() else "",
                        onValueChange = { onAmountChanged(code, it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = isPriced,
                        isError = !isPriced && !state.isManualSource,
                        label = { Text(fieldLabel) },
                        supportingText = if (isPriced || state.isManualSource) {
                            null
                        } else {
                            { Text(stringResource(MR.strings.currency_not_priced, state.sourceName)) }
                        },
                        visualTransformation = DigitGroupingTransformation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (displayedCurrencies.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics(mergeDescendants = true) { customActions = accessibilityActions }
                                    .focusable()
                                    .onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) {
                                            return@onPreviewKeyEvent false
                                        }
                                        val destination = when (event.key) {
                                            Key.DirectionLeft -> index + if (isRtl) 1 else -1
                                            Key.DirectionRight -> index + if (isRtl) -1 else 1
                                            Key.DirectionUp -> (index - 2).coerceAtLeast(0)
                                            Key.DirectionDown -> (index + 2).coerceAtMost(displayedCurrencies.lastIndex)
                                            else -> return@onPreviewKeyEvent false
                                        }
                                        if (destination !in displayedCurrencies.indices || destination == index) {
                                            return@onPreviewKeyEvent false
                                        }
                                        onReordered(displayedCurrencies.moved(index, destination))
                                        true
                                    }
                                    .pointerInput(code, displayedCurrencies) {
                                        detectDragGestures(
                                            onDragStart = {
                                                val bounds = boundsByCode[code] ?: return@detectDragGestures
                                                dragSession = CurrencyDragSession(
                                                    code = code,
                                                    baselineOrder = displayedCurrencies,
                                                    origin = Offset(bounds.centerX, (bounds.top + bounds.bottom) / 2f),
                                                    targetGap = index,
                                                )
                                            },
                                            onDragCancel = { dragSession = null },
                                            onDragEnd = {
                                                val completed = dragSession
                                                dragSession = null
                                                if (completed != null && completed.baselineOrder == displayedCurrencies) {
                                                    val reordered = moveCurrencyToGap(
                                                        completed.baselineOrder,
                                                        completed.code,
                                                        completed.targetGap,
                                                    )
                                                    if (reordered != completed.baselineOrder) onReordered(reordered)
                                                }
                                            },
                                        ) { change, dragAmount ->
                                            change.consume()
                                            val current = dragSession ?: return@detectDragGestures
                                            val newOffset = current.offset + dragAmount
                                            dragSession = current.copy(
                                                offset = newOffset,
                                                targetGap = currencyDropGap(
                                                    current.baselineOrder,
                                                    boundsByCode,
                                                    current.origin.x + newOffset.x,
                                                    current.origin.y + newOffset.y,
                                                    isRtl,
                                                ),
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = stringResource(MR.strings.reorder_currency_content_description, code),
                                )
                            }
                            if (code != state.defaultCurrencyCode) {
                                IconButton(onClick = { onRemove(code) }, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(
                                            MR.strings.remove_currency_named_content_description,
                                            code,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyGrid(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        require(constraints.hasBoundedWidth) { "CurrencyGrid requires bounded width" }
        val spacing = 8.dp.roundToPx()
        val columnWidth = ((constraints.maxWidth - spacing) / 2).coerceAtLeast(0)
        val childConstraints = androidx.compose.ui.unit.Constraints(
            minWidth = columnWidth,
            maxWidth = columnWidth,
            minHeight = 0,
            maxHeight = constraints.maxHeight,
        )
        val placeables = measurables.map { it.measure(childConstraints) }
        val rowHeights = placeables.chunked(2).map { row -> row.maxOf { it.height } }
        val height = (rowHeights.sum() + spacing * (rowHeights.size - 1).coerceAtLeast(0))
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(constraints.maxWidth, height) {
            var y = 0
            placeables.chunked(2).forEachIndexed { rowIndex, row ->
                row.forEachIndexed { columnIndex, placeable ->
                    placeable.placeRelative(x = columnIndex * (columnWidth + spacing), y = y)
                }
                y += rowHeights[rowIndex] + spacing
            }
        }
    }
}

private fun List<String>.moved(fromIndex: Int, toIndex: Int): List<String> =
    toMutableList().apply { add(toIndex, removeAt(fromIndex)) }

/**
 * A full-screen takeover (rather than a dropdown) matching the previous Skip-based app's
 * dedicated currency selection screen: a pinned, non-removable "Current Currency", the other
 * currencies the user added (removable), and the remaining ones available to add.
 */
@Composable
private fun CurrencyPickerScreen(
    state: ConverterUiState,
    onToggle: (String) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(stringResource(MR.strings.reset_selected_currencies_confirmation_title)) },
            text = {
                Text(stringResource(MR.strings.reset_selected_currencies_confirmation_message, state.defaultCurrencyCode))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onReset()
                    },
                ) { Text(stringResource(MR.strings.reset_selected_currencies_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text(stringResource(MR.strings.cancel)) }
            },
        )
    }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.selectedFiatCurrencies.size > 1) {
                        TextButton(onClick = { showResetConfirmation = true }) {
                            Text(stringResource(MR.strings.reset_selected_currencies_button))
                        }
                    }
                    TextButton(onClick = onDone) { Text(stringResource(MR.strings.done)) }
                }
            }

            var searchQuery by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(MR.strings.search_currencies_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(MR.strings.clear_search_content_description),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = screenHorizontalPadding),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = screenHorizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val currentCurrency = state.currentCurrency()
                val selectedOthers = state.selectedOtherCurrencies().filter { matchesCurrencySearch(it, searchQuery) }
                val unselected = state.unselectedCurrencies().filter { matchesCurrencySearch(it, searchQuery) }

                if (matchesCurrencySearch(currentCurrency, searchQuery)) {
                    Column {
                        Text(
                            stringResource(MR.strings.current_currency_section_title).uppercase(),
                            style = SectionHeaderStyle,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        CurrencyRow(
                            info = currentCurrency,
                            isSelected = true,
                            isPriced = state.isPriced(currentCurrency.code),
                            sourceName = state.sourceName,
                            localeCurrencyCode = state.localeCurrencyCode,
                            onClick = null,
                        )
                    }
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

                val (unselectedPriced, unselectedUnpriced) = unselected.partition { state.isPriced(it.code) }

                if (unselectedPriced.isNotEmpty()) {
                    Column {
                        Text(
                            stringResource(MR.strings.priced_currencies_section_title).uppercase(),
                            style = SectionHeaderStyle,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        unselectedPriced.forEach { info ->
                            key(info.code) {
                                CurrencyRow(
                                    info = info,
                                    isSelected = false,
                                    isPriced = true,
                                    sourceName = state.sourceName,
                                    localeCurrencyCode = state.localeCurrencyCode,
                                    onClick = { onToggle(info.code) },
                                )
                            }
                        }
                    }
                }

                if (unselectedUnpriced.isNotEmpty()) {
                    Column {
                        Text(
                            stringResource(MR.strings.unpriced_currencies_section_title).uppercase(),
                            style = SectionHeaderStyle,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        unselectedUnpriced.forEach { info ->
                            key(info.code) {
                                CurrencyRow(
                                    info = info,
                                    isSelected = false,
                                    isPriced = false,
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
