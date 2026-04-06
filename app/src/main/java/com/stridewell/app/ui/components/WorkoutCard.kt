package com.stridewell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.Workout
import com.stridewell.app.model.WorkoutIntensity
import com.stridewell.app.model.WorkoutType
import com.stridewell.app.ui.theme.ActivityNameStyle
import com.stridewell.app.ui.theme.ActivityStatLabelStyle
import com.stridewell.app.ui.theme.ActivityTimestampStyle
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

@Composable
fun WorkoutCard(
    day: PlanDay,
    unitSystem: UnitSystem,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(CornerRadius.md),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .background(
                color = cardColor,
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = day.workout.label,
                style = ActivityNameStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = cardDate(day.date),
                style = ActivityTimestampStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        metricLine(day.workout, unitSystem)?.let { metric ->
            Text(
                text = metric,
                style = ActivityStatLabelStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        notesLine(day.workout)?.let { notes ->
            Text(
                text = notes,
                style = ActivityStatLabelStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun metricLine(workout: Workout, unitSystem: UnitSystem): String? {
    val isRest = workout.type == WorkoutType.rest || workout.type == WorkoutType.recovery
    if (isRest) return null

    val parts = buildList {
        workout.target_distance_m?.let { add(FormatUtils.formatDistance(it, unitSystem)) }
        when {
            workout.target_pace_s_per_km != null -> add(FormatUtils.formatPace(workout.target_pace_s_per_km, unitSystem))
            workout.target_duration_s != null -> add(FormatUtils.formatDuration(workout.target_duration_s))
        }
    }

    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

private fun notesLine(workout: Workout): String? =
    workout.notes ?: workout.description

private fun cardDate(date: String): String {
    val parsed = DateUtils.parse(date) ?: return date
    return DateUtils.workoutCardDateFormatter.format(parsed)
}

@Preview(showBackground = true)
@Composable
private fun WorkoutCardPreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            WorkoutCard(
                day = PlanDay(
                    date = "2026-04-06",
                    workout = Workout(
                        type = WorkoutType.easy,
                        label = "Easy Run",
                        description = "Keep the effort relaxed.",
                        target_distance_m = 8000.0,
                        target_pace_s_per_km = 360.0,
                        intensity = WorkoutIntensity.easy
                    )
                ),
                unitSystem = UnitSystem.METRIC
            )
            WorkoutCard(
                day = PlanDay(
                    date = "2026-04-07",
                    workout = Workout(
                        type = WorkoutType.rest,
                        label = "Rest Day",
                        notes = "Walk or stretch if it feels good."
                    )
                ),
                unitSystem = UnitSystem.METRIC
            )
        }
    }
}
