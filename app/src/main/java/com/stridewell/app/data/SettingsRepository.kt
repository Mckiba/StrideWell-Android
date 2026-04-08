package com.stridewell.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stridewell.app.util.AppTheme
import com.stridewell.app.util.UnitSystem
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(
    @Named("settings") private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val KEY_UNIT_SYSTEM          = stringPreferencesKey("settings_unit_system")
        private val KEY_APP_THEME            = stringPreferencesKey("settings_app_theme")
        private val KEY_REFLECTION_REMINDERS = booleanPreferencesKey("settings_reflection_reminders")
        private val KEY_PLAN_UPDATE_ALERTS   = booleanPreferencesKey("settings_plan_update_alerts")
        private val KEY_HOME_HEATMAP_ONLY    = booleanPreferencesKey("settings_home_heatmap_only")
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

    suspend fun getUnitSystem(): UnitSystem =
        dataStore.data.first()[KEY_UNIT_SYSTEM]?.toUnitSystem() ?: defaultUnitSystem()

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { prefs ->
            prefs[KEY_UNIT_SYSTEM] = unitSystem.name
        }
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

    suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_UNIT_SYSTEM)
            prefs.remove(KEY_APP_THEME)
            prefs.remove(KEY_REFLECTION_REMINDERS)
            prefs.remove(KEY_PLAN_UPDATE_ALERTS)
            prefs.remove(KEY_HOME_HEATMAP_ONLY)
        }
    }

    private fun defaultUnitSystem(): UnitSystem =
        if (Locale.getDefault().country == "US") UnitSystem.IMPERIAL else UnitSystem.METRIC

    private fun String.toUnitSystem(): UnitSystem =
        runCatching { UnitSystem.valueOf(this) }.getOrDefault(defaultUnitSystem())

    private fun String.toAppTheme(): AppTheme =
        runCatching { AppTheme.valueOf(this) }.getOrDefault(AppTheme.DEVICE)
}
