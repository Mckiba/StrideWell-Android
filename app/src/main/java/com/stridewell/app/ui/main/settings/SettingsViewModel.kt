package com.stridewell.app.ui.main.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.StravaApi
import com.stridewell.app.data.ActivityRepository
import com.stridewell.app.data.AuthRepository
import com.stridewell.app.data.ChatRepository
import com.stridewell.app.data.OnboardingRepository
import com.stridewell.app.data.PlanRepository
import com.stridewell.app.data.RunsRepository
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.data.TokenStore
import com.stridewell.app.model.StravaStatusResponse
import com.stridewell.app.model.goalName
import com.stridewell.app.util.AppTheme
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.StravaOAuthHelper
import com.stridewell.app.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val onboardingRepository: OnboardingRepository,
    private val planRepository: PlanRepository,
    private val chatRepository: ChatRepository,
    private val runsRepository: RunsRepository,
    private val activityRepository: ActivityRepository,
    private val tokenStore: TokenStore,
    private val stravaApi: StravaApi,
    private val unauthorizedFlow: MutableSharedFlow<Unit>,
    @Named("oauthCode") private val oauthCodeFlow: MutableStateFlow<String?>
) : ViewModel() {

    // ── State classes ─────────────────────────────────────────────────────────

    sealed class StravaState {
        object Loading       : StravaState()
        object Disconnected  : StravaState()
        data class Connected(val expiresAt: String?, val scope: String?) : StravaState()
        data class Expired(val expiresAt: String, val scope: String?)    : StravaState()
        object Connecting    : StravaState()
        object Disconnecting : StravaState()
        data class Error(val message: String) : StravaState()
    }

    sealed class DeleteState {
        object Idle     : DeleteState()
        object Deleting : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    data class UiState(
        val stravaState:             StravaState = StravaState.Loading,
        val unitSystem:              UnitSystem  = UnitSystem.METRIC,
        val appTheme:                AppTheme    = AppTheme.DEVICE,
        val reflectionReminders:     Boolean     = true,
        val planUpdateAlerts:        Boolean     = true,
        val goalName:                String?     = null,
        val deleteState:             DeleteState = DeleteState.Idle,
        val showDisconnectDialog:    Boolean     = false,
        val showDeleteDialog:        Boolean     = false,
        val showDeleteConfirmDialog: Boolean     = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch { loadStravaStatus() }

        viewModelScope.launch {
            combine(
                settingsRepository.unitSystem,
                settingsRepository.appTheme,
                settingsRepository.reflectionReminders,
                settingsRepository.planUpdateAlerts
            ) { unit, theme, reminders, alerts ->
                _uiState.update {
                    it.copy(
                        unitSystem          = unit,
                        appTheme            = theme,
                        reflectionReminders = reminders,
                        planUpdateAlerts    = alerts
                    )
                }
            }.collect {}
        }

        // Read goal name from already-cached plan data — no extra API call
        _uiState.update { it.copy(goalName = planRepository.goalSummary.value?.goalName()) }

        // Consume OAuth code forwarded from MainActivity (same pattern as StravaConnectViewModel)
        viewModelScope.launch {
            oauthCodeFlow.collect { code ->
                if (code != null) {
                    oauthCodeFlow.value = null
                    exchangeStravaCode(code)
                }
            }
        }
    }

    // ── Strava ────────────────────────────────────────────────────────────────

    private suspend fun loadStravaStatus() {
        _uiState.update { it.copy(stravaState = StravaState.Loading) }
        try {
            val response = stravaApi.stravaStatus()
            if (response.isSuccessful) {
                val body = response.body()
                _uiState.update {
                    it.copy(stravaState = if (body != null) resolveStravaState(body) else StravaState.Error("Empty response"))
                }
            } else {
                _uiState.update { it.copy(stravaState = StravaState.Error(response.message())) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(stravaState = StravaState.Error(e.message ?: "Unknown error")) }
        }
    }

    private fun resolveStravaState(body: StravaStatusResponse): StravaState {
        if (!body.connected) return StravaState.Disconnected
        val expiresAt = body.expires_at
        return if (expiresAt != null && isExpired(expiresAt)) {
            StravaState.Expired(expiresAt, body.scope)
        } else {
            StravaState.Connected(expiresAt, body.scope)
        }
    }

    private fun isExpired(expiresAtString: String): Boolean {
        val parsed = DateUtils.parse(expiresAtString) ?: return false
        return parsed.before(Date())
    }

    fun onConnectClicked(context: Context) {
        _uiState.update { it.copy(stravaState = StravaState.Connecting) }
        StravaOAuthHelper.launch(context)
    }

    private suspend fun exchangeStravaCode(code: String) {
        when (val result = onboardingRepository.stravaConnect(code)) {
            is ApiResult.Success -> loadStravaStatus()
            is ApiResult.Error   -> _uiState.update { it.copy(stravaState = StravaState.Error(result.message)) }
        }
    }

    fun onDisconnectTapped()    { _uiState.update { it.copy(showDisconnectDialog = true) } }
    fun onDisconnectDismissed() { _uiState.update { it.copy(showDisconnectDialog = false) } }

    fun onDisconnectConfirmed() {
        _uiState.update { it.copy(showDisconnectDialog = false, stravaState = StravaState.Disconnecting) }
        viewModelScope.launch {
            try {
                val response = stravaApi.disconnect()
                if (response.isSuccessful) {
                    _uiState.update { it.copy(stravaState = StravaState.Disconnected) }
                } else {
                    _uiState.update { it.copy(stravaState = StravaState.Error(response.message())) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(stravaState = StravaState.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    fun onRetryStravaStatus() { viewModelScope.launch { loadStravaStatus() } }

    // ── Preferences ───────────────────────────────────────────────────────────

    fun onUnitSystemChanged(unitSystem: UnitSystem) {
        viewModelScope.launch { settingsRepository.setUnitSystem(unitSystem) }
    }

    fun onAppThemeChanged(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setAppTheme(theme) }
    }

    fun onReflectionRemindersChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReflectionReminders(enabled) }
    }

    fun onPlanUpdateAlertsChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPlanUpdateAlerts(enabled) }
    }

    // ── Sign out ──────────────────────────────────────────────────────────────

    fun onSignOut() {
        viewModelScope.launch {
            tokenStore.clearToken()
            listOf(
                async { settingsRepository.reset() },
                async { onboardingRepository.reset() },
                async { planRepository.reset() },
                async { chatRepository.reset() },
                async { runsRepository.reset() },
                async { activityRepository.reset() }
            ).awaitAll()
            unauthorizedFlow.tryEmit(Unit)
        }
    }

    // ── Delete account ────────────────────────────────────────────────────────

    fun onDeleteAccountTapped()  { _uiState.update { it.copy(showDeleteDialog = true) } }
    fun onDeleteDialogDismissed() { _uiState.update { it.copy(showDeleteDialog = false) } }

    fun onDeleteFirstConfirmed() {
        _uiState.update { it.copy(showDeleteDialog = false, showDeleteConfirmDialog = true) }
    }

    fun onDeleteConfirmDismissed() { _uiState.update { it.copy(showDeleteConfirmDialog = false) } }

    fun onDeleteConfirmed() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false, deleteState = DeleteState.Deleting) }
        viewModelScope.launch {
            when (val result = authRepository.deleteAccount()) {
                is ApiResult.Success -> {
                    tokenStore.clearToken()
                    listOf(
                        async { settingsRepository.reset() },
                        async { onboardingRepository.reset() },
                        async { planRepository.reset() },
                        async { chatRepository.reset() },
                        async { activityRepository.reset() }
                    ).awaitAll()
                    unauthorizedFlow.tryEmit(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(deleteState = DeleteState.Error(result.message)) }
                }
            }
        }
    }

    fun onDeleteErrorDismissed() { _uiState.update { it.copy(deleteState = DeleteState.Idle) } }
}
