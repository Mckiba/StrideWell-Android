package com.stridewell.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.SettingsApi
import com.stridewell.app.model.ProactiveCategoriesEnabled
import com.stridewell.app.model.ProactivePreferencesRequest
import com.stridewell.app.model.ProactivePreferencesStoredResponse
import com.stridewell.app.model.ProactiveQuietHours
import com.stridewell.app.model.UserUnitsRequest
import java.io.IOException
import com.stridewell.app.util.AppTheme
import com.stridewell.app.util.UnitPreference
import com.stridewell.app.util.UnitSystem
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Response

@Singleton
class SettingsRepository @Inject constructor(
    @Named("settings") private val dataStore: DataStore<Preferences>,
    private val settingsApi: SettingsApi,
) {

    companion object {
        private val KEY_UNIT_SYSTEM          = stringPreferencesKey("settings_unit_system")
        private val KEY_APP_THEME            = stringPreferencesKey("settings_app_theme")
        private val KEY_REFLECTION_REMINDERS = booleanPreferencesKey("settings_reflection_reminders")
        private val KEY_PLAN_UPDATE_ALERTS   = booleanPreferencesKey("settings_plan_update_alerts")
        private val KEY_HOME_HEATMAP_ONLY    = booleanPreferencesKey("settings_home_heatmap_only")
        private val KEY_PROACTIVE_ENABLED     = booleanPreferencesKey("settings_proactive_enabled")
        private val KEY_PROACTIVE_MILESTONE   = booleanPreferencesKey("settings_proactive_training_milestone")
        private val KEY_PROACTIVE_CONCERN     = booleanPreferencesKey("settings_proactive_training_concern")
        private val KEY_PROACTIVE_UPCOMING    = booleanPreferencesKey("settings_proactive_upcoming_event")
        private val KEY_PROACTIVE_REENGAGE    = booleanPreferencesKey("settings_proactive_reengagement")
        private val KEY_PROACTIVE_FOLLOWUP    = booleanPreferencesKey("settings_proactive_plan_followup")
        private val KEY_PROACTIVE_QUIET_ON    = booleanPreferencesKey("settings_proactive_quiet_hours_enabled")
        private val KEY_PROACTIVE_QUIET_START = stringPreferencesKey("settings_proactive_quiet_hours_start")
        private val KEY_PROACTIVE_QUIET_END   = stringPreferencesKey("settings_proactive_quiet_hours_end")
        private val KEY_PROACTIVE_TIMEZONE    = stringPreferencesKey("settings_proactive_timezone")
    }

    val unitSystem: Flow<UnitSystem> = dataStore.data.map { prefs ->
        prefs[KEY_UNIT_SYSTEM]?.toUnitSystem() ?: defaultUnitSystem()
    }

    val appTheme: Flow<AppTheme> = dataStore.data.map { prefs ->
        prefs[KEY_APP_THEME]?.toAppTheme() ?: AppTheme.DEVICE
    }

    val reflectionReminders: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_REFLECTION_REMINDERS] ?: true
    }

    val planUpdateAlerts: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PLAN_UPDATE_ALERTS] ?: true
    }

    val homeHeatmapOnly: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HOME_HEATMAP_ONLY] ?: false
    }

    val proactiveEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_ENABLED] ?: true
    }

    val proactiveTrainingMilestone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_MILESTONE] ?: true
    }

    val proactiveTrainingConcern: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_CONCERN] ?: true
    }

    val proactiveUpcomingEvent: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_UPCOMING] ?: true
    }

    val proactiveReengagement: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_REENGAGE] ?: true
    }

    val proactivePlanFollowup: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_FOLLOWUP] ?: true
    }

    val proactiveQuietHoursEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_QUIET_ON] ?: true
    }

    val proactiveQuietHoursStart: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_QUIET_START] ?: "22:00"
    }

    val proactiveQuietHoursEnd: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_QUIET_END] ?: "07:00"
    }

    val proactiveTimezone: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PROACTIVE_TIMEZONE] ?: TimeZone.getDefault().id
    }

    suspend fun getUnitSystem(): UnitSystem =
        dataStore.data.first()[KEY_UNIT_SYSTEM]?.toUnitSystem() ?: defaultUnitSystem()

    /**
     * Persists the unit preference locally, updates the in-memory snapshot for synchronous
     * readers, and best-effort syncs it to the backend so the Coach converses in this unit.
     * A sync failure is non-fatal — the local value drives all formatting.
     */
    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { prefs ->
            prefs[KEY_UNIT_SYSTEM] = unitSystem.name
        }
        UnitPreference.current = unitSystem
        safeCall { settingsApi.putUnits(UserUnitsRequest(unitSystem.name.lowercase())) }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { prefs -> prefs[KEY_APP_THEME] = theme.name }
    }

    suspend fun setReflectionReminders(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_REFLECTION_REMINDERS] = enabled }
    }

    suspend fun setPlanUpdateAlerts(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PLAN_UPDATE_ALERTS] = enabled }
    }

    suspend fun setHomeHeatmapOnly(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_HOME_HEATMAP_ONLY] = enabled }
    }

    suspend fun setProactiveEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PROACTIVE_ENABLED] = enabled }
    }

    suspend fun setProactiveTrainingMilestone(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PROACTIVE_MILESTONE] = enabled }
    }

    suspend fun setProactiveTrainingConcern(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PROACTIVE_CONCERN] = enabled }
    }

    suspend fun setProactiveUpcomingEvent(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PROACTIVE_UPCOMING] = enabled }
    }

    suspend fun setProactiveReengagement(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PROACTIVE_REENGAGE] = enabled }
    }

    suspend fun setProactivePlanFollowup(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PROACTIVE_FOLLOWUP] = enabled }
    }

    suspend fun setProactiveQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PROACTIVE_QUIET_ON] = enabled }
    }

    suspend fun setProactiveQuietHoursStart(startLocal: String) {
        dataStore.edit { prefs -> prefs[KEY_PROACTIVE_QUIET_START] = startLocal }
    }

    suspend fun setProactiveQuietHoursEnd(endLocal: String) {
        dataStore.edit { prefs -> prefs[KEY_PROACTIVE_QUIET_END] = endLocal }
    }

    suspend fun putProactivePreferences(): ApiResult<ProactivePreferencesStoredResponse> {
        val payload = getProactivePreferencesPayload()
        return safeCall { settingsApi.putProactivePreferences(payload) }
    }

    suspend fun getProactivePreferencesPayload(): ProactivePreferencesRequest {
        val prefs = dataStore.data.first()
        return ProactivePreferencesRequest(
            enabled = prefs[KEY_PROACTIVE_ENABLED] ?: true,
            categoriesEnabled = ProactiveCategoriesEnabled(
                trainingMilestone = prefs[KEY_PROACTIVE_MILESTONE] ?: true,
                trainingConcern = prefs[KEY_PROACTIVE_CONCERN] ?: true,
                upcomingEvent = prefs[KEY_PROACTIVE_UPCOMING] ?: true,
                reengagement = prefs[KEY_PROACTIVE_REENGAGE] ?: true,
                planFollowup = prefs[KEY_PROACTIVE_FOLLOWUP] ?: true,
            ),
            quietHours = ProactiveQuietHours(
                enabled = prefs[KEY_PROACTIVE_QUIET_ON] ?: true,
                startLocal = prefs[KEY_PROACTIVE_QUIET_START] ?: "22:00",
                endLocal = prefs[KEY_PROACTIVE_QUIET_END] ?: "07:00",
            ),
            timezone = prefs[KEY_PROACTIVE_TIMEZONE] ?: TimeZone.getDefault().id,
        )
    }

    suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_UNIT_SYSTEM)
            prefs.remove(KEY_APP_THEME)
            prefs.remove(KEY_REFLECTION_REMINDERS)
            prefs.remove(KEY_PLAN_UPDATE_ALERTS)
            prefs.remove(KEY_HOME_HEATMAP_ONLY)
            prefs.remove(KEY_PROACTIVE_ENABLED)
            prefs.remove(KEY_PROACTIVE_MILESTONE)
            prefs.remove(KEY_PROACTIVE_CONCERN)
            prefs.remove(KEY_PROACTIVE_UPCOMING)
            prefs.remove(KEY_PROACTIVE_REENGAGE)
            prefs.remove(KEY_PROACTIVE_FOLLOWUP)
            prefs.remove(KEY_PROACTIVE_QUIET_ON)
            prefs.remove(KEY_PROACTIVE_QUIET_START)
            prefs.remove(KEY_PROACTIVE_QUIET_END)
            prefs.remove(KEY_PROACTIVE_TIMEZONE)
        }
    }

    private fun defaultUnitSystem(): UnitSystem =
        if (Locale.getDefault().country == "US") UnitSystem.IMPERIAL else UnitSystem.METRIC

    private fun String.toUnitSystem(): UnitSystem =
        runCatching { UnitSystem.valueOf(this) }.getOrDefault(defaultUnitSystem())

    private fun String.toAppTheme(): AppTheme =
        runCatching { AppTheme.valueOf(this) }.getOrDefault(AppTheme.DEVICE)

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body)
                } else {
                    ApiResult.Error(response.code(), "Empty response body")
                }
            } else {
                ApiResult.Error(response.code(), response.message())
            }
        } catch (_: IOException) {
            ApiResult.Error(0, "No internet connection.")
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Unknown error")
        }
    }
}
