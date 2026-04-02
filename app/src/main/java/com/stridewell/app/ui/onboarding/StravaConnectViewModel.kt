package com.stridewell.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.OnboardingRepository
import com.stridewell.app.model.OnboardingStatus
import com.stridewell.app.util.Polling
import com.stridewell.app.util.StravaOAuthHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class StravaConnectViewModel @Inject constructor(
    private val repository: OnboardingRepository,
    @Named("oauthCode") private val oauthCodeFlow: MutableStateFlow<String?>
) : ViewModel() {

    // ── UI state ──────────────────────────────────────────────────────────────

    sealed class ScreenState {
        object Starting      : ScreenState()
        object Idle          : ScreenState()
        object Connecting    : ScreenState()
        object Analyzing     : ScreenState()
        object Connected     : ScreenState()
        data class SessionError(val message: String) : ScreenState()
        data class OAuthError(val message: String)   : ScreenState()
    }

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Starting)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    /** Single-shot event: true when the screen should navigate to IntakeInterview. */
    private val _navigateToInterview = MutableStateFlow(false)
    val navigateToInterview: StateFlow<Boolean> = _navigateToInterview.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch { startOnboardingSession() }

        // Collect OAuth code forwarded from MainActivity's deep link handler
        viewModelScope.launch {
            oauthCodeFlow.collect { code ->
                if (code != null) {
                    oauthCodeFlow.value = null          // consume before processing
                    exchangeStravaCode(code)
                }
            }
        }
    }

    // ── Session ───────────────────────────────────────────────────────────────

    private suspend fun startOnboardingSession() {
        _screenState.value = ScreenState.Starting
        when (val result = repository.start()) {
            is ApiResult.Success -> {
                val response = result.data
                _screenState.value = if (response.strava_connected) {
                    ScreenState.Connected
                } else {
                    ScreenState.Idle
                }
            }
            is ApiResult.Error -> {
                if (result.status == 409) {
                    // Session already exists — resume it
                    resumeExistingSession()
                } else {
                    _screenState.value = ScreenState.SessionError(result.message)
                }
            }
        }
    }

    private suspend fun resumeExistingSession() {
        when (val result = repository.status()) {
            is ApiResult.Success -> {
                val state = result.data
                when (state.status) {
                    OnboardingStatus.interview -> _navigateToInterview.value = true
                    OnboardingStatus.analyzing -> {
                        _screenState.value = ScreenState.Analyzing
                        pollUntilInterview()
                    }
                    OnboardingStatus.pending -> _screenState.value = ScreenState.Idle
                    OnboardingStatus.complete,
                    OnboardingStatus.skipped  -> Unit   // LaunchViewModel handles this
                }
            }
            is ApiResult.Error -> {
                _screenState.value = ScreenState.SessionError(result.message)
            }
        }
    }

    // ── OAuth ─────────────────────────────────────────────────────────────────

    /**
     * Opens the Strava OAuth flow in a Chrome Custom Tab.
     * The context must be an Activity context — passed from the composable
     * at the time of the click (never stored in the ViewModel).
     */
    fun onConnectClicked(context: Context) {
        _screenState.value = ScreenState.Connecting
        StravaOAuthHelper.launch(context)
    }

    private suspend fun exchangeStravaCode(code: String) {
        when (val result = repository.stravaConnect(code)) {
            is ApiResult.Success -> {
                _screenState.value = ScreenState.Analyzing
                pollUntilInterview()
            }
            is ApiResult.Error -> {
                _screenState.value = ScreenState.OAuthError(result.message)
            }
        }
    }

    private suspend fun pollUntilInterview() {
        Polling.exponentialBackoff {
            val result = repository.status()
            if (result is ApiResult.Success && result.data.status == OnboardingStatus.interview) {
                _navigateToInterview.value = true
                true
            } else {
                false
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun onSkip() {
        _navigateToInterview.value = true
    }

    fun onContinue() {
        _navigateToInterview.value = true
    }

    fun onRetrySession() {
        viewModelScope.launch { startOnboardingSession() }
    }

    /** Called by the screen after navigation so the event isn't re-triggered. */
    fun onNavigatedToInterview() {
        _navigateToInterview.value = false
    }
}
