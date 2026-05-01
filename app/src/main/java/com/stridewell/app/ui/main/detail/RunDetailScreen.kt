package com.stridewell.app.ui.main.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.ui.components.PolylineDecoder
import com.stridewell.app.ui.components.RoutePoint
import com.stridewell.app.ui.components.skeleton.MapAreaSkeleton
import com.stridewell.app.ui.theme.Spacing

/**
 * Run Detail screen — full-bleed Mapbox map under a three-detent draggable sheet.
 *
 * Layout (back to front):
 *   1. [RouteMapView] (or [MapAreaSkeleton] / black fallback)
 *   2. [ThreeDetentSheet] hosting header / splits / stats / analysis / charts
 *   3. Floating close button overlaid in the top-leading corner
 *
 * Mirrors iOS `RunDetailScreen` (`Views/Main/RunDetailScreen.swift`).
 */
@Composable
fun RunDetailScreen(
    runId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberThreeDetentSheetState(initial = SheetAnchor.Mid)
    val lazyListState = rememberLazyListState()

    LaunchedEffect(runId) { viewModel.load(runId) }

    val detail = (uiState.screenState as? RunDetailViewModel.DetailScreenState.Loaded)?.detail
    val coordinates: List<RoutePoint> = remember(detail?.run?.route?.summary_polyline) {
        detail?.run?.route?.summary_polyline?.takeIf { it.isNotEmpty() }
            ?.let(PolylineDecoder::decode)
            ?: emptyList()
    }
    val startLatLng: RoutePoint? = remember(detail?.run?.start_latlng) {
        detail?.run?.start_latlng?.takeIf { it.size >= 2 }
            ?.let { RoutePoint(latitude = it[0], longitude = it[1]) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ThreeDetentSheet(
            state = sheetState,
            onPeekBack = onBack,
            // Collapse when dragging down at Full and the list is already at the top.
            // Read canScrollBackward here (in the composition) so the lambda captures
            // the live State snapshot rather than a stale copy.
            isContentAtTop = { !lazyListState.canScrollBackward },
            background = {
                MapBackground(
                    state = uiState.screenState,
                    coordinates = coordinates,
                    startLatLng = startLatLng,
                    bottomInsetPx = sheetState.bottomInsetPx,
                )
            },
            sheetContent = { _ ->
                RunDetailSheetBody(
                    state = uiState.screenState,
                    unit = uiState.unitSystem,
                    onRetry = viewModel::retry,
                    lazyListState = lazyListState,
                )
            },
        )

        CloseButton(onBack = onBack)
    }
}

// ───────────────────────────────────────────────────────────────────────────
// Map background layer
// ───────────────────────────────────────────────────────────────────────────

@Composable
private fun BoxScope.MapBackground(
    state: RunDetailViewModel.DetailScreenState,
    coordinates: List<RoutePoint>,
    startLatLng: RoutePoint?,
    bottomInsetPx: Float,
) {
    when (state) {
        RunDetailViewModel.DetailScreenState.Loading ->
            MapAreaSkeleton(modifier = Modifier.fillMaxSize())

        is RunDetailViewModel.DetailScreenState.Loaded ->
            if (coordinates.isNotEmpty()) {
                RouteMapView(
                    coordinates = coordinates,
                    startLatLng = startLatLng,
                    bottomInsetPx = bottomInsetPx,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }

        is RunDetailViewModel.DetailScreenState.Error ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
    }
}

// ───────────────────────────────────────────────────────────────────────────
// Floating close button
// ───────────────────────────────────────────────────────────────────────────

@Composable
private fun BoxScope.CloseButton(onBack: () -> Unit) {
    val statusBar = WindowInsets.statusBars.asPaddingValues()
    Surface(
        modifier = Modifier
            .padding(top = statusBar.calculateTopPadding() + 8.dp, start = Spacing.md)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = 4.dp,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
