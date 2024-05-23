package com.daffaromyz.glucomonitor.database

import kotlinx.coroutines.flow.Flow

interface GlucoseRepository {
    suspend fun insertGlucose(glucose: Glucose)

    suspend fun updateGlucose(glucose: Glucose)

    suspend fun deleteGlucose(glucose: Glucose)

    fun getLastStream(limit : Int) : Flow<List<Glucose>>

    fun getGlucoseStream(id : Int) : Flow<Glucose>

    fun getAllStream() : Flow<List<Glucose>>
}