package com.stridewell.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class ActivityRepository @Inject constructor(
    @Named("activity") private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val KEY_LAST_SYNCED_RUN_ID = stringPreferencesKey("activity_last_synced_run_id")
        private val KEY_LAST_SYNCED_RUN_SUMMARY = stringPreferencesKey("activity_last_synced_run_summary")
        private val KEY_LAST_SEEN_RUN_ID = stringPreferencesKey("activity_last_seen_run_id")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _lastSyncedRunId = MutableStateFlow<String?>(null)
    val lastSyncedRunId: StateFlow<String?> = _lastSyncedRunId.asStateFlow()

    private val _lastSyncedRunSummary = MutableStateFlow<String?>(null)
    val lastSyncedRunSummary: StateFlow<String?> = _lastSyncedRunSummary.asStateFlow()

    private val _lastSeenRunId = MutableStateFlow<String?>(null)
    val lastSeenRunId: StateFlow<String?> = _lastSeenRunId.asStateFlow()

    val showActivityBanner: StateFlow<Boolean> = combine(
        _lastSyncedRunId,
        _lastSeenRunId
    ) { synced, seen ->
        synced != null && synced != seen
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    init {
        scope.launch {
            val prefs = dataStore.data.first()
            _lastSyncedRunId.value = prefs[KEY_LAST_SYNCED_RUN_ID]
            _lastSyncedRunSummary.value = prefs[KEY_LAST_SYNCED_RUN_SUMMARY]
            _lastSeenRunId.value = prefs[KEY_LAST_SEEN_RUN_ID]
        }
    }

    suspend fun setLastSyncedRun(runId: String, summary: String) {
        _lastSyncedRunId.value = runId
        _lastSyncedRunSummary.value = summary
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNCED_RUN_ID] = runId
            prefs[KEY_LAST_SYNCED_RUN_SUMMARY] = summary
        }
    }

    suspend fun dismissBanner() {
        val seenRunId = _lastSyncedRunId.value
        _lastSeenRunId.value = seenRunId
        dataStore.edit { prefs ->
            if (seenRunId == null) {
                prefs.remove(KEY_LAST_SEEN_RUN_ID)
            } else {
                prefs[KEY_LAST_SEEN_RUN_ID] = seenRunId
            }
        }
    }

    suspend fun reset() {
        _lastSyncedRunId.value = null
        _lastSyncedRunSummary.value = null
        _lastSeenRunId.value = null
        dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_SYNCED_RUN_ID)
            prefs.remove(KEY_LAST_SYNCED_RUN_SUMMARY)
            prefs.remove(KEY_LAST_SEEN_RUN_ID)
        }
    }
}
