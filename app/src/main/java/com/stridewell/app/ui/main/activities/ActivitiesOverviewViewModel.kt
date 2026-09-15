package com.stridewell.app.ui.main.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.RunsRepository
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.Run
import com.stridewell.app.model.RunSummaryBucket
import com.stridewell.app.model.RunSummaryResponse
import com.stridewell.app.util.ActivityRange
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Activities overview: the selected range and period, the period's summary
 * (totals + chart buckets), and the period's paginated run list. Summaries are
 * cached per range+period so switching back is instant.
 */
@HiltViewModel
class ActivitiesOverviewViewModel @Inject constructor(
    private val runsRepository: RunsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
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
        val range: ActivityRange = ActivityRange.WEEK,
        val periodStart: Date = DateUtils.periodStart(ActivityRange.WEEK, Date()),
        // Summary for the selected period, or the previous one while a new one loads.
        val summary: RunSummaryResponse? = null,
        val isLoadingSummary: Boolean = false,
        val runs: List<Run> = emptyList(),
        // Completed/modified plan days keyed by their linked run id.
        val planDaysByRunId: Map<String, PlanDay> = emptyMap(),
        val hasMore: Boolean = false,
        val isLoadingRuns: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isRefreshing: Boolean = false,
    ) {
        /** Identifies the selected range and period. */
        val selectionKey: String
            get() = if (range == ActivityRange.ALL) "all" else "${range.key}-${DateUtils.format(periodStart)}"

        /** Selectable period starts for the dropdown, newest first. */
        val periodStarts: List<Date>
            get() = DateUtils.periodStarts(range, summary?.first_run_date?.let(DateUtils::parse))

        fun sections(today: Date = Date()): List<ActivitySection> =
            buildActivitySections(range, periodStart, runs, today)

        fun clearedRuns(): UiState = copy(
            runs = emptyList(),
            planDaysByRunId = emptyMap(),
            hasMore = false,
            isLoadingRuns = false,
            isLoadingMore = false,
        )
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val summaryCache = mutableMapOf<String, RunSummaryResponse>()
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.unitSystem.collect { system ->
                _uiState.update { it.copy(unitSystem = system) }
            }
        }
        load()
    }

    fun selectRange(range: ActivityRange) {
        if (range == _uiState.value.range) return
        _uiState.update {
            it.copy(range = range, periodStart = DateUtils.periodStart(range, Date())).clearedRuns()
        }
        load()
    }

    fun selectPeriod(start: Date) {
        if (start == _uiState.value.periodStart) return
        _uiState.update { it.copy(periodStart = start).clearedRuns() }
        load()
    }

    fun refresh() {
        summaryCache.remove(_uiState.value.selectionKey)
        _uiState.update { it.copy(isRefreshing = true) }
        load()
    }

    /** Loads the summary (cached when possible) and the first page of runs for the selection. */
    private fun load() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        loadJob = viewModelScope.launch {
            val state = _uiState.value
            val key = state.selectionKey

            val summary = summaryCache[key] ?: run {
                _uiState.update { it.copy(isLoadingSummary = true) }
                val start = if (state.range == ActivityRange.ALL) null else DateUtils.format(state.periodStart)
                val result = runsRepository.summary(state.range.key, start)
                ensureActive()
                when (result) {
                    is ApiResult.Success -> result.data.also { summaryCache[key] = it }
                    is ApiResult.Error -> {
                        _uiState.update {
                            it.copy(
                                screenState = ScreenState.Error(result.message),
                                isLoadingSummary = false,
                                isRefreshing = false,
                            )
                        }
                        return@launch
                    }
                }
            }

            if (summary.first_run_date == null) {
                _uiState.update {
                    it.copy(
                        summary = summary,
                        screenState = ScreenState.Empty,
                        isLoadingSummary = false,
                        isRefreshing = false,
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    summary = summary,
                    screenState = ScreenState.Loaded,
                    isLoadingSummary = false,
                    isLoadingRuns = true,
                )
            }

            val window = listWindow(summary)
            if (window == null) {
                _uiState.update { it.copy(isLoadingRuns = false, isRefreshing = false) }
                return@launch
            }
            val result = runsRepository.runsInRange(window.first, window.second, PAGE_SIZE, 0)
            ensureActive()
            _uiState.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        runs = result.data.runs,
                        planDaysByRunId = indexPlanDays(result.data.plan_days),
                        hasMore = result.data.hasMore == true,
                        isLoadingRuns = false,
                        isRefreshing = false,
                    )
                    is ApiResult.Error -> it.clearedRuns().copy(isRefreshing = false)
                }
            }
        }
    }

    /** Appends the next page. No-op while loading or when no more pages exist. */
    fun loadMore() {
        val state = _uiState.value
        val summary = state.summary ?: return
        if (!state.hasMore || state.isLoadingMore || state.isLoadingRuns) return
        val window = listWindow(summary) ?: return
        val key = state.selectionKey

        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = runsRepository.runsInRange(window.first, window.second, PAGE_SIZE, state.runs.size)
            ensureActive()
            if (_uiState.value.selectionKey != key) return@launch
            _uiState.update {
                when (result) {
                    is ApiResult.Success -> it.copy(
                        runs = it.runs + result.data.runs,
                        planDaysByRunId = it.planDaysByRunId + indexPlanDays(result.data.plan_days),
                        hasMore = result.data.hasMore == true,
                        isLoadingMore = false,
                    )
                    is ApiResult.Error -> it.copy(isLoadingMore = false)
                }
            }
        }
    }

    /** The run behind a single-run chart bucket, from the loaded list or a one-row fetch. */
    suspend fun runForBucket(bucket: RunSummaryBucket): Run? {
        val runId = bucket.run_id?.takeIf { bucket.run_count == 1 } ?: return null
        val state = _uiState.value
        state.runs.firstOrNull { it.id == runId }?.let { return it }

        val range = ActivityRange.fromKey(state.summary?.range) ?: state.range
        val window = DateUtils.bucketWindow(range, bucket.key) ?: return null
        val result = runsRepository.runsInRange(window.first, window.second, 1, 0)
        return (result as? ApiResult.Success)?.data?.runs?.firstOrNull { it.id == runId }
    }

    /** Inclusive first and last day (YYYY-MM-DD) of a summary's [start, end) period. */
    private fun listWindow(summary: RunSummaryResponse): Pair<String, String>? {
        val end = DateUtils.parse(summary.end) ?: return null
        val lastDay = Calendar.getInstance().apply {
            time = end
            add(Calendar.DAY_OF_YEAR, -1)
        }.time
        return summary.start to DateUtils.format(lastDay)
    }

    private fun indexPlanDays(planDays: List<PlanDay>?): Map<String, PlanDay> =
        planDays.orEmpty()
            .mapNotNull { day -> day.runId?.let { it to day } }
            .toMap()
}
