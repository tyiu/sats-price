package xyz.tyiu.satsprice

import android.app.Application
import xyz.tyiu.satsprice.data.db.initAndroidContext

class SatsPriceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAndroidContext(this)
    }
}
