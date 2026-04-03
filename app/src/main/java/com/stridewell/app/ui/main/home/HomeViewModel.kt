package com.stridewell.app.ui.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.PlanRepository
import com.stridewell.app.data.RunsRepository
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.model.DecisionRecord
import com.stridewell.app.model.GoalSummary
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.PlanWeekResponse
import com.stridewell.app.model.Run
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val runsRepository: RunsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Empty : ScreenState
        data class Error(val message: String) : ScreenState
        data object Loaded : ScreenState
    }

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
        val unitSystem: UnitSystem = UnitSystem.METRIC,
        val todayPlanDay: PlanDay? = null,
        val currentWeek: PlanWeekResponse? = null,
        val goalSummary: GoalSummary? = null,
        val recentRuns: List<Run> = emptyList(),
        val hasPlanChanged: Boolean = false,
        val latestDecision: DecisionRecord? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.unitSystem,
                planRepository.todayPlanDay,
                planRepository.currentWeek,
                planRepository.goalSummary,
                planRepository.planUpdated
            ) { unitSystem, today, week, goal, planUpdated ->
                UiState(
                    screenState = _uiState.value.screenState,
                    unitSystem = unitSystem,
                    todayPlanDay = today,
                    currentWeek = week,
                    goalSummary = goal,
                    recentRuns = _uiState.value.recentRuns,
                    hasPlanChanged = planUpdated,
                    latestDecision = _uiState.value.latestDecision
                )
            }.collect { derived ->
                _uiState.value = derived
            }
        }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(screenState = ScreenState.Loading) }
            loadData()
        }
    }

    private suspend fun loadData() {
        val currentWeekStart = DateUtils.mondayString(containing = java.util.Date())

        val todayDeferred = viewModelScope.async { planRepository.today() }
        val weekDeferred = viewModelScope.async { planRepository.week(currentWeekStart) }
        val goalDeferred = viewModelScope.async { planRepository.goalSummary() }
        val runsDeferred = viewModelScope.async { runsRepository.recent(limit = 3) }
        val decisionDeferred = viewModelScope.async { planRepository.latestDecision() }

        when (val today = todayDeferred.await()) {
            is ApiResult.Success -> {
                planRepository.setTodayPlanDay(today.data)
            }
            is ApiResult.Error -> {
                if (today.status == 404) {
                    _uiState.update { it.copy(screenState = ScreenState.Empty) }
                    return
                }
                _uiState.update { it.copy(screenState = ScreenState.Error(today.message)) }
                return
            }
        }

        when (val week = weekDeferred.await()) {
            is ApiResult.Success -> planRepository.setWeekData(week.data)
            is ApiResult.Error -> Unit
        }

        when (val goal = goalDeferred.await()) {
            is ApiResult.Success -> planRepository.setGoalSummary(goal.data)
            is ApiResult.Error -> if (goal.status == 404) {
                planRepository.setGoalSummary(null)
            }
        }

        when (val runs = runsDeferred.await()) {
            is ApiResult.Success -> _uiState.update { it.copy(recentRuns = runs.data.runs) }
            is ApiResult.Error -> _uiState.update { it.copy(recentRuns = emptyList()) }
        }

        when (val latestDecision = decisionDeferred.await()) {
            is ApiResult.Success -> _uiState.update { it.copy(latestDecision = latestDecision.data.decision_record) }
            is ApiResult.Error -> _uiState.update { it.copy(latestDecision = null) }
        }

        _uiState.update { it.copy(screenState = ScreenState.Loaded) }
    }
}
