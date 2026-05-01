package com.stridewell.app.ui.components.skeleton

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Compose-native shimmer primitive — port of iOS `SkeletonView<S: Shape>`
 * (`Stridewell-iOS/.../Views/Shared/DesignSystem/SkeletonView.swift`).
 *
 * Renders a base color clipped to [shape] with an animated highlight sweeping
 * left-to-right on a 1.5s loop. Compose has no `BlendMode.SoftLight`, so the
 * highlight is a translucent linear gradient — visually close, not pixel-identical.
 *
 * Wrap with [SkeletonBlock] for the common rounded-rectangle text-line placeholder
 * case. Pass any [Shape] (e.g. `CircleShape`) for non-rectangular skeletons.
 */
@Composable
fun SkeletonView(
    shape: Shape,
    modifier: Modifier = Modifier,
    baseColor: Color = Color.Gray.copy(alpha = 0.30f),
    highlightColor: Color = Color.White.copy(alpha = 0.55f),
) {
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton-progress",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor)
            .drawWithContent {
                drawContent()
                val width = size.width
                val sweepWidth = width * 0.6f
                val startX = -sweepWidth + (width + sweepWidth) * progress
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            highlightColor,
                            Color.Transparent,
                        ),
                        start = Offset(startX, 0f),
                        end = Offset(startX + sweepWidth, size.height),
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                )
            },
    )
}

@Preview
@Composable
private fun SkeletonViewPreview() {
    SkeletonView(
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.size(120.dp, 24.dp),
    )
}
