package com.daffaromyz.glucomonitor.database

import kotlinx.coroutines.flow.Flow

class DatabaseGlucoseRepository(private val dao: GlucoseDao) : GlucoseRepository {
    override suspend fun insertGlucose(glucose: Glucose) {
        dao.insert(glucose)
    }

    override suspend fun updateGlucose(glucose: Glucose) {
        dao.update(glucose)
    }

    override suspend fun deleteGlucose(glucose: Glucose) {
        dao.delete(glucose)
    }

    override fun getLastStream(limit: Int): Flow<List<Glucose>> {
        return dao.getLast(limit)
    }

    override fun getGlucoseStream(id: Int): Flow<Glucose> {
        return dao.getGlucose(id)
    }

    override fun getAllStream(): Flow<List<Glucose>> {
        return dao.getAll()
    }

}