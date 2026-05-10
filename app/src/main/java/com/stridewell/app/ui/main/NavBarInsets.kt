package com.stridewell.app.ui.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vertical space the floating GlassNavBar reserves at the bottom of every tab.
 *
 * 68dp pill height + 12dp gap above the system navigation bar inset.
 * Mirrors the constants in [GlassNavBar].
 */
private val GLASS_NAV_BAR_HEIGHT_WITH_GAP: Dp = 80.dp

/**
 * Bottom padding required so a tab's scroll content can clear the floating
 * GlassNavBar instead of being hidden behind it. Tabs intentionally draw
 * under the nav bar so the haze blur has real pixels to render — content
 * insets, not container padding, are the right place to compensate.
 *
 * Sum: system navigation bar inset + GlassNavBar pill (68dp) + bottom gap (12dp).
 */
@Composable
fun rememberNavBarBottomInset(): Dp {
    val systemNav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return systemNav + GLASS_NAV_BAR_HEIGHT_WITH_GAP
}
