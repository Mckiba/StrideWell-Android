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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    data class UiState(
        val isLoading:    Boolean = false,
        val errorMessage: String? = null,
        /** Non-null after a successful sign-in — true if onboarding is needed. */
        val signedIn:     Boolean? = null,
        val needsOnboarding: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Authenticate
            when (val loginResult = authRepository.login(email.trim(), password)) {
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = loginResult.message) }
                    return@launch
                }
                is ApiResult.Success -> {
                    tokenStore.saveToken(loginResult.data.token)
                }
            }

            // 2. Fetch onboarding status to determine routing
            val needsOnboarding = when (val meResult = authRepository.me()) {
                is ApiResult.Success -> {
                    val status = meResult.data.onboarding_status
                    status != OnboardingStatus.complete && status != OnboardingStatus.skipped
                }
                is ApiResult.Error -> true  // fail-safe: route to onboarding
            }

            _uiState.update {
                it.copy(isLoading = false, signedIn = true, needsOnboarding = needsOnboarding)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
