package com.stridewell.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val NavShape = RoundedCornerShape(32.dp)

@Composable
fun GlassNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    // All colors sourced from MaterialTheme.colorScheme — the live CompositionLocal
    // that updates instantly on light/dark theme switches, so no isSystemInDarkTheme()
    // or hardcoded hex values are needed here.
    val colorScheme = MaterialTheme.colorScheme

    // backgroundColor: required by HazeStyle as the solid fallback on API < 31 and
    // as the base compositing layer. The screen background is the right choice.
    val backgroundColor = colorScheme.background

    // Tint layered on top of the blur — surface colour at reduced alpha so the frosted
    // blur reads through clearly. surfaceVariant sits between background and card surface,
    // giving a subtle frosted tone that works in both light and dark modes.
    val tintColor = colorScheme.surfaceVariant.copy(alpha = 0.55f)

    // 1dp rim border — outline at low alpha gives a clean edge without looking heavy.
    val borderColor = colorScheme.outline.copy(alpha = 0.35f)

    // Accent and unselected colours pulled from the theme for instant adaptation.
    val accent = colorScheme.primary
    val unselected = colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = 20.dp,
                shape = NavShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(NavShape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = backgroundColor,
                    tints = listOf(HazeTint(color = tintColor)),
                    blurRadius = 24.dp,
                    noiseFactor = 0.06f,
                )
            )
            .border(width = 0.8.dp, color = borderColor, shape = NavShape),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MainTab.entries.forEach { tab ->
            GlassNavItem(
                tab = tab,
                isSelected = selectedTab == tab,
                accent = accent,
                unselected = unselected,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun GlassNavItem(
    tab: MainTab,
    isSelected: Boolean,
    accent: Color,
    unselected: Color,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) accent else unselected,
        animationSpec = tween(durationMillis = 200),
        label = "navIconColor_${tab.name}"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "navIndicator_${tab.name}"
    )

    Column(
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pill indicator behind the icon
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(indicatorColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                modifier = Modifier.size(22.dp),
                tint = iconColor
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            color = iconColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 12.sp,
            maxLines = 1
        )
    }
}
