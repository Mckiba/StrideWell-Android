package com.stridewell.app.ui.components.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Mirrors ActivitiesScreen list layout: a vertical stack of activity-card-shaped
 * rows. Sized to fill the screen so the skeleton extends past the visible
 * viewport while loading.
 *
 * Port of iOS `ActivitiesScreenSkeleton`.
 */
@Composable
fun ActivitiesScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        repeat(10) {
            SkeletonBlock(
                height = 70.dp,
                cornerRadius = CornerRadius.md,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
        }
    }
}

@Preview(heightDp = 900)
@Composable
private fun ActivitiesScreenSkeletonPreview() {
    StridewellTheme {
        ActivitiesScreenSkeleton()
    }
}
