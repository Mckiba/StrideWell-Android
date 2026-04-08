package com.stridewell.app.ui.background.heatmap

import com.stridewell.app.data.GeoCoordinate
import com.stridewell.app.model.BoundingBox
import com.stridewell.app.ui.components.RoutePoint
import kotlin.math.abs

data class HeatmapRegion(
    val centerLat: Double,
    val centerLng: Double,
    val latDelta: Double,
    val lngDelta: Double
)

object RegionCalculator {
    // Smaller span => more zoomed in; larger span => more zoomed out.
    private const val LOCATION_SPAN = 0.01
    private const val PADDING_FACTOR = 1.15
    private const val MIN_SPAN = 0.01
    private const val OUTLIER_THRESHOLD_DEG = 0.45

    fun region(
        coordinateGroups: List<List<RoutePoint>>,
        userLocation: GeoCoordinate?,
        boundingBox: BoundingBox?
    ): HeatmapRegion? {
        if (userLocation != null) {
            return HeatmapRegion(
                centerLat = userLocation.latitude,
                centerLng = userLocation.longitude,
                latDelta = LOCATION_SPAN,
                lngDelta = LOCATION_SPAN
            )
        }
        return regionFromCoordinates(coordinateGroups, boundingBox)
    }

    private fun regionFromCoordinates(
        coordinateGroups: List<List<RoutePoint>>,
        boundingBox: BoundingBox?
    ): HeatmapRegion? {
        val localGroups = filterOutlierGroups(coordinateGroups)
        val allCoords = localGroups.flatten()
        if (allCoords.isEmpty()) return regionFromBoundingBox(boundingBox)

        val minLat = allCoords.minOf { it.latitude }
        val maxLat = allCoords.maxOf { it.latitude }
        val minLng = allCoords.minOf { it.longitude }
        val maxLng = allCoords.maxOf { it.longitude }

        return HeatmapRegion(
            centerLat = (minLat + maxLat) / 2.0,
            centerLng = (minLng + maxLng) / 2.0,
            latDelta = ((maxLat - minLat) * PADDING_FACTOR).coerceAtLeast(MIN_SPAN),
            lngDelta = ((maxLng - minLng) * PADDING_FACTOR).coerceAtLeast(MIN_SPAN)
        )
    }

    private fun filterOutlierGroups(groups: List<List<RoutePoint>>): List<List<RoutePoint>> {
        val starts = groups.mapNotNull { it.firstOrNull() }
        if (starts.size < 3) return groups

        val sortedLats = starts.map { it.latitude }.sorted()
        val sortedLngs = starts.map { it.longitude }.sorted()
        val mid = starts.size / 2
        val medianLat = sortedLats[mid]
        val medianLng = sortedLngs[mid]

        val filtered = groups.filter { group ->
            val start = group.firstOrNull() ?: return@filter false
            abs(start.latitude - medianLat) <= OUTLIER_THRESHOLD_DEG &&
                abs(start.longitude - medianLng) <= OUTLIER_THRESHOLD_DEG
        }
        return if (filtered.isEmpty()) groups else filtered
    }

    private fun regionFromBoundingBox(box: BoundingBox?): HeatmapRegion? {
        if (box == null) return null
        return HeatmapRegion(
            centerLat = (box.min_lat + box.max_lat) / 2.0,
            centerLng = (box.min_lng + box.max_lng) / 2.0,
            latDelta = ((box.max_lat - box.min_lat) * PADDING_FACTOR).coerceAtLeast(MIN_SPAN),
            lngDelta = ((box.max_lng - box.min_lng) * PADDING_FACTOR).coerceAtLeast(MIN_SPAN)
        )
    }
}
