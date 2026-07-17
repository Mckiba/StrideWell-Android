package com.stridewell.app.ui.onboarding.guided

import android.icu.util.LocaleData
import android.icu.util.ULocale
import com.stridewell.app.util.UnitPreference
import com.stridewell.app.util.UnitSystem
import kotlin.math.roundToInt

/**
 * Display-unit helpers for the guided onboarding structured inputs (volume slider, history
 * chart labels). The UI shows miles or kilometres by the athlete's chosen unit (set in the
 * first onboarding step / Settings); every value stored or sent is always kilometres.
 */
object OnboardingUnits {

    const val kmPerMile = 1.609344

    /** Whether the athlete's chosen unit is imperial; falls back to device locale when unset. */
    val usesMiles: Boolean
        get() = when (UnitPreference.current) {
            UnitSystem.IMPERIAL -> true
            UnitSystem.METRIC -> false
            null -> runCatching {
                LocaleData.getMeasurementSystem(ULocale.getDefault()) != LocaleData.MeasurementSystem.SI
            }.getOrDefault(false)
        }

    val unitLabel: String get() = if (usesMiles) "mi" else "km"

    /** Convert a stored km value to the display unit. */
    fun displayValueFromKm(km: Double): Double = if (usesMiles) km / kmPerMile else km

    /** Convert a display-unit value back to km for storage/transmission. */
    fun kmFromDisplay(value: Double): Double = if (usesMiles) value * kmPerMile else value

    /** Slider maximum in the display unit (100 mi ≈ 160 km). */
    val weeklyVolumeMax: Double get() = if (usesMiles) 100.0 else 160.0

    fun formatted(displayValue: Double): String = "${displayValue.roundToInt()} $unitLabel"
}
