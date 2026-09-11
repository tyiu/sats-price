package xyz.tyiu.satsprice.data.db

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.tyiu.satsprice.data.ExchangeRates
import xyz.tyiu.satsprice.db.AppDatabase
import kotlin.time.Instant

/**
 * Shared by every platform that opens a real [AppDatabase] (Android, Desktop, iOS/macOS) — only
 * driver construction differs per platform, so that's the only expect/actual boundary needed.
 * Web has no real SQLDelight driver and uses a `localStorage`-backed fallback instead (see
 * `LocalStorageStores.kt`).
 */
internal class SqlDelightExchangeRateStore(private val database: AppDatabase) : ExchangeRateStore {
    override suspend fun loadLastKnownRates(sourceId: String): ExchangeRates? = withContext(Dispatchers.Default) {
        val rows = database.exchangeRateQueries.selectForSource(sourceId).executeAsList()
        val first = rows.firstOrNull() ?: return@withContext null
        ExchangeRates(
            base = first.base,
            rates = rows.associate { it.currencyCode to BigDecimal.parseString(it.rate) },
            fetchedAt = Instant.fromEpochMilliseconds(first.fetchedAt),
        )
    }

    override suspend fun saveRates(sourceId: String, rates: ExchangeRates) = withContext(Dispatchers.Default) {
        database.exchangeRateQueries.transaction {
            database.exchangeRateQueries.deleteForSource(sourceId)
            rates.rates.forEach { (currencyCode, rate) ->
                database.exchangeRateQueries.insertRate(
                    sourceId = sourceId,
                    base = rates.base,
                    currencyCode = currencyCode,
                    rate = rate.toStringExpanded(),
                    fetchedAt = rates.fetchedAt.toEpochMilliseconds(),
                )
            }
        }
    }
}

internal class SqlDelightSelectedCurrenciesStore(private val database: AppDatabase) : SelectedCurrenciesStore {
    override suspend fun loadSelectedCurrencies(): List<String> = withContext(Dispatchers.Default) {
        database.selectedCurrencyQueries.selectAllOrdered().executeAsList().map { it.code }
    }

    override suspend fun saveSelectedCurrencies(codes: List<String>) = withContext(Dispatchers.Default) {
        database.selectedCurrencyQueries.transaction {
            database.selectedCurrencyQueries.deleteAll()
            codes.forEachIndexed { index, code ->
                database.selectedCurrencyQueries.insertSelected(code = code, position = index.toLong())
            }
        }
    }
}

internal class SqlDelightSelectedSourceStore(private val database: AppDatabase) : SelectedSourceStore {
    override suspend fun loadSelectedSourceId(): String? = withContext(Dispatchers.Default) {
        database.selectedSourceQueries.select().executeAsOneOrNull()?.sourceId
    }

    override suspend fun saveSelectedSourceId(sourceId: String) = withContext(Dispatchers.Default) {
        database.selectedSourceQueries.transaction {
            database.selectedSourceQueries.upsert(sourceId)
        }
    }
}
