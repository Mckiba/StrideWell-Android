package com.stridewell.app.ui.main.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.ConnectivityRepository
import com.stridewell.app.data.PlanRepository
import com.stridewell.app.data.RunsRepository
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.PlanWeekResponse
import com.stridewell.app.model.Run
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val runsRepository: RunsRepository,
    settingsRepository: SettingsRepository,
    connectivityRepository: ConnectivityRepository
) : ViewModel() {

    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Empty : ScreenState
        data class Error(val message: String) : ScreenState
        data object Loaded : ScreenState
    }

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
        val selectedMonday: Date = DateUtils.mondayOfWeek(containing = Date()),
        val displayedWeek: PlanWeekResponse? = null,
        val weekRuns: List<Run> = emptyList(),
        val selectedDay: PlanDay? = null,
        val unitSystem: UnitSystem = UnitSystem.METRIC,
        val hasPlanChanged: Boolean = false,
        val isRefreshing: Boolean = false,
        val isOffline: Boolean = false
    )

    /** Week keys with a prefetch in flight. Confined to viewModelScope's main
     *  dispatcher, so a plain set is safe. */
    private val inFlightPrefetches = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.unitSystem,
                planRepository.planUpdated,
                planRepository.currentWeek,
                connectivityRepository.isOffline
            ) { unitSystem, hasPlanChanged, refreshedCurrentWeek, isOffline ->
                _uiState.update {
                    val selectedWeekStart = DateUtils.format(it.selectedMonday)
                    val syncedDisplayedWeek = if (
                        refreshedCurrentWeek != null &&
                        refreshedCurrentWeek.start_date == selectedWeekStart
                    ) {
                        refreshedCurrentWeek
                    } else {
                        it.displayedWeek
                    }
                    val syncedScreenState = when {
                        syncedDisplayedWeek == null -> it.screenState
                        syncedDisplayedWeek.days.isEmpty() -> ScreenState.Empty
                        else -> ScreenState.Loaded
                    }
                    it.copy(unitSystem = unitSystem, hasPlanChanged = hasPlanChanged, isOffline = isOffline)
                        .copy(
                            displayedWeek = syncedDisplayedWeek,
                            screenState = syncedScreenState
                        )
                }
            }.collect {}
        }
        loadWeek(DateUtils.mondayOfWeek(containing = Date()))
    }

    fun retry() {
        loadWeek(_uiState.value.selectedMonday, forceRefresh = true, pullToRefresh = false)
    }

    fun pullToRefresh() {
        loadWeek(_uiState.value.selectedMonday, forceRefresh = true, pullToRefresh = true)
    }

    fun previousWeek() {
        loadWeek(DateUtils.previousMonday(_uiState.value.selectedMonday))
    }

    fun nextWeek() {
        loadWeek(DateUtils.nextMonday(_uiState.value.selectedMonday))
    }

    fun selectDay(day: PlanDay) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun dismissSelectedDay() {
        _uiState.update { it.copy(selectedDay = null) }
    }

    /**
     * Paints the cached week immediately so navigation stays instant, then always
     * revalidates against /plan/week. Without the revalidation a week prefetched
     * earlier renders for the rest of the session with stale day statuses.
     */
    private fun loadWeek(
        monday: Date,
        forceRefresh: Boolean = false,
        pullToRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            val startDate = DateUtils.format(monday)
            val cached = planRepository.cachedWeek(startDate)

            _uiState.update {
                when {
                    cached != null && !forceRefresh -> it.copy(
                        selectedMonday = monday,
                        displayedWeek = cached,
                        screenState = if (cached.days.isEmpty()) ScreenState.Empty else ScreenState.Loaded,
                        isRefreshing = pullToRefresh
                    )
                    // Nothing cached: clear rather than leave the previous week's
                    // days rendering under the new date range.
                    cached == null -> it.copy(
                        selectedMonday = monday,
                        displayedWeek = null,
                        weekRuns = emptyList(),
                        screenState = ScreenState.Loading,
                        isRefreshing = pullToRefresh
                    )
                    else -> it.copy(selectedMonday = monday, isRefreshing = pullToRefresh)
                }
            }

            val runsDeferred = async { runsRepository.runsForWeek(monday) }
            try {
                val result = planRepository.week(startDate)
                val weekRuns = (runsDeferred.await() as? ApiResult.Success)?.data?.runs.orEmpty()

                // The user may have navigated on while this was in flight.
                if (!isDisplaying(startDate)) return@launch

                when (result) {
                    is ApiResult.Success -> {
                        planRepository.setWeekData(result.data)
                        _uiState.update {
                            it.copy(
                                displayedWeek = result.data,
                                weekRuns = weekRuns,
                                screenState = if (result.data.days.isEmpty()) ScreenState.Empty else ScreenState.Loaded
                            )
                        }
                        prefetchAdjacentWeeks(monday)
                    }
                    is ApiResult.Error -> {
                        // A failed revalidation must not blank a week already on screen.
                        if (_uiState.value.displayedWeek != null) return@launch

                        if (result.status == 404) {
                            _uiState.update {
                                it.copy(displayedWeek = null, weekRuns = emptyList(), screenState = ScreenState.Empty)
                            }
                        } else {
                            _uiState.update { it.copy(screenState = ScreenState.Error(result.message)) }
                        }
                    }
                }
            } finally {
                if (pullToRefresh) {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    /** True while [startDate] is still the week on screen. */
    private fun isDisplaying(startDate: String): Boolean =
        DateUtils.format(_uiState.value.selectedMonday) == startDate

    private suspend fun prefetchAdjacentWeeks(monday: Date) {
        val prev = DateUtils.format(DateUtils.previousMonday(monday))
        val next = DateUtils.format(DateUtils.nextMonday(monday))
        val prevFetch = viewModelScope.async { prefetchIfNeeded(prev) }
        val nextFetch = viewModelScope.async { prefetchIfNeeded(next) }
        prevFetch.await()
        nextFetch.await()
    }

    private suspend fun prefetchIfNeeded(startDate: String) {
        if (planRepository.cachedWeek(startDate) != null) return
        if (!inFlightPrefetches.add(startDate)) return
        try {
            when (val result = planRepository.week(startDate)) {
                is ApiResult.Success -> planRepository.cacheWeek(result.data)
                is ApiResult.Error -> Unit
            }
        } finally {
            inFlightPrefetches.remove(startDate)
        }
    }
}
