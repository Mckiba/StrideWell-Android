package com.stridewell.app.ui.components.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Mirrors HomeScreen layout: goal card, banner carousel, today's workout,
 * recent activities. Used as the loading state on Home.
 *
 * Port of iOS `HomeScreenSkeleton`.
 */
@Composable
fun HomeScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        GoalCardBlock()
        BannerCarouselBlock()
        TodayWorkoutBlock()
        RecentActivitiesBlock()
    }
}

@Composable
private fun GoalCardBlock() {
    SkeletonBlock(height = 80.dp, cornerRadius = CornerRadius.md)
}

@Composable
private fun BannerCarouselBlock() {
    SkeletonBlock(height = 140.dp, cornerRadius = CornerRadius.lg)
}

@Composable
private fun TodayWorkoutBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SkeletonBlock(height = 18.dp, width = 80.dp)
        SkeletonBlock(height = 110.dp, cornerRadius = CornerRadius.md)
    }
}

@Composable
private fun RecentActivitiesBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SkeletonBlock(height = 18.dp, width = 140.dp)
            Spacer(modifier = Modifier.weight(1f))
            SkeletonBlock(height = 12.dp, width = 60.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(3) {
                SkeletonBlock(height = 70.dp, cornerRadius = CornerRadius.md)
            }
        }
    }
}

@Preview(heightDp = 800)
@Composable
private fun HomeScreenSkeletonPreview() {
    StridewellTheme {
        HomeScreenSkeleton()
    }
}
