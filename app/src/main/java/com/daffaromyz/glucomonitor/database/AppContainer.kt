package com.daffaromyz.glucomonitor.database

import android.content.Context

interface AppContainer {
    val glucoseRepository : GlucoseRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val glucoseRepository: GlucoseRepository by lazy {
        DatabaseGlucoseRepository(GlucoseDatabase.getDatabase(context).glucoseDao())
    }
}