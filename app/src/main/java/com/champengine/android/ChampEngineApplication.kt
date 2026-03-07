package com.champengine.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChampEngineApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // SQLCipher initialization is handled in the DI module
        // Sentinel starts when the app is foregrounded — not on boot
    }
}
