package com.stridewell.app.ui.main.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.RunsRepository
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.model.Run
import com.stridewell.app.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    private val runsRepository: RunsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 350L
    }

    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Empty : ScreenState
        data class Error(val message: String) : ScreenState
        data object Loaded : ScreenState
    }

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
        val unitSystem: UnitSystem = UnitSystem.METRIC,
        val runs: List<Run> = emptyList(),
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val searchQuery: String = "",
        val selectedDate: String? = null   // YYYY-MM-DD, null = no filter
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.unitSystem.collect { system ->
                _uiState.update { it.copy(unitSystem = system) }
            }
        }
        refresh()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            refresh()
        }
    }

    fun onDateSelected(date: String?) {
        _uiState.update { it.copy(selectedDate = date) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val firstLoad = _uiState.value.screenState == ScreenState.Loading
            if (!firstLoad) {
                _uiState.update { it.copy(isRefreshing = true) }
            }
            val state = _uiState.value
            when (val result = runsRepository.recent(
                limit = PAGE_SIZE,
                offset = 0,
                search = state.searchQuery.ifBlank { null },
                date = state.selectedDate
            )) {
                is ApiResult.Success -> {
                    val runs = result.data.runs
                    _uiState.update {
                        it.copy(
                            screenState = if (runs.isEmpty()) ScreenState.Empty else ScreenState.Loaded,
                            runs = runs,
                            hasMore = result.data.hasMore == true,
                            isRefreshing = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            screenState = ScreenState.Error(result.message),
                            isRefreshing = false
                        )
                    }
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.screenState != ScreenState.Loaded) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = runsRepository.recent(
                limit = PAGE_SIZE,
                offset = state.runs.size,
                search = state.searchQuery.ifBlank { null },
                date = state.selectedDate
            )) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            runs = it.runs + result.data.runs,
                            hasMore = result.data.hasMore == true,
                            isLoadingMore = false
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}
