package com.stridewell.app.ui.onboarding.guided

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Full-bleed [background] with a bottom sheet that drags between a collapsed and an expanded
 * height, snapping to the nearer on release. [sheetContent] receives an `onInteract` callback
 * to expand the sheet programmatically (e.g. on input focus).
 */
@Composable
fun ExpandableSheetScaffold(
    modifier: Modifier = Modifier,
    autoExpand: Boolean = false,
    collapsedFraction: Float = 0.52f,
    expandedFraction: Float = 0.9f,
    sheetColor: Color = MaterialTheme.colorScheme.surface,
    handleColor: Color = MaterialTheme.colorScheme.outline,
    background: @Composable BoxScope.() -> Unit,
    sheetContent: @Composable ColumnScope.(onInteract: () -> Unit) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val available = maxHeight
        val availablePx = with(density) { available.toPx() }
        val collapsedPx = availablePx * collapsedFraction
        val expandedPx = availablePx * expandedFraction

        val heightPx = remember { Animatable(collapsedPx) }
        var expanded by remember { mutableStateOf(false) }

        fun expand() {
            if (expanded) return
            expanded = true
            scope.launch { heightPx.animateTo(expandedPx, spring(dampingRatio = 0.85f, stiffness = 400f)) }
        }

        LaunchedEffect(autoExpand) { if (autoExpand) expand() }

        Box(modifier = Modifier.fillMaxSize()) {
            background()

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(with(density) { heightPx.value.toDp() })
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(sheetColor)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                DragHandle(
                    handleColor = handleColor,
                    onDrag = { delta ->
                        scope.launch {
                            heightPx.snapTo((heightPx.value - delta).coerceIn(collapsedPx, expandedPx))
                        }
                    },
                    onDragStopped = { velocity ->
                        val shouldExpand = when {
                            velocity < -VELOCITY_THRESHOLD -> true
                            velocity > VELOCITY_THRESHOLD -> false
                            else -> heightPx.value > (collapsedPx + expandedPx) / 2f
                        }
                        expanded = shouldExpand
                        scope.launch {
                            heightPx.animateTo(
                                if (shouldExpand) expandedPx else collapsedPx,
                                spring(dampingRatio = 0.85f, stiffness = 400f)
                            )
                        }
                    }
                )
                sheetContent(::expand)
            }
        }
    }
}

@Composable
private fun DragHandle(handleColor: Color, onDrag: (Float) -> Unit, onDragStopped: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm, bottom = 6.dp)
            .draggable(
                state = rememberDraggableState { delta -> onDrag(delta) },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> onDragStopped(velocity) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier = Modifier
                .width(40.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(handleColor)
        )
    }
}

private const val VELOCITY_THRESHOLD = 400f
