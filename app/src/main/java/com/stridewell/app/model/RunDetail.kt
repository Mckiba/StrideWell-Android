package com.stridewell.app.model

import kotlinx.serialization.Serializable

/**
 * Full Run detail returned by `GET /runs/:id`.
 *
 * Superset of [Run] with description, timezone, route metadata, lap-derived totals,
 * elapsed/moving time split, fastest pace, max HR/cadence, power, and elevation loss.
 *
 * Mirrors iOS `RunDetail` in `Models/RunDetailModels.swift`.
 */
@Serializable
data class RunDetail(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val provider: String,
    val sport_type: String,
    val start_time: String,
    val timezone: String? = null,
    val start_latlng: List<Double>? = null,
    val distance_m: Double,
    val duration_s: Int,
    val moving_time_s: Int? = null,
    val elapsed_time_s: Int? = null,
    val avg_pace_s_per_km: Double? = null,
    val best_pace_s_per_km: Double? = null,
    val avg_hr_bpm: Int? = null,
    val max_hr_bpm: Int? = null,
    val avg_cadence_spm: Int? = null,
    val max_cadence_spm: Int? = null,
    val avg_power_w: Int? = null,
    val calories_kcal: Int? = null,
    val elevation_gain_m: Double? = null,
    val elevation_loss_m: Double? = null,
    val route: RunRoute? = null
)

/**
 * Per-mile (or per-km) split. `source` is `"lap"` when the row comes from
 * Strava lap data, or `"computed"` when derived from streams.
 */
@Serializable
data class RunSplit(
    val index: Int,
    val distance_m: Double,
    val duration_s: Int,
    val avg_pace_s_per_km: Double,
    val avg_hr_bpm: Int? = null,
    val avg_cadence_spm: Int? = null,
    val elevation_gain_m: Double? = null,
    val source: String
)

/**
 * Down-sampled (≤500 points) parallel arrays of metrics over distance.
 * `distance_m` is the index axis; remaining arrays are aligned to it.
 * Any of the metric arrays may be null when the underlying device didn't record them.
 */
@Serializable
data class RunStreams(
    val distance_m: List<Double>,
    val altitude_m: List<Double>? = null,
    val heartrate: List<Double>? = null,
    val cadence: List<Double>? = null
)

@Serializable
data class RunDetailResponse(
    val run: RunDetail,
    val splits: List<RunSplit> = emptyList(),
    val streams: RunStreams? = null
)
