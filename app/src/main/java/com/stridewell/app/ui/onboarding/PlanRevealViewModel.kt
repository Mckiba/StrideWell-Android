package com.stridewell.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.OnboardingRepository
import com.stridewell.app.data.PlanRepository
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.model.PlanVersionResponse
import com.stridewell.app.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlanRevealViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    sealed interface ScreenState {
        data object Loading : ScreenState
        data class Loaded(val plan: PlanVersionResponse) : ScreenState
        data class Confirming(val plan: PlanVersionResponse) : ScreenState
        data class Error(val message: String) : ScreenState
    }

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
        val confirmError: String? = null,
        val unitSystem: UnitSystem = UnitSystem.METRIC,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigateToMain = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToMain: SharedFlow<Unit> = _navigateToMain.asSharedFlow()

    private var loadedPlan: PlanVersionResponse? = null
    private var loadedPlanVersionId: String? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(unitSystem = settingsRepository.getUnitSystem()) }
            fetchPlan()
        }
    }

    fun retry() {
        viewModelScope.launch { fetchPlan() }
    }

    fun confirmPlan() {
        val planVersionId = loadedPlanVersionId ?: return
        val plan = loadedPlan ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    screenState = ScreenState.Confirming(plan),
                    confirmError = null
                )
            }

            when (val result = onboardingRepository.confirmPlan(planVersionId)) {
                is ApiResult.Success -> {
                    onboardingRepository.markComplete()
                    _navigateToMain.tryEmit(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            screenState = ScreenState.Loaded(plan),
                            confirmError = result.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun fetchPlan() {
        _uiState.update {
            it.copy(
                screenState = ScreenState.Loading,
                confirmError = null
            )
        }

        val unitSystem = settingsRepository.getUnitSystem()
        _uiState.update { it.copy(unitSystem = unitSystem) }

        val onboardingState = when (val result = onboardingRepository.status()) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                _uiState.update { it.copy(screenState = ScreenState.Error(result.message)) }
                return
            }
        }

        val planVersionId = onboardingState.first_plan_version_id
        if (planVersionId.isNullOrBlank()) {
            _uiState.update {
                it.copy(screenState = ScreenState.Error("Plan ID not available. Please go back and try again."))
            }
            return
        }

        when (val result = planRepository.version(planVersionId, weeks = 1)) {
            is ApiResult.Success -> {
                loadedPlanVersionId = planVersionId
                loadedPlan = result.data
                _uiState.update { it.copy(screenState = ScreenState.Loaded(result.data)) }
            }
            is ApiResult.Error -> {
                _uiState.update { it.copy(screenState = ScreenState.Error(result.message)) }
            }
        }
    }
}
