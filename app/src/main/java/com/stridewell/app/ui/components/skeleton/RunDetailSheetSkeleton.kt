package com.stridewell.app.ui.components.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Mirrors RunDetailScreen's headerSection + splitsSection + statsSection layout
 * so the swap to real content doesn't shift the page.
 *
 * Port of iOS `RunDetailSheetSkeleton`.
 */
@Composable
fun RunDetailSheetSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        HeaderBlock()
        HorizontalDivider()
        SplitsBlock()
        HorizontalDivider()
        StatsBlock()
    }
}

@Composable
private fun HeaderBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SkeletonBlock(height = 24.dp, width = 220.dp, cornerRadius = 8.dp)
        SkeletonBlock(height = 12.dp, width = 140.dp)

        Row(modifier = Modifier.fillMaxWidth()) {
            StatPlaceholder()
            Spacer(modifier = Modifier.weight(1f))
            StatPlaceholder()
            Spacer(modifier = Modifier.weight(1f))
            StatPlaceholder()
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatPlaceholder()
            Spacer(modifier = Modifier.weight(1f))
            StatPlaceholder()
            Spacer(modifier = Modifier.weight(1f))
            StatPlaceholder()
        }
    }
}

@Composable
private fun StatPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SkeletonBlock(height = 10.dp, width = 64.dp)
        SkeletonBlock(height = 18.dp, width = 80.dp)
    }
}

@Composable
private fun SplitsBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SkeletonBlock(
            height = 18.dp,
            width = 80.dp,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            repeat(4) {
                SkeletonBlock(height = 32.dp, cornerRadius = 8.dp)
            }
        }
    }
}

@Composable
private fun StatsBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SkeletonBlock(
            height = 18.dp,
            width = 100.dp,
            modifier = Modifier
                .padding(horizontal = Spacing.md)
                .padding(top = Spacing.md),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(11) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md),
                ) {
                    SkeletonBlock(height = 14.dp, width = 110.dp)
                    Spacer(modifier = Modifier.weight(1f))
                    SkeletonBlock(height = 14.dp, width = 70.dp)
                }
            }
        }
    }
}

@Preview(heightDp = 700)
@Composable
private fun RunDetailSheetSkeletonPreview() {
    StridewellTheme {
        RunDetailSheetSkeleton()
    }
}
