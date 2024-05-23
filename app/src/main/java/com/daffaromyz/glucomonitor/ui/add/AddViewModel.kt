package com.daffaromyz.glucomonitor.ui.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.daffaromyz.glucomonitor.database.Glucose
import com.daffaromyz.glucomonitor.database.GlucoseDao
import com.daffaromyz.glucomonitor.database.GlucoseDatabase

data class UiState (
    val glucoseValue:Int? = null,
    val isUnitMg:Boolean? = null
)
class AddViewModel() : ViewModel() {

    private val _valueText = MutableLiveData<String>().apply {
        value = "000"
    }
    val valueText: LiveData<String> = _valueText
}