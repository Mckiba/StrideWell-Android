package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
data class BoundingBox(
    val min_lat: Double,
    val max_lat: Double,
    val min_lng: Double,
    val max_lng: Double
)

@Serializable
data class HeatmapResponse(
    val polylines: List<String>,
    val run_count: Int,
    val bounding_box: BoundingBox? = null
)
