package com.stridewell.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProactivePreferencesRequest(
    val enabled: Boolean,
    @SerialName("categories_enabled")
    val categoriesEnabled: ProactiveCategoriesEnabled,
    @SerialName("quiet_hours")
    val quietHours: ProactiveQuietHours,
    val timezone: String,
)

@Serializable
data class ProactiveCategoriesEnabled(
    @SerialName("training_milestone")
    val trainingMilestone: Boolean,
    @SerialName("training_concern")
    val trainingConcern: Boolean,
    @SerialName("upcoming_event")
    val upcomingEvent: Boolean,
    val reengagement: Boolean,
    @SerialName("plan_followup")
    val planFollowup: Boolean,
)

@Serializable
data class ProactiveQuietHours(
    val enabled: Boolean,
    @SerialName("start_local")
    val startLocal: String,
    @SerialName("end_local")
    val endLocal: String,
)

@Serializable
data class ProactivePreferencesStoredResponse(
    val stored: Boolean,
)
