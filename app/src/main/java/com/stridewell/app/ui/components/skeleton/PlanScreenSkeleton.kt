package com.stridewell.app.ui.components.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Mirrors PlanScreen layout: week navigator, week overview, 7-day workout list,
 * trailing metadata. Used as Plan tab's loading state.
 *
 * Port of iOS `PlanScreenSkeleton`.
 */
@Composable
fun PlanScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        WeekNavigatorBlock()
        WeekOverviewBlock()
        WeekDaysBlock()
        MetadataBlock()
    }
}

@Composable
private fun WeekNavigatorBlock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier) {
            SkeletonView(
                shape = CircleShape,
                modifier = Modifier.size(width = 32.dp, height = 32.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        SkeletonBlock(height = 18.dp, width = 160.dp)
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier) {
            SkeletonView(
                shape = CircleShape,
                modifier = Modifier.size(width = 32.dp, height = 32.dp),
            )
        }
    }
}

@Composable
private fun WeekOverviewBlock() {
    SkeletonBlock(height = 120.dp, cornerRadius = CornerRadius.md)
}

@Composable
private fun WeekDaysBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        repeat(7) {
            SkeletonBlock(height = 84.dp, cornerRadius = CornerRadius.md)
        }
    }
}

@Composable
private fun MetadataBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SkeletonBlock(height = 14.dp, width = 140.dp)
        SkeletonBlock(height = 12.dp)
        SkeletonBlock(height = 12.dp, width = 240.dp)
    }
}

@Preview(heightDp = 900)
@Composable
private fun PlanScreenSkeletonPreview() {
    StridewellTheme {
        PlanScreenSkeleton()
    }
}
