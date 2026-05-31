package com.stridewell.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlanDay(
    val date: String,   // YYYY-MM-DD
    val workout: Workout,
    val notes: String? = null,

    // Completion state annotated at the API boundary. Optional so older cached
    // payloads still deserialise; null status renders as PLANNED.
    val status: PlanDayStatus? = null,
    @SerialName("run_id") val runId: String? = null,

    // For COMPLETED/MODIFIED days the API embeds the matched run inline so the
    // completed card renders the map + actual stats without a separate fetch.
    @SerialName("linked_run") val linkedRun: Run? = null,
)

@Serializable
enum class PlanDayStatus {
    @SerialName("planned")   PLANNED,
    @SerialName("completed") COMPLETED,
    @SerialName("missed")    MISSED,
    @SerialName("modified")  MODIFIED,
    @SerialName("rest")      REST,
}

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
