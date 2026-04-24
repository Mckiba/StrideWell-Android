package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class WorkoutType {
    easy, long_run, tempo, intervals, hills, recovery, rest, cross_train, race
}

@Serializable
enum class WorkoutIntensity {
    very_easy, easy, moderate, hard, very_hard
}

/** V2 Phase 2 — min/max pace target expressed as seconds/km. */
@Serializable
data class PaceRange(
    val min_s_per_km: Double,
    val max_s_per_km: Double
)

@Serializable
data class Workout(
    val type: WorkoutType,
    val label: String,
    val description: String? = null,
    val target_distance_m: Double? = null,
    val target_duration_s: Int? = null,
    val target_pace_s_per_km: Double? = null,
    val target_pace_range: PaceRange? = null,
    val effort_level: String? = null,
    val intensity: WorkoutIntensity? = null,
    val notes: String? = null
)
