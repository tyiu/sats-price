package xyz.tyiu.satsprice.data.db

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import xyz.tyiu.satsprice.db.AppDatabase

private val database: AppDatabase by lazy {
    AppDatabase(AndroidSqliteDriver(AppDatabase.Schema, androidAppContext, "sats-price.db"))
}

actual fun createExchangeRateStore(): ExchangeRateStore = SqlDelightExchangeRateStore(database)
actual fun createSelectedCurrenciesStore(): SelectedCurrenciesStore = SqlDelightSelectedCurrenciesStore(database)
actual fun createSelectedSourceStore(): SelectedSourceStore = SqlDelightSelectedSourceStore(database)
