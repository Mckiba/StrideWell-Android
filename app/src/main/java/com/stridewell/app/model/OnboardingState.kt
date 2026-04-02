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
    val conversation_id: String? = null
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
    val message: InterviewMessage
)

@Serializable
data class OnboardingMessageOnboardingState(
    val status: OnboardingStatus,
    val intake_complete: Boolean,
    val plan_building: Boolean,
    val first_plan_version_id: String? = null
)

@Serializable
data class OnboardingMessageResponse(
    val conversation_id: String,
    val reply: InterviewMessage,
    val onboarding_state: OnboardingMessageOnboardingState
)

@Serializable
data class ConfirmPlanRequest(
    val plan_version_id: String
)
