package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class DecisionTrigger {
    new_activity, missed_workout, reflection_submitted,
    user_requested_recalc, fatigue_flag, injury_flag, onboarding
}

@Serializable
data class SignalsUsed(
    val fatigue_trend: String? = null,
    val injury_risk_level: String? = null,
    val compliance_rate: Double? = null,
    val intake_confidence: String? = null,
    val strava_connected: Boolean? = null
)

@Serializable
data class DecisionRecord(
    val decision_id: String,
    val user_id: String,
    val created_at: String,
    val trigger: DecisionTrigger,
    val from_plan_version_id: String? = null,
    val to_plan_version_id: String,
    val diff_summary: List<String>,
    val rationale_bullets: List<String>,
    val signals_used: SignalsUsed? = null,
    val guardrails_applied: List<String>? = null
)

@Serializable
data class LatestDecisionResponse(
    val decision_record: DecisionRecord
)
