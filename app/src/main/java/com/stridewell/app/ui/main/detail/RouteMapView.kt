package com.stridewell.app.ui.main.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.stridewell.app.ui.components.RoutePoint

/**
 * Mapbox 11 Compose wrapper rendering a single decoded route polyline with
 * start / finish markers, fit to bounds with the supplied bottom inset so the
 * route stays visible above the bottom sheet.
 *
 * Mirrors iOS `RouteMapView` (`Stridewell-iOS/.../Views/Shared/Components/`).
 *
 * @param coordinates decoded polyline coordinates (lat, lng)
 * @param startLatLng optional explicit start (otherwise uses first coordinate)
 * @param bottomInsetPx pixels reserved at the bottom of the map for the sheet —
 *   used both for camera fit padding and to push Mapbox attribution above the sheet
 */
@Composable
fun RouteMapView(
    coordinates: List<RoutePoint>,
    startLatLng: RoutePoint?,
    bottomInsetPx: Float,
    modifier: Modifier = Modifier,
) {
    if (coordinates.isEmpty()) return

    val points = remember(coordinates) {
        coordinates.map { Point.fromLngLat(it.longitude, it.latitude) }
    }
    val start = remember(startLatLng, coordinates) {
        startLatLng?.let { Point.fromLngLat(it.longitude, it.latitude) }
            ?: points.firstOrNull()
    }
    val finish = remember(points) { points.lastOrNull() }

    val mapViewportState = rememberMapViewportState()
    val ornamentBottomPadding = with(LocalDensity.current) { (bottomInsetPx + 8f).toDp() }

    // Re-fit the camera whenever the bottom inset changes (sheet drag) or the
    // route changes (new run loaded).
    LaunchedEffect(points, bottomInsetPx) {
        if (points.isNotEmpty()) {
            mapViewportState.setCameraOptions(
                com.mapbox.maps.CameraOptions.Builder()
                    .center(points[points.size / 2])
                    .zoom(13.5)
                    .padding(
                        EdgeInsets(
                            /* top    = */ 60.0,
                            /* left   = */ 24.0,
                            /* bottom = */ (bottomInsetPx + 24f).toDouble(),
                            /* right  = */ 24.0,
                        )
                    )
                    .build()
            )
        }
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState,
        compass = { },
        logo = {
            Logo(
                contentPadding = PaddingValues(
                    start = 8.dp,
                    bottom = ornamentBottomPadding,
                )
            )
        },
        attribution = {
            Attribution(
                contentPadding = PaddingValues(
                    start = 8.dp,
                    bottom = ornamentBottomPadding,
                )
            )
        },
        style = { MapStyle(style = STYLE_OUTDOORS) },
    ) {
        // White casing under the route so it stays legible on dark / satellite styles.
        PolylineAnnotation(points = points) {
            lineColor = ROUTE_CASING_COLOR
            lineWidth = ROUTE_CASING_WIDTH
        }
        // Accent route line on top.
        PolylineAnnotation(points = points) {
            lineColor = ROUTE_LINE_COLOR
            lineWidth = ROUTE_LINE_WIDTH
        }

        start?.let {
            CircleAnnotation(point = it) {
                circleColor = MARKER_START_COLOR
                circleRadius = MARKER_RADIUS
                circleStrokeColor = MARKER_STROKE_COLOR
                circleStrokeWidth = MARKER_STROKE_WIDTH
            }
        }

        finish?.let {
            CircleAnnotation(point = it) {
                circleColor = MARKER_FINISH_COLOR
                circleRadius = MARKER_RADIUS
                circleStrokeColor = MARKER_STROKE_COLOR
                circleStrokeWidth = MARKER_STROKE_WIDTH
            }
        }
    }
}

// MARK: - Style + color constants

private const val STYLE_OUTDOORS = "mapbox://styles/mapbox/outdoors-v12"

// Route line: same accent blue as iOS, white casing underneath for contrast.
private val ROUTE_LINE_COLOR = Color(0xFF285CE0)
private const val ROUTE_LINE_WIDTH = 4.0
private val ROUTE_CASING_COLOR = Color.White
private const val ROUTE_CASING_WIDTH = 7.0

// Markers (matches iOS): green start, blue finish, white stroke.
private val MARKER_START_COLOR = Color(0xFF2EBD6B)
private val MARKER_FINISH_COLOR = Color(0xFF285CE0)
private val MARKER_STROKE_COLOR = Color.White
private const val MARKER_RADIUS = 7.0
private const val MARKER_STROKE_WIDTH = 2.0
