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
            heatmapViewModel.loadIfNeeded(isDark = isDarkTheme)
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
                            color = if (isDarkTheme) Color(0xFF121212).copy(alpha = 0.88f)
                            else Color.White.copy(alpha = 0.85f)
                        )
                )
            }
        }
    }
}
