package com.stridewell.app.model

import com.stridewell.app.util.DateUtils
import java.util.Calendar
import java.util.Date
import kotlinx.serialization.Serializable
import kotlin.math.ceil

@Serializable
data class GoalSummary(
    val goal_race_date: String?,
    val goal_race_distance_m: Double?,
    val plan_start_date: String,
    val horizon_days: Int,
    val total_distance_m: Double
)

fun GoalSummary.goalName(): String {
    val distance = goal_race_distance_m ?: return "Training Goal"
    return when {
        distance < 5500 -> "5K"
        distance < 9000 -> "8K"
        distance < 11000 -> "10K"
        distance < 16000 -> "15K"
        distance < 19000 -> "10 Mile"
        distance < 23000 -> "Half Marathon"
        distance < 38000 -> "30K"
        else -> "Marathon"
    }
}

fun GoalSummary.totalWeeks(): Int =
    ceil(horizon_days / 7.0).toInt()

fun GoalSummary.weeksCompleted(now: Date = Date()): Int {
    val start = DateUtils.parse(plan_start_date) ?: return 0
    val daysElapsed = ((now.time - start.time) / (24L * 60L * 60L * 1000L)).toInt() / 7
    return daysElapsed.coerceIn(0, totalWeeks())
}

fun GoalSummary.formattedRaceDate(): String? {
    val date = goal_race_date?.let(DateUtils::parse) ?: return null
    val calendar = Calendar.getInstance().apply { time = date }
    val month = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(date)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "$month ${ordinal(day)}"
}

private fun ordinal(day: Int): String {
    if (day in 11..13) return "${day}th"
    return when (day % 10) {
        1 -> "${day}st"
        2 -> "${day}nd"
        3 -> "${day}rd"
        else -> "${day}th"
    }
}
