package com.stridewell.app.ui.components

import androidx.annotation.DrawableRes
import com.stridewell.R

// Maps a backend `icon` string to a themed weather VectorDrawable.
// Rendered as a tinted Icon, so the drawable's own colors are overridden.
@DrawableRes
fun weatherCardImage(icon: String): Int = when (icon) {
    "exclamation_circle"    -> R.drawable.weather_extreme_weather
    "rain_cloud"            -> R.drawable.weather_rain
    "sun_max"               -> R.drawable.weather_uv
    "wind"                  -> R.drawable.weather_wind
    "thermometer_sun"       -> R.drawable.weather_sun
    "thermometer_snowflake" -> R.drawable.weather_extreme_cold
    "sun_setting"           -> R.drawable.weather_sunset
    else                    -> R.drawable.weather_sun
}
