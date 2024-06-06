package com.daffaromyz.glucomonitor.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "glucose")
data class Glucose(
    @PrimaryKey(autoGenerate = true) val id : Int = 0,
    val datetime : LocalDateTime = LocalDateTime.now(),
    val value : Int
)