package com.stridewell.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        private val KEY_UNIT_SYSTEM = stringPreferencesKey("settings_unit_system")
    }

    val unitSystem: Flow<UnitSystem> = dataStore.data.map { prefs ->
        prefs[KEY_UNIT_SYSTEM]?.toUnitSystem() ?: defaultUnitSystem()
    }

    suspend fun getUnitSystem(): UnitSystem =
        dataStore.data.first()[KEY_UNIT_SYSTEM]?.toUnitSystem() ?: defaultUnitSystem()

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { prefs ->
            prefs[KEY_UNIT_SYSTEM] = unitSystem.name
        }
    }

    private fun defaultUnitSystem(): UnitSystem =
        if (Locale.getDefault().country == "US") UnitSystem.IMPERIAL else UnitSystem.METRIC

    private fun String.toUnitSystem(): UnitSystem =
        runCatching { UnitSystem.valueOf(this) }.getOrDefault(defaultUnitSystem())
}
