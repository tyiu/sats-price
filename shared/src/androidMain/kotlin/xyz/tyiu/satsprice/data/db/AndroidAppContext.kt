package xyz.tyiu.satsprice.data.db

import android.content.Context

internal lateinit var androidAppContext: Context
    private set

/** Called once from [android.app.Application.onCreate] so the shared module can open its database. */
fun initAndroidContext(context: Context) {
    androidAppContext = context.applicationContext
}
