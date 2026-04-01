package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
data class PlanDay(
    val date: String,   // YYYY-MM-DD
    val workout: Workout,
    val notes: String? = null
)

@Serializable
data class PlanWeekResponse(
    val plan_version_id: String,
    val start_date: String,
    val days: List<PlanDay>,
    val phase_label: String? = null,
    val coaching_notes: String? = null,
    val rationale_bullets: List<String>? = null,
    val warning_flags: List<String>? = null
)

@Serializable
data class PlanVersionWeek(
    val week_number: Int,
    val start_date: String,
    val days: List<PlanDay>
)

@Serializable
data class ConfirmPlanResponse(
    val confirmed: Boolean,
    val plan_version_id: String
)
