package com.stridewell.app.model

import kotlinx.serialization.Serializable

// ── Execution Analysis ──────────────────────────────────────────────────────

@Serializable
data class PaceConsistency(
    val coefficient_of_variation: Double,
    val classification: String,
    val split_profile: String,
    val first_half_avg_pace_s_per_km: Double,
    val second_half_avg_pace_s_per_km: Double,
    val pace_delta_s: Double
)

@Serializable
data class ExecutionSegment(
    val segment_index: Int,
    val distance_m: Double,
    val pace_s_per_km: Double,
    val hr_bpm: Int? = null,
    val elevation_delta_m: Double? = null
)

@Serializable
data class ExecutionQuality(
    val score: String,
    val factors: List<String>
)

@Serializable
data class ExecutionAnalysis(
    val pace_consistency: PaceConsistency,
    val segments: List<ExecutionSegment>,
    val execution_quality: ExecutionQuality? = null,
    val stopped_time_s: Int? = null,
    val stop_ratio: Double? = null
)

// ── HR Analysis ─────────────────────────────────────────────────────────────

@Serializable
data class CardiacDrift(
    val first_half_avg_hr: Int,
    val second_half_avg_hr: Int,
    val drift_bpm: Int,
    val drift_pct: Double,
    val significant: Boolean,
    val interpretation: String
)

@Serializable
data class HREfficiency(
    val pace_per_hr_beat: Double,
    val vs_recent_avg: Double? = null,
    val trend: String
)

@Serializable
data class HRZoneDistribution(
    val zone_1_pct: Double,
    val zone_2_pct: Double,
    val zone_3_pct: Double,
    val zone_4_pct: Double,
    val zone_5_pct: Double,
    val primary_zone: Int,
    val appropriate_for_workout: Boolean? = null,
    val zone_note: String? = null
)

@Serializable
data class HRAnalysis(
    val avg_hr_bpm: Int,
    val max_hr_bpm: Int? = null,
    val cardiac_drift: CardiacDrift? = null,
    val efficiency: HREfficiency? = null,
    val zone_distribution: HRZoneDistribution? = null
)

// ── Trend Context ───────────────────────────────────────────────────────────

@Serializable
data class WeeklyVolume(
    val current_week_km: Double,
    val previous_week_km: Double,
    val four_week_avg_km: Double,
    val trend: String
)

@Serializable
data class PaceTrendSample(
    val date: String,
    val avg_pace_s_per_km: Double,
    val distance_m: Double
)

@Serializable
data class PaceTrend(
    val workout_type: String,
    val last_6_runs: List<PaceTrendSample>,
    val trend: String,
    val trend_rate_s_per_km_per_week: Double? = null,
    val note: String? = null
)

@Serializable
data class LongRunSample(
    val date: String,
    val distance_m: Double,
    val avg_pace_s_per_km: Double
)

@Serializable
data class LongRunProgression(
    val last_4_long_runs: List<LongRunSample>,
    val distance_trend: String,
    val longest_recent_m: Double
)

@Serializable
data class Compliance(
    val planned: Int,
    val completed: Int,
    val rate: Double
)

@Serializable
data class TrendContext(
    val weekly_volume: WeeklyVolume,
    val pace_trend: PaceTrend,
    val long_run_progression: LongRunProgression? = null,
    val runs_this_week: Int,
    val runs_last_week: Int,
    val streak_days: Int,
    val compliance_last_14_days: Compliance
)

// ── Planned vs Actual ───────────────────────────────────────────────────────

@Serializable
data class PlannedVsActual(
    val distance_delta_m: Double? = null,
    val pace_delta_s_per_km: Double? = null,
    val duration_delta_s: Double? = null,
    val completed_as_planned: Boolean,
    val notes: String
)

// ── Top-level response from GET /runs/:id/analysis ──────────────────────────

@Serializable
data class RunAnalysisResponse(
    val run_id: String,
    val status: String? = null,
    val computed_at: String,
    val execution_analysis: ExecutionAnalysis? = null,
    val hr_analysis: HRAnalysis? = null,
    val trend_context: TrendContext? = null,
    val planned_vs_actual: PlannedVsActual? = null
)
