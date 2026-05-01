package com.stridewell.app.ui.main.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Discrete sheet positions for [ThreeDetentSheet]. */
enum class SheetAnchor { Peek, Mid, Full }

/**
 * State holder for [ThreeDetentSheet]. Construct via [rememberThreeDetentSheetState].
 *
 * Wraps an [AnchoredDraggableState] and exposes:
 *  - [currentAnchor] — the resting anchor
 *  - [sheetTopPx] — the live (animating) y-position of the sheet's top edge
 *  - [bottomInsetPx] — the live height of the sheet from the bottom of the
 *    container; pass to a background map's camera padding so the route stays
 *    framed and the Mapbox attribution remains above the sheet
 */
@OptIn(ExperimentalFoundationApi::class)
class ThreeDetentSheetState internal constructor(
    initial: SheetAnchor,
    internal val draggableState: AnchoredDraggableState<SheetAnchor>,
) {
    internal var containerHeightPx by mutableFloatStateOf(0f)
    internal var safeBottomPx by mutableFloatStateOf(0f)

    val currentAnchor: SheetAnchor get() = draggableState.currentValue
    val targetAnchor: SheetAnchor get() = draggableState.targetValue

    /** Y-position (px) of the sheet's top edge from the top of the container. */
    val sheetTopPx: Float
        get() = if (draggableState.offset.isNaN()) containerHeightPx else draggableState.offset

    /** Pixels of the container reserved by the sheet (= containerHeight - sheetTop). */
    val bottomInsetPx: Float
        get() = (containerHeightPx - sheetTopPx).coerceAtLeast(0f)

    suspend fun animateTo(anchor: SheetAnchor) {
        draggableState.animateTo(anchor)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberThreeDetentSheetState(
    initial: SheetAnchor = SheetAnchor.Mid,
): ThreeDetentSheetState {
    val density = LocalDensity.current
    val savedAnchor: SheetAnchor = rememberSaveable { initial }
    val velocityThresholdPx = with(density) { 125.dp.toPx() }

    val draggable = remember {
        AnchoredDraggableState(
            initialValue = savedAnchor,
            anchors = DraggableAnchors {
                SheetAnchor.Peek at 0f
                SheetAnchor.Mid at 0f
                SheetAnchor.Full at 0f
            },
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { velocityThresholdPx },
            snapAnimationSpec = tween(durationMillis = 300),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    return remember(draggable) {
        ThreeDetentSheetState(initial = savedAnchor, draggableState = draggable)
    }
}

/**
 * Three-detent draggable bottom sheet:
 *  - **Peek** — small grabber row visible (~120dp + safe-area)
 *  - **Mid**  — covers ~50% of the container
 *  - **Full** — covers the entire container
 *
 * The hero / map content is supplied via [background]; the sheet body is the
 * [sheetContent] lambda. Background remains interactive at Peek and Mid since
 * the sheet only intercepts gestures within its own bounds.
 *
 * Predictive back: Full → Mid → Peek → [onPeekBack].
 *
 * Sheet movement comes from two sources:
 *  1. [DragHandle] — direct drag on the pill handle via [Modifier.draggable].
 *  2. [sheetScrollConnection] — intercepts [LazyColumn] scroll events so that
 *     scrolling up at Peek/Mid expands the sheet before content scrolls
 *     (mirrors iOS "scroll draws the sheet up" behaviour), and pulling down at
 *     Full when content is at its top collapses the sheet to Mid.
 *
 * The [Surface] carries no [anchoredDraggable] — that prevented the
 * [LazyColumn] from winning gesture disambiguation and broke scroll-to-expand.
 * The [Surface] also carries no corner clip — rounded-top clipping created
 * visible map gaps at the left/right screen edges.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreeDetentSheet(
    state: ThreeDetentSheetState,
    modifier: Modifier = Modifier,
    onPeekBack: () -> Unit = {},
    /**
     * Returns true when the sheet's scrollable content is scrolled to its very
     * top (i.e. [LazyListState.canScrollBackward] == false). Used to decide
     * whether a downward drag should collapse the sheet rather than scroll
     * content. Defaults to false (never collapse via scroll) if not supplied.
     */
    isContentAtTop: () -> Boolean = { false },
    background: @Composable BoxScope.() -> Unit,
    sheetContent: @Composable ColumnScope.(currentAnchor: SheetAnchor) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Intercepts LazyColumn scroll events so that:
    //
    //  EXPAND (Peek/Mid → Full):
    //   • onPreScroll with upward delta → dispatchRawDelta expands the sheet
    //     pixel-by-pixel before content scrolls (iOS "scroll draws sheet up").
    //   • Once at Full the unconsumed remainder flows to content.
    //   • onPreFling upward at non-Full → animateTo Full.
    //
    //  COLLAPSE (Full → Mid):
    //   • onPreScroll with downward delta + isContentAtTop() → intercept BEFORE
    //     the LazyColumn so Android overscroll never absorbs the velocity.
    //   • onPreFling downward + isContentAtTop() → animateTo Mid.
    //   • onPostFling: if dispatchRawDelta left the sheet at an intermediate
    //     position (e.g. slow drag, no fling), settle to the nearest anchor.
    val sheetScrollConnection = remember(state) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Expand: upward scroll (negative y) when sheet is not at Full.
                if (available.y < 0f && state.currentAnchor != SheetAnchor.Full) {
                    val unconsumed = state.draggableState.dispatchRawDelta(available.y)
                    return Offset(0f, available.y - unconsumed)
                }
                // Collapse: downward scroll (positive y) at Full when content is
                // already at its top. Intercept here so overscroll never gets it.
                if (available.y > 0f &&
                    state.currentAnchor == SheetAnchor.Full &&
                    isContentAtTop()
                ) {
                    val unconsumed = state.draggableState.dispatchRawDelta(available.y)
                    return Offset(0f, available.y - unconsumed)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Expand fling: upward velocity at non-Full → snap to Full.
                if (available.y < 0f && state.currentAnchor != SheetAnchor.Full) {
                    state.animateTo(SheetAnchor.Full)
                    return available
                }
                // Collapse fling: downward velocity at Full.
                // Use `alreadyDragged` as a reliable backup: if dispatchRawDelta
                // already moved the sheet (offset > fullOff), isContentAtTop() was
                // true during the drag, so we can safely collapse regardless of any
                // frame-lag in canScrollBackward.
                if (available.y > 0f && state.currentAnchor == SheetAnchor.Full) {
                    val offset  = state.draggableState.offset
                    val fullOff = state.draggableState.anchors.positionOf(SheetAnchor.Full)
                    val alreadyDragged = !offset.isNaN() && !fullOff.isNaN() && offset > fullOff
                    if (alreadyDragged || isContentAtTop()) {
                        state.animateTo(SheetAnchor.Mid)
                        return available
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Settle the sheet if dispatchRawDelta left it at an intermediate
                // position (e.g. slow drag with near-zero release velocity).
                val offset  = state.draggableState.offset
                if (offset.isNaN()) return Velocity.Zero

                val fullOff = state.draggableState.anchors.positionOf(SheetAnchor.Full)
                val midOff  = state.draggableState.anchors.positionOf(SheetAnchor.Mid)
                val peekOff = state.draggableState.anchors.positionOf(SheetAnchor.Peek)
                if (fullOff.isNaN() || midOff.isNaN() || peekOff.isNaN()) return Velocity.Zero

                val tolerance = 1f
                val isOffAnchor =
                    kotlin.math.abs(offset - fullOff) > tolerance &&
                    kotlin.math.abs(offset - midOff)  > tolerance &&
                    kotlin.math.abs(offset - peekOff) > tolerance

                if (!isOffAnchor) return Velocity.Zero

                val target = when {
                    // Sheet was dragged downward from Full: collapse to Mid for any
                    // release velocity, unless the user strongly flicked back upward
                    // (negative velocity < -500) to cancel the gesture.
                    state.currentAnchor == SheetAnchor.Full && offset > fullOff ->
                        if (available.y < -500f) SheetAnchor.Full else SheetAnchor.Mid

                    // Fast upward fling during Peek/Mid expansion → Full.
                    available.y < -300f -> SheetAnchor.Full

                    // Positional fall-through for Peek ↔ Mid transitions.
                    offset < (fullOff + midOff)  / 2f -> SheetAnchor.Full
                    offset < (midOff  + peekOff) / 2f -> SheetAnchor.Mid
                    else -> SheetAnchor.Peek
                }
                state.animateTo(target)
                return available
            }
        }
    }

    BackHandler(enabled = true) {
        scope.launch {
            when (state.currentAnchor) {
                SheetAnchor.Full -> state.animateTo(SheetAnchor.Mid)
                SheetAnchor.Mid -> state.animateTo(SheetAnchor.Peek)
                SheetAnchor.Peek -> onPeekBack()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                val h = size.height.toFloat()
                if (h <= 0f || state.containerHeightPx == h) return@onSizeChanged
                state.containerHeightPx = h
                val peekPx = with(density) { 120.dp.toPx() + state.safeBottomPx }
                val midPx = h * 0.5f
                val fullPx = h
                state.draggableState.updateAnchors(
                    DraggableAnchors {
                        SheetAnchor.Peek at (h - peekPx)
                        SheetAnchor.Mid at (h - midPx)
                        SheetAnchor.Full at (h - fullPx)
                    },
                    state.currentAnchor,
                )
            },
    ) {
        // Layer 1: hero/map content — full size of the container.
        background()

        // Layer 2: the sheet itself, offset from the top of the container.
        // No anchoredDraggable here — it competed with LazyColumn for gestures
        // and broke scroll-to-expand.  Direct drag is handled by DragHandle.
        // No clip(RoundedCornerShape) — it left visible map gaps at screen edges.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { state.containerHeightPx.toDp() }.coerceAtLeast(1.dp))
                .offset { IntOffset(0, state.sheetTopPx.roundToInt()) },
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(sheetScrollConnection),
            ) {
                DragHandle(state = state)
                sheetContent(state.currentAnchor)
            }
        }
    }
}

/**
 * Full-width drag handle row. The pill provides the visual affordance; the
 * [Modifier.draggable] on the entire row makes it easy to grab. On drag stop,
 * snaps to the nearest anchor using velocity + positional thresholds.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DragHandle(state: ThreeDetentSheetState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm, bottom = 6.dp)
            .draggable(
                state = rememberDraggableState { delta ->
                    state.draggableState.dispatchRawDelta(delta)
                },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    val offset   = state.draggableState.offset
                    val anchors  = state.draggableState.anchors
                    val peekOff  = anchors.positionOf(SheetAnchor.Peek)
                    val midOff   = anchors.positionOf(SheetAnchor.Mid)
                    val fullOff  = anchors.positionOf(SheetAnchor.Full)
                    val target = when {
                        velocity < -300f -> SheetAnchor.Full
                        velocity >  300f -> if (offset > (midOff + peekOff) / 2f) SheetAnchor.Peek else SheetAnchor.Mid
                        offset < (fullOff + midOff) / 2f -> SheetAnchor.Full
                        offset < (midOff  + peekOff) / 2f -> SheetAnchor.Mid
                        else -> SheetAnchor.Peek
                    }
                    state.animateTo(target)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            modifier = Modifier
                .width(40.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outline),
        )
    }
}
