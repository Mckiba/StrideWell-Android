package com.stridewell.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.AuthRepository
import com.stridewell.app.data.TokenStore
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
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed class LaunchState {
        /** Auth check in progress — splash screen stays visible. */
        object Loading : LaunchState()
        /** No token, or token rejected — show WelcomeScreen. */
        object Unauthenticated : LaunchState()
        /** Valid token but onboarding not finished — route to onboarding stack. */
        object NeedsOnboarding : LaunchState()
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
                _state.value = LaunchState.Unauthenticated
                return@launch
            }

            when (val result = authRepository.me()) {
                is ApiResult.Success -> {
                    val status = result.data.onboarding_status
                    val onboardingDone = status == OnboardingStatus.complete ||
                            status == OnboardingStatus.skipped
                    _state.value = if (onboardingDone) {
                        LaunchState.Authenticated
                    } else {
                        LaunchState.NeedsOnboarding
                    }
                }
                is ApiResult.Error -> {
                    // 401 or network error — treat as unauthenticated
                    tokenStore.clearToken()
                    _state.value = LaunchState.Unauthenticated
                }
            }
        }
    }

    /** Called after a successful sign-in to re-run the routing logic. */
    fun onSignedIn(needsOnboarding: Boolean) {
        _state.value = if (needsOnboarding) LaunchState.NeedsOnboarding else LaunchState.Authenticated
    }

    /** Called on sign-out or 401 — resets to unauthenticated. */
    fun onSignedOut() {
        _state.value = LaunchState.Unauthenticated
    }
}
