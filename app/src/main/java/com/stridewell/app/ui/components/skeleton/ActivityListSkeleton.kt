package com.stridewell.app.ui.components.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * Placeholder for the Activities overview list while runs load: an optional
 * section-title bar followed by activity-card-shaped blocks.
 */
@Composable
fun ActivityListSkeleton(
    cardCount: Int,
    modifier: Modifier = Modifier,
    showsHeader: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl2),
    ) {
        if (showsHeader) {
            SkeletonBlock(height = 22.dp, width = 140.dp, cornerRadius = CornerRadius.sm)
        }
        repeat(cardCount) {
            SkeletonBlock(height = 80.dp, cornerRadius = CornerRadius.md)
        }
    }
}

@Preview(heightDp = 460)
@Composable
private fun ActivityListSkeletonPreview() {
    StridewellTheme {
        ActivityListSkeleton(cardCount = 3, modifier = Modifier.padding(Spacing.md))
    }
}

@Preview(heightDp = 140)
@Composable
private fun ActivityListSkeletonLoadMorePreview() {
    StridewellTheme {
        ActivityListSkeleton(
            cardCount = 1,
            showsHeader = false,
            modifier = Modifier.padding(Spacing.md)
        )
    }
}
