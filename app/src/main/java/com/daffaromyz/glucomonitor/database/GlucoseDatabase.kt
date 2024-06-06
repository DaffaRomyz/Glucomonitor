package com.daffaromyz.glucomonitor.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Glucose::class], version = 2, exportSchema = false)
@TypeConverters(DataConverter::class)
abstract class GlucoseDatabase : RoomDatabase() {

    abstract fun glucoseDao() : GlucoseDao

    companion object {
        @Volatile
        private var Instance: GlucoseDatabase? = null

        fun getDatabase(context: Context) : GlucoseDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, GlucoseDatabase::class.java, "glucose_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}