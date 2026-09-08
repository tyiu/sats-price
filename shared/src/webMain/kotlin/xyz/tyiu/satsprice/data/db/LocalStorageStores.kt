package xyz.tyiu.satsprice.data.db

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.browser.localStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.tyiu.satsprice.data.ExchangeRates
import kotlin.time.Instant

/**
 * Web isn't a shipped platform (see README's Supported Platforms) and SQLDelight's web driver
 * needs a worker plus a wasm sqlite binary, so persistence here goes through the browser's
 * localStorage instead of a real database — plenty for this small amount of data, and it
 * survives page reloads unlike the in-memory state it replaced.
 */
private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class StoredExchangeRates(val base: String, val rates: Map<String, String>, val fetchedAtMillis: Long)

private class LocalStorageExchangeRateStore : ExchangeRateStore {
    override suspend fun loadLastKnownRates(sourceId: String): ExchangeRates? {
        val raw = localStorage.getItem(key(sourceId)) ?: return null
        val stored = try {
            json.decodeFromString<StoredExchangeRates>(raw)
        } catch (e: Exception) {
            return null
        }
        return ExchangeRates(
            base = stored.base,
            rates = stored.rates.mapValues { (_, value) -> BigDecimal.parseString(value) },
            fetchedAt = Instant.fromEpochMilliseconds(stored.fetchedAtMillis),
        )
    }

    override suspend fun saveRates(sourceId: String, rates: ExchangeRates) {
        val stored = StoredExchangeRates(
            base = rates.base,
            rates = rates.rates.mapValues { (_, value) -> value.toStringExpanded() },
            fetchedAtMillis = rates.fetchedAt.toEpochMilliseconds(),
        )
        localStorage.setItem(key(sourceId), json.encodeToString(stored))
    }

    private fun key(sourceId: String) = "satsprice.exchangeRate.$sourceId"
}

private class LocalStorageSelectedCurrenciesStore : SelectedCurrenciesStore {
    override suspend fun loadSelectedCurrencies(): List<String> {
        val raw = localStorage.getItem(KEY) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveSelectedCurrencies(codes: List<String>) {
        localStorage.setItem(KEY, json.encodeToString(codes))
    }

    private companion object {
        const val KEY = "satsprice.selectedCurrencies"
    }
}

private class LocalStorageSelectedSourceStore : SelectedSourceStore {
    override suspend fun loadSelectedSourceId(): String? = localStorage.getItem(KEY)

    override suspend fun saveSelectedSourceId(sourceId: String) {
        localStorage.setItem(KEY, sourceId)
    }

    private companion object {
        const val KEY = "satsprice.selectedSourceId"
    }
}

actual fun createExchangeRateStore(): ExchangeRateStore = LocalStorageExchangeRateStore()
actual fun createSelectedCurrenciesStore(): SelectedCurrenciesStore = LocalStorageSelectedCurrenciesStore()
actual fun createSelectedSourceStore(): SelectedSourceStore = LocalStorageSelectedSourceStore()
