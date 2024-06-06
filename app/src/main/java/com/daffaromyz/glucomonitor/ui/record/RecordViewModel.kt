package com.daffaromyz.glucomonitor.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.daffaromyz.glucomonitor.GlucoseApplication
import com.daffaromyz.glucomonitor.database.Glucose
import com.daffaromyz.glucomonitor.database.GlucoseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import kotlinx.coroutines.flow.StateFlow

class RecordViewModel(private val repository: GlucoseRepository) : ViewModel() {


    val homeUiState = repository.getAllStream().map { HomeUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = HomeUiState()
        )
    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
        val Factory : ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GlucoseApplication)
                val glucoseRepository = application.container.glucoseRepository
                RecordViewModel(repository = glucoseRepository)
            }
        }
    }
}

data class HomeUiState(val glucoseList: List<Glucose> = listOf())