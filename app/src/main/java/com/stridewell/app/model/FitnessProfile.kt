package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
data class HRRange(
    val min_bpm: Int,
    val max_bpm: Int
)

@Serializable
data class PaceZones(
    val recovery: PaceRange,
    val easy: PaceRange,
    val moderate: PaceRange,
    val tempo: PaceRange,
    val threshold: PaceRange,
    val interval: PaceRange,
    val repetition: PaceRange
)

@Serializable
data class HRZones(
    val max_hr_bpm: Int,
    val max_hr_source: String,
    val zone_1: HRRange,
    val zone_2: HRRange,
    val zone_3: HRRange,
    val zone_4: HRRange,
    val zone_5: HRRange
)

@Serializable
data class FitnessProfileHistoryEntry(
    val threshold_pace_s_per_km: Double,
    val date: String,
    val method: String
)

@Serializable
data class FitnessProfile(
    val estimated_threshold_pace_s_per_km: Double? = null,
    val estimation_method: String? = null,
    val estimation_date: String? = null,
    val estimation_source: String? = null,
    val confidence: String? = null,
    val pace_zones: PaceZones? = null,
    val hr_zones: HRZones? = null,
    val history: List<FitnessProfileHistoryEntry> = emptyList()
)
