package com.stridewell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.Run
import com.stridewell.app.model.Workout
import com.stridewell.app.model.WorkoutType
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem
import java.util.Date

@Composable
fun WeekOverviewCard(
    days: List<PlanDay>,
    weekRuns: List<Run>,
    monday: Date,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    val cardColor = MaterialTheme.colorScheme.surface
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val workoutDays = days.filter { it.workout.type != WorkoutType.rest }
    val plannedDistance = workoutDays.mapNotNull { it.workout.target_distance_m }.sum()
    val completedDistance = weekRuns.sumOf { it.distance_m }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(CornerRadius.md), ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.10f))
            .background(cardColor, RoundedCornerShape(CornerRadius.md))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row {
            Text(
                text = "Overview",
                style = overviewTitleStyle(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = DateUtils.weekRangeLabel(monday),
                style = overviewSubtitleStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (workoutDays.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.fillMaxWidth()) {
                workoutDays.forEach { day ->
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                color = if (weekRuns.any { run ->
                                    DateUtils.parseISO8601(run.start_time)?.let(DateUtils::format) == day.date
                                }) MaterialTheme.colorScheme.primary else emptyColor,
                                shape = RoundedCornerShape(100.dp)
                            )
                    )
                }
            }
        }

        Row {
            MetricColumn(
                label = "WORKOUTS",
                value = "${weekRuns.count()}/${workoutDays.count()}"
            )
            Spacer(modifier = Modifier.weight(1f))
            MetricColumn(
                label = "DISTANCE",
                value = weekDistanceLabel(completedDistance, plannedDistance, unitSystem)
            )
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = SofiaSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = TextStyle(
                fontFamily = SofiaSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun weekDistanceLabel(actualMeters: Double, plannedMeters: Double, unitSystem: UnitSystem): String {
    val divisor = if (unitSystem == UnitSystem.IMPERIAL) 1609.344 else 1000.0
    val actualValue = actualMeters / divisor
    val actualString = if (actualValue < 0.05) "0" else String.format("%.1f", actualValue)
    return "$actualString /${FormatUtils.distance(plannedMeters, unitSystem)}"
}

private fun overviewTitleStyle() = TextStyle(fontFamily = SofiaSansFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
private fun overviewSubtitleStyle() = TextStyle(fontFamily = SofiaSansFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp)

@Preview(showBackground = true)
@Composable
private fun WeekOverviewCardPreview() {
    StridewellTheme {
        WeekOverviewCard(
            days = listOf(
                PlanDay("2026-04-06", Workout(WorkoutType.easy, "Easy Run", target_distance_m = 8000.0)),
                PlanDay("2026-04-07", Workout(WorkoutType.rest, "Rest")),
                PlanDay("2026-04-08", Workout(WorkoutType.long_run, "Long Run", target_distance_m = 16000.0))
            ),
            weekRuns = listOf(
                Run("1", "strava", "Run", "Morning Run", "2026-04-06T07:30:00Z", 8200.0, 2600, 317.0, 45.0, null)
            ),
            monday = DateUtils.parse("2026-04-06") ?: Date(),
            unitSystem = UnitSystem.IMPERIAL,
            modifier = Modifier.padding(Spacing.md)
        )
    }
}
