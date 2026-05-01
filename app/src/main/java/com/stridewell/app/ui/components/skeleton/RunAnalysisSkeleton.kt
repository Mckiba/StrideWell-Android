package com.stridewell.app.ui.components.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Compact skeleton for the analysis section while it loads independently of the
 * rest of RunDetailScreen.
 *
 * Mirrors iOS `RunAnalysisSkeleton`.
 */
@Composable
fun RunAnalysisSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SkeletonBlock(height = 18.dp, width = 160.dp)
        SkeletonBlock(height = 12.dp)
        SkeletonBlock(height = 12.dp, width = 260.dp)
    }
}

@Preview
@Composable
private fun RunAnalysisSkeletonPreview() {
    StridewellTheme {
        RunAnalysisSkeleton(modifier = Modifier.fillMaxWidth())
    }
}
