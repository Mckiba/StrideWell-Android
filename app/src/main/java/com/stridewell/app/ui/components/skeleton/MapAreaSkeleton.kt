package com.stridewell.app.ui.components.skeleton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape

/**
 * Shimmer covering the visible map area above the bottom sheet (top half of
 * the screen at the mid detent). Sized so the shimmer animation feels right
 * rather than being hidden behind the sheet.
 *
 * Mirrors iOS `MapAreaSkeleton`.
 */
@Composable
fun MapAreaSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        SkeletonView(
            shape = RectangleShape,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f),
        )
        Spacer(modifier = Modifier.fillMaxSize())
    }
}
