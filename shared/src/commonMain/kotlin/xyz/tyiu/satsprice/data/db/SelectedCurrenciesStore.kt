package xyz.tyiu.satsprice.data.db

/** Persists the user's selected fiat currency codes, preserving the order they're displayed in. */
interface SelectedCurrenciesStore {
    suspend fun loadSelectedCurrencies(): List<String>
    suspend fun saveSelectedCurrencies(codes: List<String>)
}

expect fun createSelectedCurrenciesStore(): SelectedCurrenciesStore
