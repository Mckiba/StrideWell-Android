package com.stridewell.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary           = md_light_primary,
    onPrimary         = md_light_onPrimary,
    primaryContainer  = md_light_primaryContainer,
    onPrimaryContainer = md_light_onPrimaryContainer,
    secondary         = md_light_secondary,
    onSecondary       = md_light_onSecondary,
    background        = md_light_background,
    onBackground      = md_light_onBackground,
    surface           = md_light_surface,
    onSurface         = md_light_onSurface,
    surfaceVariant    = md_light_surfaceVariant,
    onSurfaceVariant  = md_light_onSurfaceVariant,
    outline           = md_light_outline,
    error             = md_light_error,
    onError           = md_light_onError
)

private val DarkColorScheme = darkColorScheme(
    primary           = md_dark_primary,
    onPrimary         = md_dark_onPrimary,
    primaryContainer  = md_dark_primaryContainer,
    onPrimaryContainer = md_dark_onPrimaryContainer,
    secondary         = md_dark_secondary,
    onSecondary       = md_dark_onSecondary,
    background        = md_dark_background,
    onBackground      = md_dark_onBackground,
    surface           = md_dark_surface,
    onSurface         = md_dark_onSurface,
    surfaceVariant    = md_dark_surfaceVariant,
    onSurfaceVariant  = md_dark_onSurfaceVariant,
    outline           = md_dark_outline,
    error             = md_dark_error,
    onError           = md_dark_onError
)

@Composable
fun StridewellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is disabled — the app has its own design-system palette and dynamic
    // color from Android 12+ would override those tokens.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = StridewellTypography,
        content     = content
    )
}
