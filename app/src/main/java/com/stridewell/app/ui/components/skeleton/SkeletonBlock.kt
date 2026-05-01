package com.stridewell.app.ui.components.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.Spacing

/**
 * Rounded-rectangle shimmer block — the most common skeleton primitive.
 *
 * Mirrors iOS `SkeletonBlock(width:height:cornerRadius:)`. When [width] is null,
 * the block fills its parent's width.
 */
@Composable
fun SkeletonBlock(
    height: Dp,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    cornerRadius: Dp = 6.dp,
) {
    val sized = if (width != null) {
        modifier.width(width).height(height)
    } else {
        modifier.fillMaxWidth().height(height)
    }
    SkeletonView(
        shape = RoundedCornerShape(cornerRadius),
        modifier = sized,
    )
}

@Preview
@Composable
private fun SkeletonBlockPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.padding(Spacing.md),
    ) {
        SkeletonBlock(height = 24.dp, width = 200.dp, cornerRadius = 8.dp)
        SkeletonBlock(height = 12.dp)
        SkeletonBlock(height = 12.dp, width = 240.dp)
    }
}
