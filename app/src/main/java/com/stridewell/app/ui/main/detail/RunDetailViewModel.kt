package com.stridewell.app.ui.main.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.AnalysisApi
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.RunsRepository
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.model.RunAnalysisResponse
import com.stridewell.app.model.RunDetailResponse
import com.stridewell.app.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response

/**
 * ViewModel backing [RunDetailScreen]. Fetches the full run detail (route +
 * splits + streams) and the V2 analysis in parallel; analysis errors do NOT
 * gate the rest of the screen — a failed/not-ready analysis renders inline in
 * the analysis section while header/splits/stats/charts still display.
 *
 * Mirrors iOS `RunDetailScreen`'s `loadAll()` behaviour.
 */
@HiltViewModel
class RunDetailViewModel @Inject constructor(
    private val runsRepository: RunsRepository,
    private val analysisApi: AnalysisApi,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Top-level state — drives the sheet's loading/error/loaded branches. */
    sealed interface DetailScreenState {
        data object Loading : DetailScreenState
        data class Loaded(
            val detail: RunDetailResponse,
            val analysis: AnalysisState,
        ) : DetailScreenState
        data class Error(val message: String) : DetailScreenState
    }

    /** Analysis sub-state — independent of the detail load. */
    sealed interface AnalysisState {
        data object Loading : AnalysisState
        data class Loaded(val response: RunAnalysisResponse) : AnalysisState
        /** 404 from analysis endpoint — analysis isn't ready yet. */
        data object NotReady : AnalysisState
        /** Non-404 error — the analysis section renders silently empty. */
        data object Error : AnalysisState
    }

    data class UiState(
        val screenState: DetailScreenState = DetailScreenState.Loading,
        val unitSystem: UnitSystem = UnitSystem.METRIC,
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
        if (currentRunId == runId && _uiState.value.screenState is DetailScreenState.Loaded) return
        currentRunId = runId
        _uiState.update { it.copy(screenState = DetailScreenState.Loading) }

        viewModelScope.launch {
            val (detailResult, analysisResponse) = coroutineScope {
                val detail = async { runsRepository.runDetail(runId) }
                val analysis = async { runCatching { analysisApi.runAnalysis(runId) } }
                detail.await() to analysis.await()
            }

            val analysisState = mapAnalysis(analysisResponse)

            _uiState.update { ui ->
                ui.copy(
                    screenState = when (detailResult) {
                        is ApiResult.Success -> DetailScreenState.Loaded(
                            detail = detailResult.data,
                            analysis = analysisState,
                        )
                        is ApiResult.Error -> DetailScreenState.Error(
                            detailResult.message.ifBlank { "Failed to load run." },
                        )
                    },
                )
            }
        }
    }

    fun retry() {
        currentRunId?.let { load(it) }
    }

    private fun mapAnalysis(
        result: Result<Response<RunAnalysisResponse>>,
    ): AnalysisState = mapAnalysisResult(result)
}

/**
 * Top-level helper for [RunDetailViewModel.mapAnalysis] — extracted as `internal`
 * so unit tests can exercise the success / 404 / error / exception branches
 * without standing up the full ViewModel.
 */
internal fun mapAnalysisResult(
    result: Result<Response<RunAnalysisResponse>>,
): RunDetailViewModel.AnalysisState {
    val response = result.getOrNull() ?: return RunDetailViewModel.AnalysisState.Error
    return when {
        response.isSuccessful ->
            response.body()?.let(RunDetailViewModel.AnalysisState::Loaded)
                ?: RunDetailViewModel.AnalysisState.NotReady
        response.code() == 404 -> RunDetailViewModel.AnalysisState.NotReady
        else -> RunDetailViewModel.AnalysisState.Error
    }
}
