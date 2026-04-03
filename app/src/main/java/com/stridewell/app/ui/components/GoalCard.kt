package com.stridewell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.stridewell.app.model.GoalSummary
import com.stridewell.app.model.formattedRaceDate
import com.stridewell.app.model.goalName
import com.stridewell.app.model.totalWeeks
import com.stridewell.app.model.weeksCompleted
import com.stridewell.app.ui.theme.CardSurfaceDark
import com.stridewell.app.ui.theme.CardSurfaceLight
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.ProgressTrackEmptyDark
import com.stridewell.app.ui.theme.ProgressTrackEmptyLight
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

@Composable
fun GoalCard(
    summary: GoalSummary,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    val cardColor = if (isSystemInDarkTheme()) CardSurfaceDark else CardSurfaceLight
    val progressTrack = if (isSystemInDarkTheme()) ProgressTrackEmptyDark else ProgressTrackEmptyLight
    val totalWeeks = maxOf(summary.totalWeeks(), 1)
    val weeksCompleted = summary.weeksCompleted()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(CornerRadius.input),
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.24f)
            )
            .background(
                color = cardColor,
                shape = RoundedCornerShape(CornerRadius.input)
            )
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = summary.goalName(),
            style = TextStyle(
                fontFamily = SofiaSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 28.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        summary.formattedRaceDate()?.let { raceDate ->
            Text(
                text = raceDate,
                style = TextStyle(
                    fontFamily = SofiaSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(totalWeeks) { index ->
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            color = if (index < weeksCompleted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                progressTrack
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                if (index != totalWeeks - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "Weeks Completed",
                    style = goalStatLabelStyle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$weeksCompleted/$totalWeeks",
                    style = goalStatValueStyle(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = "Distance Completed",
                    style = goalStatLabelStyle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatUtils.distance(summary.total_distance_m, unitSystem),
                    style = goalStatValueStyle(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun goalStatLabelStyle() = TextStyle(
    fontFamily = SofiaSansFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

private fun goalStatValueStyle() = TextStyle(
    fontFamily = SofiaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 24.sp
)

@Preview(showBackground = true)
@Composable
private fun GoalCardPreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            GoalCard(
                summary = GoalSummary(
                    goal_race_date = "2026-10-04",
                    goal_race_distance_m = 21097.5,
                    plan_start_date = "2026-08-03",
                    horizon_days = 63,
                    total_distance_m = 459027.8
                ),
                unitSystem = UnitSystem.IMPERIAL
            )
            GoalCard(
                summary = GoalSummary(
                    goal_race_date = null,
                    goal_race_distance_m = null,
                    plan_start_date = "2026-08-03",
                    horizon_days = 42,
                    total_distance_m = 128748.0
                ),
                unitSystem = UnitSystem.METRIC
            )
        }
    }
}
