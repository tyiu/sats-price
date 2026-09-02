package xyz.tyiu.satsprice.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.icerock.moko.resources.compose.stringResource
import xyz.tyiu.satsprice.CurrencyInfo
import xyz.tyiu.satsprice.shared.MR

private val SectionColors
    @Composable get() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

@Composable
fun PriceScreen(
    viewModel: PriceViewModel = viewModel { PriceViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                Text(
                    text = state.statusLine(),
                    style = MaterialTheme.typography.bodySmall,
                )
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
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            val rate = state.defaultCurrencyRate()
                            if (rate.isNotEmpty()) {
                                Text(
                                    text = rate,
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
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
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = state.btcAmount,
                        onValueChange = viewModel::onBtcAmountChanged,
                        label = { Text(stringResource(MR.strings.btc_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = exceedsMaxSupply,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (exceedsMaxSupply) {
                        Text(
                            text = stringResource(MR.strings.exceeds_max_supply),
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
                        MultiCurrencySelector(
                            selectedCodes = state.selectedFiatCurrencies,
                            options = state.availableFiatCurrencies,
                            localeCurrencyCode = state.localeCurrencyCode,
                            onToggle = viewModel::onFiatCurrencyToggled,
                        )
                    }

                    state.selectedFiatCurrencies.forEach { code ->
                        key(code) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = state.fiatAmounts[code].orEmpty(),
                                    onValueChange = { viewModel.onFiatAmountChanged(code, it) },
                                    label = { Text(code) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.onFiatCurrencyToggled(code) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(
                                            MR.strings.remove_currency_content_description,
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
private fun MultiCurrencySelector(
    selectedCodes: List<String>,
    options: List<CurrencyInfo>,
    localeCurrencyCode: String?,
    onToggle: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(
                if (selectedCodes.isEmpty()) {
                    stringResource(MR.strings.add_currency)
                } else {
                    stringResource(MR.strings.currencies_selected_count, selectedCodes.size)
                },
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { info ->
                val checked = info.code in selectedCodes
                val label = if (info.code == localeCurrencyCode) {
                    stringResource(MR.strings.currency_option_label_local, info.code, info.displayName)
                } else {
                    stringResource(MR.strings.currency_option_label, info.code, info.displayName)
                }
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { Checkbox(checked = checked, onCheckedChange = { onToggle(info.code) }) },
                    onClick = { onToggle(info.code) },
                )
            }
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
