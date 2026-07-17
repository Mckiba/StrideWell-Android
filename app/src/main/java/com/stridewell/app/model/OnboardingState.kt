package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class OnboardingStatus {
    pending, analyzing, interview, complete, skipped
}

@Serializable
data class OnboardingState(
    val status: OnboardingStatus,
    val strava_connected: Boolean,
    val intake_complete: Boolean,
    val first_plan_version_id: String? = null,
    val conversation_id: String? = null,
    // V2 guided flow — optional so responses from a backend that omits them still decode.
    val history_summary: StravaHistorySummary? = null,   // present when Strava is connected
    val confirmed_fields: List<String>? = null,          // intake keys confirmed so far; drives which screen shows next
    val partial_intake: PartialIntake? = null            // confirmed values so far; pre-fills structured controls
)

@Serializable
data class OnboardingStartResponse(
    val status: OnboardingStatus,
    val strava_connected: Boolean,
    val conversation_id: String
)

@Serializable
data class StravaConnectRequest(
    val code: String
)

@Serializable
data class StravaConnectResponse(
    val connected: Boolean
)

@Serializable
enum class InterviewMessageRole {
    user, assistant
}

@Serializable
data class InterviewMessage(
    val id: String,
    val role: InterviewMessageRole,
    val content: String,
    val agent_used: String? = null,
    val created_at: String
)

@Serializable
data class OnboardingMessageRequest(
    val conversation_id: String,
    val message: InterviewMessage,
    // V2 guided flow — both optional; encodeDefaults=false omits nulls, so the body
    // matches a plain free-text turn when unset.
    val screen_context: String? = null,
    val structured_fields: StructuredFields? = null
)

@Serializable
data class OnboardingMessageOnboardingState(
    val status: OnboardingStatus,
    val intake_complete: Boolean,
    val plan_building: Boolean,
    val first_plan_version_id: String? = null,
    val confirmed_fields: List<String>? = null   // intake keys confirmed so far
)

@Serializable
data class OnboardingMessageResponse(
    val conversation_id: String,
    val reply: InterviewMessage,
    val onboarding_state: OnboardingMessageOnboardingState
)

@Serializable
data class OnboardingHistoryResponse(
    val messages: List<InterviewMessage>,
    val has_more: Boolean
)

@Serializable
data class ConfirmPlanRequest(
    val plan_version_id: String
)

// POST /onboarding/skip → 202
@Serializable
data class OnboardingSkipResponse(
    val queued: Boolean,
    val message: String? = null
)

// ── V2 guided flow ─────────────────────────────────────────────────────────────

/**
 * Intake values a guided control submits directly instead of relying on the model to
 * re-read them from the message text. Every field is nullable; `encodeDefaults = false`
 * (set on [com.stridewell.app.api.appJson]) omits the ones left null, so the body only
 * carries the keys that were set — matching the backend's `additionalProperties: false`
 * whitelist. `active_injury` is deliberately excluded (it comes through conversation).
 */
@Serializable
data class StructuredFields(
    val current_weekly_volume_km: Double? = null,
    val training_phase: String? = null,
    val goal_type: String? = null,
    val goal_race_date: String? = null,          // YYYY-MM-DD
    val goal_race_distance_m: Double? = null,
    val goal_time_seconds: Int? = null,
    val available_days_per_week: Int? = null,
    val available_day_names: List<String>? = null,
    val preferred_long_run_day: String? = null,
    val rest_day_constraints: List<String>? = null,
    val has_done_speedwork: Boolean? = null,
    val following_existing_plan: Boolean? = null
) {
    /** True when no field is set — lets call sites avoid sending an empty object. */
    val isEmpty: Boolean
        get() = current_weekly_volume_km == null && training_phase == null && goal_type == null &&
            goal_race_date == null && goal_race_distance_m == null && goal_time_seconds == null &&
            available_days_per_week == null && available_day_names == null && preferred_long_run_day == null &&
            rest_day_constraints == null && has_done_speedwork == null && following_existing_plan == null
}

/**
 * The values confirmed so far, echoed by the status endpoint. Every field is optional so
 * any subset decodes; each screen reads the keys it owns to pre-fill its controls. Mirrors
 * [StructuredFields] plus `active_injury` (which structured fields lack).
 */
@Serializable
data class PartialIntake(
    val current_weekly_volume_km: Double? = null,
    val training_phase: String? = null,
    val active_injury: Boolean? = null,
    val goal_type: String? = null,
    val goal_race_date: String? = null,
    val goal_race_distance_m: Double? = null,
    val goal_time_seconds: Int? = null,
    val available_days_per_week: Int? = null,
    val available_day_names: List<String>? = null,
    val preferred_long_run_day: String? = null,
    val rest_day_constraints: List<String>? = null,
    val has_done_speedwork: Boolean? = null,
    val following_existing_plan: Boolean? = null
)

/**
 * One week in the volume-history series. Values are in km; the UI converts to the
 * athlete's display unit.
 */
@Serializable
data class HistoryWeekVolume(
    val week_start: String,   // YYYY-MM-DD, Monday of the week
    val volume_km: Double
)

/**
 * Computed training history echoed by the status endpoint when Strava is connected.
 * Only the fields the app displays or branches on are modeled; unknown keys are ignored.
 */
@Serializable
data class StravaHistorySummary(
    val avg_weekly_volume_km_4wk: Double? = null,
    val avg_weekly_volume_km_12wk: Double? = null,
    val peak_weekly_volume_km_12wk: Double? = null,
    val recent_long_run_m: Double? = null,
    val avg_runs_per_week_4wk: Double? = null,
    val consistency_rate_12wk: Double? = null,
    val has_speed_work: Boolean? = null,
    val inferred_training_phase: String? = null,   // may be "insufficient_data" → route to manual baseline
    val volume_trend: String? = null,
    // Last 12 weeks, oldest first. Optional: summaries stored before this field existed
    // decode without it and the card falls back to stats only.
    val weekly_volumes: List<HistoryWeekVolume>? = null
)
