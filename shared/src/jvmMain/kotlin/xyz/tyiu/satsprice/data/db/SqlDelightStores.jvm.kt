package xyz.tyiu.satsprice.data.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import xyz.tyiu.satsprice.db.AppDatabase
import java.io.File

private val database: AppDatabase by lazy {
    val appDir = File(System.getProperty("user.home"), ".sats-price").apply { mkdirs() }
    val databaseFile = File(appDir, "sats-price.db")
    val isNewDatabase = !databaseFile.exists()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    if (isNewDatabase) {
        AppDatabase.Schema.create(driver)
    }
    AppDatabase(driver)
}

actual fun createExchangeRateStore(): ExchangeRateStore = SqlDelightExchangeRateStore(database)
actual fun createSelectedCurrenciesStore(): SelectedCurrenciesStore = SqlDelightSelectedCurrenciesStore(database)
actual fun createSelectedSourceStore(): SelectedSourceStore = SqlDelightSelectedSourceStore(database)
