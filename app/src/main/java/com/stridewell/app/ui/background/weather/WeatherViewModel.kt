package com.stridewell.app.ui.background.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.data.LocationRepository
import com.stridewell.app.data.WeatherRepository
import com.stridewell.app.model.StormCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val condition: StormCondition = StormCondition.CLEAR,
    val debugCondition: StormCondition? = null
) {
    val activeCondition: StormCondition
        get() = debugCondition ?: condition
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var lastFetchAtMs: Long? = null

    fun setDebugCondition(condition: StormCondition?) {
        _uiState.update { it.copy(debugCondition = condition) }
    }

    fun fetchIfNeeded(hasLocationPermission: Boolean) {
        val last = lastFetchAtMs
        if (last != null && System.currentTimeMillis() - last < REFRESH_INTERVAL_MS) return

        viewModelScope.launch {
            val coordinate = locationRepository.requestLocation(hasLocationPermission) ?: return@launch
            val condition = weatherRepository.fetchCondition(coordinate) ?: StormCondition.CLEAR
            lastFetchAtMs = System.currentTimeMillis()
            _uiState.update { it.copy(condition = condition) }
        }
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 15 * 60 * 1000L
    }
}
