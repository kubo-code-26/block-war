package com.blockwar

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk

class BlockWarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlayGamesSdk.initialize(this)
    }
}
