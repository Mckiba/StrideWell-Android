package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
data class SorenessEntry(
    val location: String,
    val score: Int  // 1–10
)

@Serializable
data class Reflection(
    val user_id: String,
    val submitted_at: String,
    val fatigue: Int,        // 1–10
    val sleep_quality: Int,  // 1–10
    val mood: Int,           // 3, 5, or 8
    val soreness: List<SorenessEntry>? = null,
    val free_text: String? = null,
    val related_run_id: String? = null,
    val constraints: List<String>? = null
)

@Serializable
data class ReflectionSubmission(
    val fatigue: Int,
    val sleep_quality: Int,
    val mood: Int,
    val soreness: List<SorenessEntry>? = null,
    val free_text: String? = null,
    val related_run_id: String? = null
)

@Serializable
data class ReflectionResponse(
    val stored: Boolean,
    val reflection_id: String
)
