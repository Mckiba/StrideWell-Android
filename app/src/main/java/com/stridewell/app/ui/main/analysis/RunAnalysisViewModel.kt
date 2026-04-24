package com.stridewell.app.ui.main.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.AnalysisApi
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.model.RunAnalysisResponse
import com.stridewell.app.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RunAnalysisViewModel @Inject constructor(
    private val api: AnalysisApi,
    settingsRepository: SettingsRepository
) : ViewModel() {

    sealed interface ScreenState {
        data object Loading : ScreenState
        data class Loaded(val analysis: RunAnalysisResponse) : ScreenState
        data object NotReady : ScreenState  // 404 or computed_at missing
        data class Error(val message: String) : ScreenState
    }

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
        val unitSystem: UnitSystem = UnitSystem.METRIC
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentRunId: String? = null

    init {
        settingsRepository.unitSystem
            .onEach { unit -> _uiState.update { it.copy(unitSystem = unit) } }
            .launchIn(viewModelScope)
    }

    fun load(runId: String) {
        if (currentRunId == runId && _uiState.value.screenState is ScreenState.Loaded) return
        currentRunId = runId
        viewModelScope.launch {
            _uiState.update { it.copy(screenState = ScreenState.Loading) }
            try {
                val response = api.runAnalysis(runId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        _uiState.update { it.copy(screenState = ScreenState.NotReady) }
                    } else {
                        _uiState.update { it.copy(screenState = ScreenState.Loaded(body)) }
                    }
                } else if (response.code() == 404) {
                    _uiState.update { it.copy(screenState = ScreenState.NotReady) }
                } else {
                    _uiState.update {
                        it.copy(screenState = ScreenState.Error(response.message().ifBlank { "Failed to load analysis" }))
                    }
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(screenState = ScreenState.Error("No internet connection. Please check your network."))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(screenState = ScreenState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    fun retry() {
        currentRunId?.let { load(it) }
    }
}
