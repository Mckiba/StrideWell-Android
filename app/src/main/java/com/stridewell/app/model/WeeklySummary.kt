package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyLongRun(
    val date: String,
    val distance_m: Double,
    val pace_s_per_km: Double
)

@Serializable
data class WeeklyQualitySession(
    val date: String,
    val type: String,
    val execution_quality: String
)

@Serializable
data class WeeklySummary(
    val week_start: String,
    val total_distance_m: Double,
    val total_duration_s: Int,
    val run_count: Int,
    val planned_run_count: Int,
    val compliance_rate: Double,
    val avg_easy_pace_s_per_km: Double? = null,
    val long_run: WeeklyLongRun? = null,
    val quality_sessions: List<WeeklyQualitySession> = emptyList(),
    val fatigue_trend: String,
    val volume_vs_previous_week_pct: Double? = null
)
