package com.stridewell.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.AuthRepository
import com.stridewell.app.data.ChatRepository
import com.stridewell.app.data.OnboardingRepository
import com.stridewell.app.data.PlanRepository
import com.stridewell.app.data.RunsRepository
import com.stridewell.app.data.TokenStore
import com.stridewell.app.navigation.Route
import com.stridewell.app.model.OnboardingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val authRepository: AuthRepository,
    private val onboardingRepository: OnboardingRepository,
    private val planRepository: PlanRepository,
    private val chatRepository: ChatRepository,
    private val runsRepository: RunsRepository
) : ViewModel() {

    sealed class LaunchState {
        /** Auth check in progress — splash screen stays visible. */
        object Loading : LaunchState()
        /** No token, or token rejected — show WelcomeScreen. */
        object Unauthenticated : LaunchState()
        /** Valid token but onboarding not finished — route based on exact onboarding status. */
        data class NeedsOnboarding(val route: String) : LaunchState()
        /** Valid token and onboarding complete — route to main tab bar. */
        object Authenticated : LaunchState()
    }

    private val _state = MutableStateFlow<LaunchState>(LaunchState.Loading)
    val state: StateFlow<LaunchState> = _state.asStateFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            if (tokenStore.getToken() == null) {
                planRepository.clearInMemoryState(clearSeenVersion = true)
                chatRepository.clearInMemoryState(clearPersistedConversationId = true)
                runsRepository.reset()
                _state.value = LaunchState.Unauthenticated
                return@launch
            }

            when (val result = authRepository.me()) {
                is ApiResult.Success -> {
                    val status = result.data.onboarding_status ?: OnboardingStatus.pending
                    val onboardingDone = status == OnboardingStatus.complete ||
                            status == OnboardingStatus.skipped
                    _state.value = if (onboardingDone) {
                        LaunchState.Authenticated
                    } else {
                        LaunchState.NeedsOnboarding(resolveOnboardingRoute(status))
                    }
                }
                is ApiResult.Error -> {
                    if (result.status == 0) {
                        // Network error — token is still valid, route from persisted state
                        val onboardingDone = onboardingRepository.isOnboardingComplete()
                        _state.value = if (onboardingDone) {
                            LaunchState.Authenticated
                        } else {
                            LaunchState.NeedsOnboarding(Route.IntakeInterview.path)
                        }
                    } else {
                        // 401 or other auth error — token rejected, force sign-in
                        tokenStore.clearToken()
                        planRepository.clearInMemoryState(clearSeenVersion = true)
                        chatRepository.clearInMemoryState(clearPersistedConversationId = true)
                        runsRepository.reset()
                        _state.value = LaunchState.Unauthenticated
                    }
                }
            }
        }
    }

    /** Called after a successful sign-in to re-run the routing logic. */
    fun onSignedIn(status: OnboardingStatus?) {
        val resolvedStatus = status ?: OnboardingStatus.pending
        _state.value = if (
            resolvedStatus == OnboardingStatus.complete ||
            resolvedStatus == OnboardingStatus.skipped
        ) {
            LaunchState.Authenticated
        } else {
            LaunchState.NeedsOnboarding(Route.forOnboardingStatus(resolvedStatus))
        }
    }

    /** Called on sign-out or 401 — resets to unauthenticated. */
    fun onSignedOut() {
        _state.value = LaunchState.Unauthenticated
    }

    private suspend fun resolveOnboardingRoute(status: OnboardingStatus): String {
        if (status != OnboardingStatus.interview) {
            return Route.forOnboardingStatus(status)
        }

        return when (val result = onboardingRepository.status()) {
            is ApiResult.Success -> when {
                result.data.first_plan_version_id != null -> Route.PlanReveal.path
                result.data.intake_complete -> Route.PlanBuilding.path
                else -> Route.IntakeInterview.path
            }
            is ApiResult.Error -> Route.IntakeInterview.path
        }
    }
}
