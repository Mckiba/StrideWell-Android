package com.stridewell.app.model

import kotlinx.serialization.Serializable

/** Response from GET /runs/summary. Dates are local to the user's timezone. */
@Serializable
data class RunSummaryResponse(
    val range: String,          // week | month | year | all
    val start: String,          // YYYY-MM-DD, inclusive
    val end: String,            // YYYY-MM-DD, exclusive
    val totals: RunSummaryTotals,
    val buckets: List<RunSummaryBucket>,
    val first_run_date: String? = null,
)

@Serializable
data class RunSummaryTotals(
    val distance_m: Double,
    val run_count: Int,
    val duration_s: Int,
    val avg_pace_s_per_km: Double? = null,
)

@Serializable
data class RunSummaryBucket(
    val key: String,            // YYYY-MM-DD, YYYY-MM, or YYYY
    val distance_m: Double,
    val run_count: Int,
    val run_id: String? = null, // set only when run_count == 1
)
