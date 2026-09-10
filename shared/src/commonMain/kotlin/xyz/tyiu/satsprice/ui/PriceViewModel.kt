package xyz.tyiu.satsprice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.tyiu.satsprice.CurrencyInfo
import xyz.tyiu.satsprice.currencyDecimalDigits
import xyz.tyiu.satsprice.data.CoinbaseExchangeRateSource
import xyz.tyiu.satsprice.data.CoinGeckoExchangeRateSource
import xyz.tyiu.satsprice.data.ExchangeRateSource
import xyz.tyiu.satsprice.data.ExchangeRates
import xyz.tyiu.satsprice.data.ManualExchangeRateSource
import xyz.tyiu.satsprice.data.createHttpClient
import xyz.tyiu.satsprice.data.db.ExchangeRateStore
import xyz.tyiu.satsprice.data.db.SelectedCurrenciesStore
import xyz.tyiu.satsprice.data.db.SelectedSourceStore
import xyz.tyiu.satsprice.data.db.createExchangeRateStore
import xyz.tyiu.satsprice.data.db.createSelectedCurrenciesStore
import xyz.tyiu.satsprice.data.db.createSelectedSourceStore
import xyz.tyiu.satsprice.domain.CurrencyConverter
import xyz.tyiu.satsprice.domain.formatAmount
import xyz.tyiu.satsprice.domain.formatAmountFixed
import xyz.tyiu.satsprice.domain.sanitizeDecimalInput
import xyz.tyiu.satsprice.domain.sanitizeIntegerInput
import xyz.tyiu.satsprice.domain.toBigDecimalOrNull
import xyz.tyiu.satsprice.localeCurrencyCode
import xyz.tyiu.satsprice.systemCurrencies
import xyz.tyiu.satsprice.warmRegionDisplayNameCache
import kotlin.time.Instant

private const val AUTO_REFRESH_INTERVAL_MILLIS = 60_000L

data class ConverterUiState(
    val btcAmount: String = "1",
    val satsAmount: String = formatAmount(CurrencyConverter.SATS_PER_BTC, 0),
    val selectedFiatCurrencies: List<String> = listOf("USD"),
    val fiatAmounts: Map<String, String> = emptyMap(),
    val rateDisplays: Map<String, String> = emptyMap(),
    val availableFiatCurrencies: List<CurrencyInfo> = emptyList(),
    val pricedCurrencyCodes: Set<String> = emptySet(),
    val localeCurrencyCode: String? = null,
    val defaultCurrencyCode: String = "USD",
    val sourceName: String = "",
    val isManualSource: Boolean = false,
    val manualRateInput: String = "",
    val manualSatsPerCurrencyInput: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val lastUpdated: Instant? = null,
)

private sealed interface EditedField {
    data object Btc : EditedField
    data object Sats : EditedField
    data class Fiat(val code: String) : EditedField
}

class PriceViewModel(
    httpClient: HttpClient = createHttpClient(),
    private val manualSource: ManualExchangeRateSource = ManualExchangeRateSource(),
    private val coinbaseSource: ExchangeRateSource = CoinbaseExchangeRateSource(httpClient),
    private val coinGeckoSource: ExchangeRateSource = CoinGeckoExchangeRateSource(httpClient),
    private val exchangeRateStore: ExchangeRateStore = createExchangeRateStore(),
    private val selectedCurrenciesStore: SelectedCurrenciesStore = createSelectedCurrenciesStore(),
    private val selectedSourceStore: SelectedSourceStore = createSelectedSourceStore(),
) : ViewModel() {

    private val sources: List<ExchangeRateSource> = listOf(coinbaseSource, coinGeckoSource, manualSource)
    val availableSources: List<String> = sources.map { it.displayName }

    private var currentSource: ExchangeRateSource = coinbaseSource
    private val systemCurrencyList: List<CurrencyInfo> by lazy { systemCurrencies() }
    private val localCurrencyCode: String? by lazy {
        localeCurrencyCode()?.takeIf { code -> systemCurrencyList.any { it.code == code } }
    }
    private val defaultCurrencyCode: String by lazy { localCurrencyCode ?: "USD" }
    private val decimalDigitsCache = mutableMapOf<String, Int>()
    private fun decimalDigitsFor(code: String) = decimalDigitsCache.getOrPut(code) { currencyDecimalDigits(code) }

    private val _uiState = MutableStateFlow(
        ConverterUiState(
            selectedFiatCurrencies = listOf(defaultCurrencyCode),
            // Every system currency is offered regardless of whether the active source prices
            // it — CurrencyRow/OutlinedTextField indicate unpriced ones instead of hiding them,
            // so switching sources doesn't silently drop currencies from the user's selection.
            availableFiatCurrencies = systemCurrencyList.let { list ->
                val (matching, rest) = list.partition { it.code == defaultCurrencyCode }
                matching + rest
            },
            localeCurrencyCode = localCurrencyCode,
            defaultCurrencyCode = defaultCurrencyCode,
            sourceName = coinbaseSource.displayName,
        ),
    )
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    private var rates: ExchangeRates? = null
    private var lastPersistedSelection: List<String>? = null

    init {
        manualSource.currencyCode = defaultCurrencyCode
        // Fired off separately (rather than folded into the launch below) so it doesn't delay
        // that one's own startup work — this just needs to finish before the user reaches the
        // currency picker's search field, not before anything else.
        viewModelScope.launch { warmRegionDisplayNameCache(systemCurrencyList) }
        viewModelScope.launch {
            selectedCurrenciesStore.loadSelectedCurrencies().takeIf { it.isNotEmpty() }?.let { persisted ->
                lastPersistedSelection = persisted
                val selection = if (defaultCurrencyCode in persisted) persisted else listOf(defaultCurrencyCode) + persisted
                _uiState.update { it.copy(selectedFiatCurrencies = selection) }
            }
            selectedSourceStore.loadSelectedSourceId()
                ?.let { id -> sources.firstOrNull { it.id == id } }
                ?.let { persisted ->
                    currentSource = persisted
                    _uiState.update {
                        it.copy(sourceName = persisted.displayName, isManualSource = persisted === manualSource)
                    }
                }
            seedFromCache(currentSource)
            while (isActive) {
                refresh()
                delay(AUTO_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    /** Shows the last known rates for [source] immediately, before its network fetch completes. */
    private suspend fun seedFromCache(source: ExchangeRateSource) {
        val cached = exchangeRateStore.loadLastKnownRates(source.id) ?: return
        rates = cached
        _uiState.update { state -> recomputeFromKnownField(state.copy(lastUpdated = cached.fetchedAt), cached) }
    }

    private fun persistSelectionIfChanged(selection: List<String>) {
        if (selection == lastPersistedSelection) return
        lastPersistedSelection = selection
        viewModelScope.launch { selectedCurrenciesStore.saveSelectedCurrencies(selection) }
    }

    fun refresh() {
        if (currentSource === manualSource && manualSource.rate == null) {
            // No rate typed in (yet): nothing to price fiat currencies with, so clear any stale
            // rate this might otherwise still compute fiat amounts (or the derived "currency to
            // Sats" rate) from.
            rates = null
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    pricedCurrencyCodes = emptySet(),
                    rateDisplays = emptyMap(),
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val newRates = currentSource.getRates("BTC")
                rates = newRates
                exchangeRateStore.saveRates(currentSource.id, newRates)
                _uiState.update { state ->
                    recomputeFromKnownField(
                        state.copy(isLoading = false, errorMessage = null, lastUpdated = newRates.fetchedAt),
                        newRates,
                    )
                }
            } catch (_: Exception) {
                // A fixed, generic message rather than e.message: exceptions from the HTTP client
                // or JSON parsing can carry verbose, technical text (full response bodies, stack
                // traces) that isn't meaningful to a user. Not localized: this shared ViewModel has
                // no platform Context to resolve a moko-resources string on Android, and no
                // locale-aware synchronous resolution path that works everywhere.
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Could not fetch exchange rates")
                }
            }
        }
    }

    fun onBtcAmountChanged(value: String) = updateAmount(EditedField.Btc, sanitizeDecimalInput(value))
    fun onSatsAmountChanged(value: String) = updateAmount(EditedField.Sats, sanitizeIntegerInput(value))
    fun onFiatAmountChanged(code: String, value: String) =
        updateAmount(EditedField.Fiat(code), sanitizeDecimalInput(value))

    /** [defaultCurrencyCode] is the pinned "Current Currency" and can't be removed. */
    fun onFiatCurrencyToggled(code: String) {
        if (code == defaultCurrencyCode) return
        _uiState.update { state ->
            val newSelection = if (code in state.selectedFiatCurrencies) {
                state.selectedFiatCurrencies - code
            } else {
                state.selectedFiatCurrencies + code
            }
            val newState = state.copy(selectedFiatCurrencies = newSelection)
            rates?.let { recomputeFromKnownField(newState, it) } ?: newState
        }
        persistSelectionIfChanged(_uiState.value.selectedFiatCurrencies)
    }

    /** Removes every selected currency except the pinned [defaultCurrencyCode]. */
    fun onSelectedCurrenciesReset() {
        _uiState.update { state ->
            if (state.selectedFiatCurrencies == listOf(defaultCurrencyCode)) return@update state
            val newState = state.copy(selectedFiatCurrencies = listOf(defaultCurrencyCode))
            rates?.let { recomputeFromKnownField(newState, it) } ?: newState
        }
        persistSelectionIfChanged(_uiState.value.selectedFiatCurrencies)
    }

    /**
     * Reorders the selected currencies to [newOrder]. Any code in [newOrder] that isn't
     * currently selected is ignored, and any currently-selected code missing from [newOrder]
     * keeps its relative position at the end — callers only need to describe the reordering of
     * codes they know about (e.g. a Swift `List.onMove`'s resulting order).
     */
    fun onFiatCurrenciesReordered(newOrder: List<String>) {
        _uiState.update { state ->
            val known = newOrder.filter { it in state.selectedFiatCurrencies }
            val missing = state.selectedFiatCurrencies.filterNot { it in known }
            val newSelection = known + missing
            if (newSelection == state.selectedFiatCurrencies) return@update state
            val newState = state.copy(selectedFiatCurrencies = newSelection)
            rates?.let { recomputeFromKnownField(newState, it) } ?: newState
        }
        persistSelectionIfChanged(_uiState.value.selectedFiatCurrencies)
    }

    fun onSourceSelected(displayName: String) {
        val selected = sources.firstOrNull { it.displayName == displayName } ?: return
        val previousRates = rates
        currentSource = selected
        rates = null
        _uiState.update {
            it.copy(
                sourceName = selected.displayName,
                isManualSource = selected === manualSource,
                rateDisplays = emptyMap(),
            )
        }

        if (selected === manualSource && manualSource.rate == null) {
            // Switching to Manual for the first time: seed it with whatever rate is already
            // known for the default currency — the source just switched away from, or else
            // whatever other source was last fetched — rather than starting blank.
            viewModelScope.launch {
                selectedSourceStore.saveSelectedSourceId(selected.id)
                val seedRate = previousRates?.rates?.get(defaultCurrencyCode) ?: fallbackManualRate()
                if (seedRate != null) {
                    manualSource.rate = seedRate
                    _uiState.update {
                        it.copy(
                            manualRateInput = formatAmount(seedRate, decimalDigitsFor(defaultCurrencyCode)),
                            manualSatsPerCurrencyInput = CurrencyConverter.satsPerCurrencyUnit(seedRate)
                                ?.let { sats -> formatAmount(sats, 0) }
                                .orEmpty(),
                        )
                    }
                }
                refresh()
            }
            return
        }

        viewModelScope.launch {
            selectedSourceStore.saveSelectedSourceId(selected.id)
            seedFromCache(selected)
            refresh()
        }
    }

    /** The default currency's last known rate from any non-Manual source's cache, if any. */
    private suspend fun fallbackManualRate(): BigDecimal? =
        sources.filterNot { it === manualSource }.firstNotNullOfOrNull { source ->
            exchangeRateStore.loadLastKnownRates(source.id)?.rates?.get(defaultCurrencyCode)
        }

    /**
     * The "BTC to [defaultCurrencyCode]" and "[defaultCurrencyCode] to Sats" fields edit the same
     * underlying [ManualExchangeRateSource.rate] from two different angles — entering one always
     * recomputes the other, via the same self-inverse division ([CurrencyConverter.satsPerCurrencyUnit]
     * of a rate is a Sats amount, and of a Sats amount is a rate).
     */
    fun onManualRateChanged(value: String) {
        val sanitized = sanitizeDecimalInput(value)
        // Also cleared (rather than left as the last valid rate) when sanitized fails to parse —
        // e.g. the field is emptied, or is mid-edit on an incomplete number — so refresh() then
        // wipes any fiat amounts computed from it, rather than leaving stale ones on screen.
        val parsed = sanitized.toBigDecimalOrNull()
        manualSource.rate = parsed
        _uiState.update {
            it.copy(
                manualRateInput = sanitized,
                manualSatsPerCurrencyInput = parsed?.let { rate -> CurrencyConverter.satsPerCurrencyUnit(rate) }
                    ?.let { sats -> formatAmount(sats, 0) }
                    .orEmpty(),
            )
        }
        refresh()
    }

    fun onManualSatsPerCurrencyChanged(value: String) {
        // Sats are indivisible — unlike the rate field, this one takes whole numbers only.
        val sanitized = sanitizeIntegerInput(value)
        val parsedSats = sanitized.toBigDecimalOrNull()
        val rate = parsedSats?.let { sats -> CurrencyConverter.satsPerCurrencyUnit(sats) }
        manualSource.rate = rate
        _uiState.update {
            it.copy(
                manualSatsPerCurrencyInput = sanitized,
                manualRateInput = rate?.let { formatAmount(it, decimalDigitsFor(defaultCurrencyCode)) }.orEmpty(),
            )
        }
        refresh()
    }

    private fun updateAmount(field: EditedField, rawValue: String) {
        _uiState.update { state ->
            val currentValue = when (field) {
                EditedField.Btc -> state.btcAmount
                EditedField.Sats -> state.satsAmount
                is EditedField.Fiat -> state.fiatAmounts[field.code].orEmpty()
            }
            // Some platform text fields (notably SwiftUI's) can re-fire the binding's setter with
            // the field's current, unchanged text just from gaining focus. Treat that as a no-op
            // rather than re-deriving every other field from this one's rounded display value.
            if (rawValue == currentValue) return@update state

            val withRaw = state.withField(field, rawValue)
            val amount = rawValue.toBigDecimalOrNull()
            val currentRates = rates
            if (amount == null || currentRates == null) return@update withRaw

            val btcValue: BigDecimal? = when (field) {
                EditedField.Btc -> amount
                EditedField.Sats -> CurrencyConverter.satsToBtc(amount)
                is EditedField.Fiat -> CurrencyConverter.fiatToBtc(amount, currentRates, field.code)
            }
            if (btcValue == null) return@update withRaw

            withRaw.copy(
                btcAmount = if (field == EditedField.Btc) withRaw.btcAmount else formatAmount(btcValue, 8),
                satsAmount = if (field == EditedField.Sats) {
                    withRaw.satsAmount
                } else {
                    formatAmount(CurrencyConverter.btcToSats(btcValue), 0)
                },
                fiatAmounts = withRaw.fiatAmounts + state.selectedFiatCurrencies
                    .filterNot { code -> field is EditedField.Fiat && field.code == code }
                    .associateWith { code ->
                        CurrencyConverter.btcToFiat(btcValue, currentRates, code)
                            ?.let { formatAmountFixed(it, decimalDigitsFor(code)) }
                            ?: withRaw.fiatAmounts[code].orEmpty()
                    },
            )
        }
    }

    private fun ConverterUiState.withField(field: EditedField, value: String): ConverterUiState = when (field) {
        EditedField.Btc -> copy(btcAmount = value)
        EditedField.Sats -> copy(satsAmount = value)
        is EditedField.Fiat -> copy(fiatAmounts = fiatAmounts + (field.code to value))
    }

    /** Recomputes every other field from whichever field currently holds a valid number. */
    private fun recomputeFromKnownField(state: ConverterUiState, rates: ExchangeRates): ConverterUiState {
        val btc = state.btcAmount.toBigDecimalOrNull()
        val sats = state.satsAmount.toBigDecimalOrNull()
        var sourceFiatCode: String? = null

        val btcValue: BigDecimal? = when {
            btc != null -> btc
            sats != null -> CurrencyConverter.satsToBtc(sats)
            else -> state.selectedFiatCurrencies.firstNotNullOfOrNull { code ->
                state.fiatAmounts[code]?.toBigDecimalOrNull()
                    ?.let { amount -> CurrencyConverter.fiatToBtc(amount, rates, code) }
                    ?.also { sourceFiatCode = code }
            }
        }

        val rateDisplays = (state.selectedFiatCurrencies + state.defaultCurrencyCode).distinct().associateWith { code ->
            rates.rates[code]?.let { formatAmountFixed(it, decimalDigitsFor(code)) } ?: ""
        }
        val withRateInfo = state.copy(pricedCurrencyCodes = rates.rates.keys, rateDisplays = rateDisplays)

        if (btcValue == null) return withRateInfo

        return withRateInfo.copy(
            btcAmount = if (btc != null) state.btcAmount else formatAmount(btcValue, 8),
            satsAmount = if (sats != null) state.satsAmount else formatAmount(CurrencyConverter.btcToSats(btcValue), 0),
            fiatAmounts = state.selectedFiatCurrencies.associateWith { code ->
                if (code == sourceFiatCode) {
                    state.fiatAmounts[code].orEmpty()
                } else {
                    CurrencyConverter.btcToFiat(btcValue, rates, code)
                        ?.let { formatAmountFixed(it, decimalDigitsFor(code)) }
                        ?: state.fiatAmounts[code].orEmpty()
                }
            },
        )
    }
}
