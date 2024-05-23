package com.daffaromyz.glucomonitor.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(glucose: Glucose)

    @Update
    suspend fun update(glucose: Glucose)

    @Delete
    suspend fun delete(glucose: Glucose)

    @Query("SELECT * FROM glucose WHERE id = :id")
    fun getGlucose(id : Int) : Flow<Glucose>

    @Query("SELECT * FROM glucose ORDER BY id DESC LIMIT :limit")
    fun getLast(limit : Int) : Flow<List<Glucose>>

    @Query("SELECT * FROM glucose ORDER BY id DESC")
    fun getAll() : Flow<List<Glucose>>
}