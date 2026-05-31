package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
data class RunRoute(
    val summary_polyline: String? = null
)

@Serializable
data class Run(
    val id: String,
    val provider: String,
    val sport_type: String,
    val title: String? = null,
    val start_time: String,           // ISO 8601
    val distance_m: Double,
    val duration_s: Int,
    val avg_pace_s_per_km: Double? = null,
    val elevation_gain_m: Double,
    val route: RunRoute? = null,
    // YYYY-MM-DD of the plan day this run fulfilled, or null for standalone runs
    // (historical, cross-training, outside the active plan). From /runs/recent.
    val plan_day_date: String? = null,
)

@Serializable
data class RecentRunsResponse(
    val runs: List<Run>,
    val hasMore: Boolean? = null
)
