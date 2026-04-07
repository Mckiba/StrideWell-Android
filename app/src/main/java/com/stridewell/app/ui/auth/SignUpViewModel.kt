package com.stridewell.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.ActivityRepository
import com.stridewell.app.data.AuthRepository
import com.stridewell.app.data.ChatRepository
import com.stridewell.app.data.PlanRepository
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
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore,
    private val planRepository: PlanRepository,
    private val chatRepository: ChatRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    data class UiState(
        val isLoading:       Boolean = false,
        val errorMessage:    String? = null,
        /** Non-null after successful registration. */
        val registeredWith:  Boolean? = null,
        val onboardingStatus: OnboardingStatus? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isEmpty()) return

        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords don't match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Register
            when (val result = authRepository.register(email.trim(), password)) {
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    return@launch
                }
                is ApiResult.Success -> {
                    planRepository.clearInMemoryState(clearSeenVersion = true)
                    chatRepository.clearInMemoryState(clearPersistedConversationId = true)
                    activityRepository.reset()
                    tokenStore.saveToken(result.data.token)
                }
            }

            // 2. Fetch onboarding status — new users always need onboarding,
            //    but we confirm via the API to stay in sync with the backend.
            val onboardingStatus = when (val meResult = authRepository.me()) {
                is ApiResult.Success -> {
                    meResult.data.onboarding_status ?: OnboardingStatus.pending
                }
                is ApiResult.Error -> OnboardingStatus.pending
            }

            _uiState.update {
                it.copy(isLoading = false, registeredWith = true, onboardingStatus = onboardingStatus)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
