package xyz.tyiu.satsprice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.tyiu.satsprice.ui.ConverterUiState
import xyz.tyiu.satsprice.ui.PriceViewModel
import xyz.tyiu.satsprice.ui.defaultCurrencyRate
import xyz.tyiu.satsprice.ui.exceedsMaxSupply
import xyz.tyiu.satsprice.ui.statusLine

data class FiatRow(val code: String, val amount: String, val rateDisplay: String)

data class IosConverterState(
    val btcAmount: String,
    val satsAmount: String,
    val exceedsMaxSupply: Boolean,
    val fiatRows: List<FiatRow>,
    val availableCurrencies: List<CurrencyInfo>,
    val selectedCurrencyCodes: List<String>,
    val localeCurrencyCode: String?,
    val defaultCurrencyCode: String,
    val sourceName: String,
    val isManualSource: Boolean,
    val manualRateInput: String,
    val isLoading: Boolean,
    val errorMessage: String?,
    val statusLine: String,
    val defaultCurrencyRate: String,
)

/**
 * Swift-facing bridge over [PriceViewModel]. Exposes a flattened, Map-free state shape
 * (Kotlin/Native's ObjC/Swift export handles `List<DataClass>` far better than `Map<K, V>`)
 * and observes via a plain callback rather than raw `Flow`, so no extra Swift interop
 * tooling (e.g. SKIE) is required.
 */
class IosPriceViewModel {
    private val viewModel = PriceViewModel()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val sourceNames: List<String> get() = viewModel.availableSources

    fun observeState(onChange: (IosConverterState) -> Unit) {
        scope.launch {
            viewModel.uiState.collect { state -> onChange(state.toIosState()) }
        }
    }

    fun refresh() = viewModel.refresh()
    fun onBtcAmountChanged(value: String) = viewModel.onBtcAmountChanged(value)
    fun onSatsAmountChanged(value: String) = viewModel.onSatsAmountChanged(value)
    fun onFiatAmountChanged(code: String, value: String) = viewModel.onFiatAmountChanged(code, value)
    fun onFiatCurrencyToggled(code: String) = viewModel.onFiatCurrencyToggled(code)
    fun onSourceSelected(name: String) = viewModel.onSourceSelected(name)
    fun onManualRateChanged(value: String) = viewModel.onManualRateChanged(value)
}

private fun ConverterUiState.toIosState(): IosConverterState = IosConverterState(
    btcAmount = btcAmount,
    satsAmount = satsAmount,
    exceedsMaxSupply = exceedsMaxSupply(),
    fiatRows = selectedFiatCurrencies.map { code ->
        FiatRow(code, fiatAmounts[code].orEmpty(), rateDisplays[code].orEmpty())
    },
    availableCurrencies = availableFiatCurrencies,
    selectedCurrencyCodes = selectedFiatCurrencies,
    localeCurrencyCode = localeCurrencyCode,
    defaultCurrencyCode = defaultCurrencyCode,
    sourceName = sourceName,
    isManualSource = isManualSource,
    manualRateInput = manualRateInput,
    isLoading = isLoading,
    errorMessage = errorMessage,
    statusLine = statusLine(),
    defaultCurrencyRate = defaultCurrencyRate(),
)
