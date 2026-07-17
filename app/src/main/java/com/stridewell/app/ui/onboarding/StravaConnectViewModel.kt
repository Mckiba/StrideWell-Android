package com.stridewell.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.OnboardingRepository
import com.stridewell.app.data.OnboardingSessionStore
import com.stridewell.app.data.TokenStore
import com.stridewell.app.model.OnboardingState
import com.stridewell.app.model.OnboardingStatus
import com.stridewell.app.navigation.OnboardingFlow
import com.stridewell.app.navigation.Route
import com.stridewell.app.util.Polling
import com.stridewell.app.util.StravaOAuthHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * Connect screen. Offers three choices — connect Strava, continue without it, or skip
 * onboarding — and, on resume, advances to the first unsatisfied guided screen once a
 * data-connection decision exists.
 */
@HiltViewModel
class StravaConnectViewModel @Inject constructor(
    private val repository: OnboardingRepository,
    private val sessionStore: OnboardingSessionStore,
    private val tokenStore: TokenStore,
    private val unauthorizedFlow: MutableSharedFlow<Unit>,
    @Named("oauthCode") private val oauthCodeFlow: MutableStateFlow<String?>
) : ViewModel() {

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

    /** Route to navigate to next, or null. Consumed by the screen via [onNavConsumed]. */
    private val _navTarget = MutableStateFlow<String?>(null)
    val navTarget: StateFlow<String?> = _navTarget.asStateFlow()

    init {
        viewModelScope.launch { startOnboardingSession() }
        viewModelScope.launch {
            oauthCodeFlow.collect { code ->
                if (code != null) {
                    oauthCodeFlow.value = null
                    exchangeStravaCode(code)
                }
            }
        }
    }

    // ── Session ─────────────────────────────────────────────────────────────

    private suspend fun startOnboardingSession() {
        _screenState.value = ScreenState.Starting
        when (val result = repository.start()) {
            is ApiResult.Success -> {
                val response = result.data
                repository.saveConversationId(response.conversation_id)
                sessionStore.setConversationId(response.conversation_id)
                sessionStore.setStravaConnected(response.strava_connected)
                _screenState.value = if (response.strava_connected) ScreenState.Connected else ScreenState.Idle
            }
            is ApiResult.Error -> {
                if (result.status == 409) resumeExistingSession()
                else _screenState.value = ScreenState.SessionError(result.message)
            }
        }
    }

    private suspend fun resumeExistingSession() {
        when (val result = repository.status()) {
            is ApiResult.Success -> {
                val state = result.data
                state.conversation_id?.let { repository.saveConversationId(it) }
                sessionStore.update(state)
                when (state.status) {
                    OnboardingStatus.interview -> {
                        val decided = repository.hasDecidedConnection() || state.strava_connected
                        _screenState.value = if (state.strava_connected) ScreenState.Connected else ScreenState.Idle
                        if (decided) advanceIntoInterview(state)
                    }
                    OnboardingStatus.analyzing -> {
                        _screenState.value = ScreenState.Analyzing
                        pollUntilInterview()
                    }
                    OnboardingStatus.pending -> _screenState.value = ScreenState.Idle
                    OnboardingStatus.complete,
                    OnboardingStatus.skipped -> Unit
                }
            }
            is ApiResult.Error -> _screenState.value = ScreenState.SessionError(result.message)
        }
    }

    // ── OAuth ───────────────────────────────────────────────────────────────

    fun onConnectClicked(context: Context) {
        _screenState.value = ScreenState.Connecting
        StravaOAuthHelper.launch(context)
    }

    private suspend fun exchangeStravaCode(code: String) {
        when (val result = repository.stravaConnect(code)) {
            is ApiResult.Success -> {
                repository.markConnectionDecided()
                sessionStore.setStravaConnected(true)
                _screenState.value = ScreenState.Analyzing
                pollUntilInterview()
            }
            is ApiResult.Error -> _screenState.value = ScreenState.OAuthError(result.message)
        }
    }

    private suspend fun pollUntilInterview() {
        var attempts = 0
        Polling.exponentialBackoff {
            attempts += 1
            if (attempts > 10) {
                _screenState.value = ScreenState.SessionError(
                    "Analysis is taking longer than expected. Tap 'Try again' to retry."
                )
                return@exponentialBackoff true
            }
            when (val result = repository.status()) {
                is ApiResult.Success -> {
                    if (result.data.status == OnboardingStatus.interview) {
                        sessionStore.update(result.data)
                        advanceIntoInterview(result.data)
                        true
                    } else false
                }
                is ApiResult.Error -> false
            }
        }
    }

    // ── Choices ─────────────────────────────────────────────────────────────

    /** Continue without Strava: record the decision and advance into the manual branch. */
    fun onContinueWithoutStrava() {
        viewModelScope.launch {
            repository.markConnectionDecided()
            hydrateAndAdvance()
        }
    }

    /** Post-connect "Continue" affordance. */
    fun onContinueForward() {
        viewModelScope.launch { hydrateAndAdvance() }
    }

    /** Skip onboarding entirely — the backend builds a default plan. */
    fun onSkipOnboarding() {
        viewModelScope.launch {
            when (repository.skip()) {
                is ApiResult.Success -> _navTarget.value = Route.PlanBuilding.path
                is ApiResult.Error -> Unit
            }
        }
    }

    fun onRetrySession() {
        viewModelScope.launch { startOnboardingSession() }
    }

    fun onNavConsumed() {
        _navTarget.value = null
    }

    fun onSignOut() {
        viewModelScope.launch {
            sessionStore.reset()
            tokenStore.clearToken()
            unauthorizedFlow.tryEmit(Unit)
        }
    }

    // ── Advancement ─────────────────────────────────────────────────────────

    /** Fetch the latest status (for history_summary + confirmed_fields), then advance. */
    private suspend fun hydrateAndAdvance() {
        when (val result = repository.status()) {
            is ApiResult.Success -> {
                sessionStore.update(result.data)
                advanceIntoInterview(result.data)
            }
            is ApiResult.Error -> advanceIntoInterview(currentState())
        }
    }

    private fun advanceIntoInterview(state: OnboardingState) {
        val branch = OnboardingFlow.baselineBranch(state.strava_connected, state.history_summary)
        val next = OnboardingFlow.firstUnsatisfied(branch, state.confirmed_fields ?: emptyList())
        _navTarget.value = next?.let { OnboardingFlow.route(it) } ?: Route.PlanBuilding.path
    }

    private fun currentState(): OnboardingState = OnboardingState(
        status = OnboardingStatus.interview,
        strava_connected = sessionStore.stravaConnected.value,
        intake_complete = false,
        first_plan_version_id = null,
        conversation_id = sessionStore.conversationId.value,
        history_summary = sessionStore.historySummary.value,
        confirmed_fields = sessionStore.confirmedFields.value,
        partial_intake = sessionStore.partialIntake.value
    )
}
