package com.stridewell.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlanBuildingViewModel @Inject constructor(
    private val repository: OnboardingRepository
) : ViewModel() {

    data class UiState(
        val isPolling: Boolean = true,
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigateToPlanReveal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToPlanReveal: SharedFlow<Unit> = _navigateToPlanReveal.asSharedFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun retry() {
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _uiState.value = UiState(isPolling = true, errorMessage = null)

            var delayMs = 3_000L
            var elapsedMs = 0L

            while (true) {
                when (val result = repository.status()) {
                    is ApiResult.Success -> {
                        val state = result.data
                        state.conversation_id?.let { repository.saveConversationId(it) }

                        if (state.first_plan_version_id != null) {
                            _navigateToPlanReveal.tryEmit(Unit)
                            return@launch
                        }
                    }
                    is ApiResult.Error -> {
                        if (elapsedMs >= 120_000L) {
                            _uiState.update {
                                it.copy(
                                    isPolling = false,
                                    errorMessage = result.message
                                )
                            }
                            return@launch
                        }
                    }
                }

                if (elapsedMs >= 120_000L) {
                    _uiState.update {
                        it.copy(
                            isPolling = false,
                            errorMessage = "Building your plan is taking longer than expected. Please try again."
                        )
                    }
                    return@launch
                }

                delay(delayMs)
                elapsedMs += delayMs
                delayMs = minOf(delayMs + 3_000L, 15_000L)
            }
        }
    }
}
