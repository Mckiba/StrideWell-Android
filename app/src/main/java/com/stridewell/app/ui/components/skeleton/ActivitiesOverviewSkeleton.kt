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
 * Mirrors the Activities overview: range picker, period totals, chart, and the
 * first activity cards.
 */
@Composable
fun ActivitiesOverviewSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl2),
    ) {
        SkeletonBlock(height = 30.dp, cornerRadius = 15.dp)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SkeletonBlock(height = 22.dp, width = 110.dp, cornerRadius = CornerRadius.sm)
            SkeletonBlock(height = 64.dp, width = 140.dp, cornerRadius = CornerRadius.sm)
            SkeletonBlock(height = 20.dp, width = 60.dp, cornerRadius = CornerRadius.sm)
            SkeletonBlock(height = 48.dp, cornerRadius = CornerRadius.sm)
        }
        SkeletonBlock(height = 140.dp, cornerRadius = CornerRadius.sm)
        repeat(2) {
            SkeletonBlock(height = 80.dp, cornerRadius = CornerRadius.md)
        }
    }
}

@Preview(heightDp = 900)
@Composable
private fun ActivitiesOverviewSkeletonPreview() {
    StridewellTheme {
        ActivitiesOverviewSkeleton()
    }
}
