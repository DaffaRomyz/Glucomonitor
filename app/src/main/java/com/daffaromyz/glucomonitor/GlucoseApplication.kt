package com.daffaromyz.glucomonitor

import android.app.Application
import com.daffaromyz.glucomonitor.database.AppContainer
import com.daffaromyz.glucomonitor.database.AppDataContainer

class GlucoseApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}