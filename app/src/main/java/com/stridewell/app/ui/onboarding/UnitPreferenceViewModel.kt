package com.stridewell.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.data.SettingsRepository
import com.stridewell.app.util.UnitPreference
import com.stridewell.app.util.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Backs the first onboarding step (unit preference). Holds no UI state — the screen owns the
 * current selection; this only exposes a sensible default and persists the final choice.
 */
@HiltViewModel
class UnitPreferenceViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Default before the athlete chooses — US locale lands on Imperial, otherwise Metric. */
    val defaultUnit: UnitSystem =
        if (Locale.getDefault().country == "US") UnitSystem.IMPERIAL else UnitSystem.METRIC

    /**
     * Reflect the choice immediately for synchronous readers (OnboardingUnits), then persist it
     * and best-effort sync to the backend so the Coach converses in this unit.
     */
    fun commit(unit: UnitSystem) {
        UnitPreference.current = unit
        viewModelScope.launch { settingsRepository.setUnitSystem(unit) }
    }
}
