package xyz.tyiu.satsprice.data.db

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import xyz.tyiu.satsprice.db.AppDatabase

private val database: AppDatabase by lazy {
    AppDatabase(NativeSqliteDriver(AppDatabase.Schema, "sats-price.db"))
}

actual fun createExchangeRateStore(): ExchangeRateStore = SqlDelightExchangeRateStore(database)
actual fun createSelectedCurrenciesStore(): SelectedCurrenciesStore = SqlDelightSelectedCurrenciesStore(database)
actual fun createSelectedSourceStore(): SelectedSourceStore = SqlDelightSelectedSourceStore(database)
