package com.stridewell.app.util

/**
 * Process-wide snapshot of the athlete's chosen unit, kept in sync by [SettingsRepository]
 * (written on every change and seeded at app start). Lets synchronous UI helpers such as
 * [com.stridewell.app.ui.onboarding.guided.OnboardingUnits] read the preference without a
 * suspend DataStore call. Null until first read; callers fall back to device locale.
 */
object UnitPreference {
    @Volatile
    var current: UnitSystem? = null
}
