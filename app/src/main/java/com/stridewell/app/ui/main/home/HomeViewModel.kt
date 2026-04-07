package com.stridewell.app.ui.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.ActivityRepository
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
    private val settingsRepository: SettingsRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private data class BaseInputs(
        val unitSystem: UnitSystem,
        val today: PlanDay?,
        val week: PlanWeekResponse?,
        val goal: GoalSummary?,
        val planUpdated: Boolean
    )

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
        val nextWeek: PlanWeekResponse? = null,
        val goalSummary: GoalSummary? = null,
        val recentRuns: List<Run> = emptyList(),
        val hasPlanChanged: Boolean = false,
        val latestDecision: DecisionRecord? = null,
        val showActivityBanner: Boolean = false,
        val latestSyncedRunId: String? = null,
        val isOffline: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val baseInputs = combine(
                settingsRepository.unitSystem,
                planRepository.todayPlanDay,
                planRepository.currentWeek,
                planRepository.goalSummary,
                planRepository.planUpdated
            ) { unitSystem, today, week, goal, planUpdated ->
                BaseInputs(
                    unitSystem = unitSystem,
                    today = today,
                    week = week,
                    goal = goal,
                    planUpdated = planUpdated
                )
            }

            combine(
                baseInputs,
                activityRepository.showActivityBanner,
                activityRepository.lastSyncedRunId
            ) { base, showActivityBanner, syncedRunId ->
                val week = base.week
                val nextWeekStart = week?.start_date
                    ?.let(DateUtils::parse)
                    ?.let(DateUtils::nextMonday)
                    ?.let(DateUtils::format)
                val nextWeek = nextWeekStart?.let { planRepository.cachedWeek(it) }
                UiState(
                    screenState = _uiState.value.screenState,
                    unitSystem = base.unitSystem,
                    todayPlanDay = base.today,
                    currentWeek = week,
                    nextWeek = nextWeek,
                    goalSummary = base.goal,
                    recentRuns = _uiState.value.recentRuns,
                    hasPlanChanged = base.planUpdated,
                    latestDecision = _uiState.value.latestDecision,
                    showActivityBanner = showActivityBanner,
                    latestSyncedRunId = syncedRunId,
                    isOffline = _uiState.value.isOffline
                )
            }.collect { derived ->
                _uiState.value = derived
            }
        }

        viewModelScope.launch {
            planRepository.isOffline.collect { offline ->
                _uiState.update { it.copy(isOffline = offline) }
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

    fun dismissActivityBanner() {
        viewModelScope.launch {
            activityRepository.dismissBanner()
        }
    }

    fun dismissPlanChangeBanner() {
        viewModelScope.launch {
            planRepository.markPlanChangeSeen()
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
