package xyz.tyiu.satsprice.data.db

/** Persists the id of the last price source the user selected. */
interface SelectedSourceStore {
    suspend fun loadSelectedSourceId(): String?
    suspend fun saveSelectedSourceId(sourceId: String)
}

expect fun createSelectedSourceStore(): SelectedSourceStore
