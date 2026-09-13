package com.stridewell.app.ui.background.heatmap

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay

@Composable
fun HeatmapBackgroundView(
    hasLocationPermission: Boolean,
    isDarkTheme: Boolean,
    heatmapViewModel: HeatmapViewModel,
    modifier: Modifier = Modifier
) {
    val state by heatmapViewModel.state.collectAsStateWithLifecycle()
    var renderSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(hasLocationPermission) {
        heatmapViewModel.setLocationPermissionGranted(hasLocationPermission)
    }

    LaunchedEffect(renderSize, isDarkTheme) {
        if (renderSize != IntSize.Zero) {
            heatmapViewModel.setRenderSize(renderSize)
            // Give the location request a moment to land. Without this the first
            // render races the async fix and frames the map on every run the user
            // has ever done — the whole city — and then caches that.
            var waited = 0
            while (heatmapViewModel.coordinate.value == null && waited < LOCATION_WAIT_ATTEMPTS) {
                delay(LOCATION_WAIT_INTERVAL_MS)
                waited++
            }
            heatmapViewModel.loadIfNeeded(isDark = isDarkTheme)
        }
    }

    // A fix can still arrive after the grace period (cold start, slow GPS). Re-render
    // once when it does, so the user is not left looking at the city-wide fallback.
    val coordinate by heatmapViewModel.coordinate.collectAsStateWithLifecycle()
    LaunchedEffect(coordinate, state) {
        // Only over a finished render, otherwise this races the initial load and
        // cancels it - both paths call generate(), which cancels the previous job.
        if (coordinate != null &&
            state is HeatmapState.Ready &&
            renderSize != IntSize.Zero &&
            heatmapViewModel.needsLocationRegeneration()
        ) {
            heatmapViewModel.invalidateAndRegenerate(isDark = isDarkTheme)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { renderSize = it }
    ) {
        when (val current = state) {
            HeatmapState.Idle,
            HeatmapState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
            HeatmapState.Insufficient,
            is HeatmapState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0D1117), Color(0xFF1A1F2E))
                            )
                        )
                )
            }
            is HeatmapState.Ready -> {
                val bitmap = remember(current.imageBytes) {
                    BitmapFactory.decodeByteArray(current.imageBytes, 0, current.imageBytes.size)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            // Dark matches iOS at 0.88. Light is deliberately lighter than
                            // the iOS 0.85: the Mapbox basemap is far paler than MapKit's, and
                            // a heavier scrim washes out the park and water fills entirely.
                            color = if (isDarkTheme) Color(0xFF121212).copy(alpha = 0.88f)
                            else Color.White.copy(alpha = 0.62f)
                        )
                )
            }
        }
    }
}

/** Matches the iOS grace period of 15 x 100ms before the first heatmap render. */
private const val LOCATION_WAIT_ATTEMPTS = 15
private const val LOCATION_WAIT_INTERVAL_MS = 100L
