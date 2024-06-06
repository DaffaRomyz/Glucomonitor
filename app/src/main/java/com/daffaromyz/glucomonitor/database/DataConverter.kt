package com.daffaromyz.glucomonitor.database

import androidx.room.TypeConverter
import java.time.LocalDateTime

class DataConverter {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.toString()
    }
}