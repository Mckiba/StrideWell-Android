package com.stridewell.app.util

enum class AppTheme { DEVICE, LIGHT, DARK }

fun AppTheme.isDark(systemIsDark: Boolean): Boolean = when (this) {
    AppTheme.DEVICE -> systemIsDark
    AppTheme.LIGHT  -> false
    AppTheme.DARK   -> true
}
