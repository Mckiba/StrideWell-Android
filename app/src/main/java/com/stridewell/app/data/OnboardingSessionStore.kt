package com.stridewell.app.data

import com.stridewell.app.model.OnboardingState
import com.stridewell.app.model.PartialIntake
import com.stridewell.app.model.StravaHistorySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory session state shared across the guided onboarding screens (each has its own
 * ViewModel). `confirmedFields` drives which screen shows next; `partialIntake` pre-fills the
 * structured controls when returning to a screen.
 *
 * The persisted data-connection decision flag lives in [OnboardingRepository], not here —
 * this store is ephemeral working state.
 */
@Singleton
class OnboardingSessionStore @Inject constructor() {

    private val _confirmedFields = MutableStateFlow<List<String>>(emptyList())
    val confirmedFields: StateFlow<List<String>> = _confirmedFields.asStateFlow()

    private val _partialIntake = MutableStateFlow<PartialIntake?>(null)
    val partialIntake: StateFlow<PartialIntake?> = _partialIntake.asStateFlow()

    private val _historySummary = MutableStateFlow<StravaHistorySummary?>(null)
    val historySummary: StateFlow<StravaHistorySummary?> = _historySummary.asStateFlow()

    private val _stravaConnected = MutableStateFlow(false)
    val stravaConnected: StateFlow<Boolean> = _stravaConnected.asStateFlow()

    private val _conversationId = MutableStateFlow<String?>(null)
    val conversationId: StateFlow<String?> = _conversationId.asStateFlow()

    /** Hydrate from a `GET /onboarding/status` (or start) response. Only overwrites the
     *  V2 fields when present, so a response that omits them leaves them as-is. */
    fun update(from: OnboardingState) {
        _stravaConnected.value = from.strava_connected
        from.conversation_id?.let { _conversationId.value = it }
        from.confirmed_fields?.let { _confirmedFields.value = it }
        from.partial_intake?.let { _partialIntake.value = it }
        from.history_summary?.let { _historySummary.value = it }
    }

    /** Store the confirmed fields returned by a message reply. */
    fun applyConfirmedFields(fields: List<String>?) {
        if (fields != null) _confirmedFields.value = fields
    }

    fun setConversationId(id: String) {
        _conversationId.value = id
    }

    fun setStravaConnected(connected: Boolean) {
        _stravaConnected.value = connected
    }

    fun reset() {
        _confirmedFields.value = emptyList()
        _partialIntake.value = null
        _historySummary.value = null
        _stravaConnected.value = false
        _conversationId.value = null
    }
}
