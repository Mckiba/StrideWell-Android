package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlanSource {
    architect, adjuster, manual
}

@Serializable
data class PlanVersion(
    val plan_version_id: String,
    val user_id: String,
    val source: PlanSource,
    val start_date: String,
    val horizon_days: Int,
    val created_at: String,
    val days: List<PlanDay>,
    val phase_label: String? = null,
    val rationale_bullets: List<String>? = null,
    val coaching_notes: String? = null,
    val warning_flags: List<String>? = null
)

@Serializable
data class PlanVersionResponse(
    val plan_version_id: String,
    val source: PlanSource,
    val start_date: String,
    val horizon_days: Int,
    val phase_label: String? = null,
    val coaching_notes: String? = null,
    val rationale_bullets: List<String>? = null,
    val warning_flags: List<String>? = null,
    val weeks: List<PlanVersionWeek>
)
