package com.stridewell.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.Workout
import com.stridewell.app.model.WorkoutIntensity
import com.stridewell.app.model.WorkoutType
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

@Composable
fun WorkoutDetailSheet(
    day: PlanDay,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = DateUtils.planDayDate(day.date),
                style = detailHeaderStyle(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = day.workout.label,
                style = detailTitleStyle(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = day.workout.type.name.replace("_", " ").replaceFirstChar { it.uppercase() },
            style = TextStyle(
                fontFamily = SofiaSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )

        day.workout.description?.let {
            DetailCard { Text(it, color = MaterialTheme.colorScheme.onSurface) }
        }

        if (day.workout.target_distance_m != null || day.workout.target_pace_s_per_km != null || day.workout.target_duration_s != null) {
            DetailCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Targets", style = detailSectionStyle(), color = MaterialTheme.colorScheme.onSurface)
                    day.workout.target_distance_m?.let { DetailRow("Distance", FormatUtils.distance(it, unitSystem)) }
                    day.workout.target_pace_s_per_km?.let { DetailRow("Pace", FormatUtils.pace(it, unitSystem)) }
                    day.workout.target_duration_s?.let { DetailRow("Duration", FormatUtils.duration(it)) }
                }
            }
        }

        day.workout.intensity?.let {
            DetailCard { DetailRow("Intensity", it.name.replace("_", " ").replaceFirstChar { c -> c.uppercase() }) }
        }

        day.notes?.let {
            DetailCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Day Notes", style = detailSectionStyle(), color = MaterialTheme.colorScheme.onSurface)
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        day.workout.notes?.let {
            DetailCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Workout Notes", style = detailSectionStyle(), color = MaterialTheme.colorScheme.onSurface)
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(CornerRadius.md),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun detailHeaderStyle() = TextStyle(fontFamily = SofiaSansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
private fun detailTitleStyle() = TextStyle(fontFamily = SofiaSansFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp)
private fun detailSectionStyle() = TextStyle(fontFamily = SofiaSansFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)

@Preview(showBackground = true)
@Composable
private fun WorkoutDetailSheetPreview() {
    StridewellTheme {
        WorkoutDetailSheet(
            day = PlanDay(
                date = "2026-04-06",
                workout = Workout(
                    type = WorkoutType.tempo,
                    label = "Tempo Run",
                    description = "Hold steady tempo effort after a smooth warm-up.",
                    target_distance_m = 10000.0,
                    target_duration_s = 3200,
                    target_pace_s_per_km = 320.0,
                    intensity = WorkoutIntensity.hard,
                    notes = "Focus on relaxed shoulders."
                ),
                notes = "Keep the route flat if possible."
            ),
            unitSystem = UnitSystem.METRIC
        )
    }
}
