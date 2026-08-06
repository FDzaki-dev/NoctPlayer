package com.noctplayer.app

import android.app.Application
import com.noctplayer.app.core.crash.CrashLogger
import com.noctplayer.app.data.local.db.AppDatabase

class NoctPlayerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
