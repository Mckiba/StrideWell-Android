package com.stridewell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.PlanDayStatus
import com.stridewell.app.model.Run
import com.stridewell.app.model.RunRoute
import com.stridewell.app.model.Workout
import com.stridewell.app.model.WorkoutIntensity
import com.stridewell.app.model.WorkoutType
import com.stridewell.app.ui.theme.ActivityNameStyle
import com.stridewell.app.ui.theme.ActivityStatLabelStyle
import com.stridewell.app.ui.theme.ActivityStatValueStyle
import com.stridewell.app.ui.theme.ActivityTimestampStyle
import com.stridewell.app.ui.theme.CardBorderMissed
import com.stridewell.app.ui.theme.CardDivider
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.ui.theme.TextMissed
import com.stridewell.app.ui.theme.cardBottomStroke
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

/**
 * Plan-day card with visual states keyed on `day.status`.
 *
 *  • Planned / Rest — compact stacked layout, no bottom stroke.
 *  • Completed/Modified — route thumbnail + planned stats on top, divider,
 *    actual stats from `day.linkedRun` underneath, accent bottom stroke.
 *  • Missed — planned layout in grey, grey bottom stroke.
 */
@Composable
fun WorkoutCard(
    day: PlanDay,
    unitSystem: UnitSystem,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val effectiveStatus = remember(day.workout.type, day.status) {
        when {
            day.workout.type == WorkoutType.rest -> PlanDayStatus.REST
            else -> day.status ?: PlanDayStatus.PLANNED
        }
    }
    // Only states that carry meaning get a stroke; planned/rest stay plain.
    val strokeColor: Color? = when (effectiveStatus) {
        PlanDayStatus.COMPLETED, PlanDayStatus.MODIFIED -> MaterialTheme.colorScheme.primary
        PlanDayStatus.MISSED                            -> CardBorderMissed
        PlanDayStatus.PLANNED, PlanDayStatus.REST       -> null
    }
    val cardColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(CornerRadius.md),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.14f),
            )
            // Stroke layer first (outermost): a rounded rect in the stroke color
            // whose bottom 3dp peek out below the card surface as a curved band.
            // No-op when strokeColor is null (planned/rest).
            .cardBottomStroke(strokeColor, cornerRadius = CornerRadius.md)
            .background(color = cardColor, shape = RoundedCornerShape(CornerRadius.md))
            .clip(RoundedCornerShape(CornerRadius.md))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        when (effectiveStatus) {
            PlanDayStatus.COMPLETED, PlanDayStatus.MODIFIED -> {
                val linkedRun = day.linkedRun
                if (linkedRun != null) {
                    CompletedLayout(day = day, linkedRun = linkedRun, unitSystem = unitSystem)
                } else {
                    // Defensive: backend regression — fall back to planned-style.
                    PlannedLayout(day = day, unitSystem = unitSystem, textColor = MaterialTheme.colorScheme.onSurface)
                }
            }

            PlanDayStatus.MISSED -> {
                PlannedLayout(day = day, unitSystem = unitSystem, textColor = TextMissed)
            }

            PlanDayStatus.PLANNED, PlanDayStatus.REST -> {
                PlannedLayout(day = day, unitSystem = unitSystem, textColor = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ─── Planned / Missed / Rest layout ─────────────────────────────────────────

@Composable
private fun PlannedLayout(
    day: PlanDay,
    unitSystem: UnitSystem,
    textColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = day.workout.label,
            style = ActivityNameStyle,
            color = textColor,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = cardDate(day.date),
            style = ActivityTimestampStyle,
            color = textColor,
        )
    }

    if (!isRestOrRecovery(day.workout)) {
        PlannedStatsRow(workout = day.workout, unitSystem = unitSystem, textColor = textColor)
    }

    notesLine(day.workout)?.let { notes ->
        Text(
            text = notes,
            style = ActivityStatLabelStyle,
            color = textColor,
        )
    }
}

// ─── Completed / Modified layout ────────────────────────────────────────────

@Composable
private fun CompletedLayout(
    day: PlanDay,
    linkedRun: Run,
    unitSystem: UnitSystem,
) {
    val routeCoordinates = remember(linkedRun.route?.summary_polyline) {
        linkedRun.route?.summary_polyline
            ?.takeIf { it.isNotEmpty() }
            ?.let(PolylineDecoder::decode)
            .orEmpty()
    }
    val textColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        RouteThumbnail(
            coordinates = routeCoordinates,
            strokeColor = MaterialTheme.colorScheme.primary,
            placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            width = 55.dp,
            height = 55.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = day.workout.label,
                    style = ActivityNameStyle,
                    color = textColor,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = cardDate(day.date),
                    style = ActivityTimestampStyle,
                    color = textColor,
                )
            }
            PlannedStatsRow(workout = day.workout, unitSystem = unitSystem, textColor = textColor)
            notesLine(day.workout)?.let { notes ->
                Text(
                    text = notes,
                    style = ActivityStatLabelStyle,
                    color = textColor,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    // Divider between planned and actual.
    Spacer(
        modifier = Modifier
            .alpha(0.06f)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface),
    )

    ActualStatsRow(run = linkedRun, unitSystem = unitSystem)
}

// ─── Shared rows ────────────────────────────────────────────────────────────

@Composable
private fun PlannedStatsRow(
    workout: Workout,
    unitSystem: UnitSystem,
    textColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardStat(
            label = "DISTANCE",
            value = workout.target_distance_m
                ?.let { FormatUtils.formatDistance(it, unitSystem) }
                ?: "-",
            textColor = textColor,
        )
        Spacer(modifier = Modifier.weight(1f))
        CardStat(
            label = "TIME",
            value = workout.target_duration_s
                ?.let { FormatUtils.formatDuration(it) }
                ?: "-",
            textColor = textColor,
        )
        Spacer(modifier = Modifier.weight(1f))
        CardStat(
            label = "TARGET PACE",
            value = targetPaceValue(workout, unitSystem) ?: "-",
            textColor = textColor,
        )
    }
}

@Composable
private fun ActualStatsRow(
    run: Run,
    unitSystem: UnitSystem,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardStat(
            label = "DISTANCE",
            value = FormatUtils.formatDistance(run.distance_m, unitSystem),
            textColor = textColor,
        )
        Spacer(modifier = Modifier.weight(1f))
        CardStat(
            label = "TIME",
            value = FormatUtils.formatDuration(run.duration_s),
            textColor = textColor,
        )
        Spacer(modifier = Modifier.weight(1f))
        CardStat(
            label = "AVG PACE",
            value = run.avg_pace_s_per_km?.let { FormatUtils.formatPace(it, unitSystem) } ?: "—",
            textColor = textColor,
        )
    }
}

@Composable
private fun CardStat(label: String, value: String, textColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = ActivityStatLabelStyle,
            color = textColor,
            maxLines = 1,
        )
        Text(
            text = value,
            style = ActivityStatValueStyle,
            color = textColor,
            maxLines = 1,
        )
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun isRestOrRecovery(workout: Workout): Boolean =
    workout.type == WorkoutType.rest || workout.type == WorkoutType.recovery

private fun notesLine(workout: Workout): String? =
    workout.notes ?: workout.description

private fun cardDate(date: String): String {
    val parsed = DateUtils.parse(date) ?: return date
    return DateUtils.workoutCardDateFormatter.format(parsed)
}

private fun targetPaceValue(workout: Workout, unitSystem: UnitSystem): String? {
    workout.target_pace_range?.let { range ->
        return FormatUtils.formatPaceRange(range.min_s_per_km, range.max_s_per_km, unitSystem)
    }
    return workout.target_pace_s_per_km?.let { FormatUtils.formatPace(it, unitSystem) }
}

// ─── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun WorkoutCardPlannedPreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            WorkoutCard(
                day = PlanDay(
                    date = "2026-04-06",
                    workout = Workout(
                        type = WorkoutType.easy,
                        label = "Easy Run",
                        description = "Flat route is ideal",
                        target_distance_m = 8000.0,
                        target_duration_s = 2400,
                        target_pace_s_per_km = 360.0,
                        intensity = WorkoutIntensity.easy,
                        notes = "Try to keep hr below 150bpm",
                    ),
                    status = PlanDayStatus.PLANNED,
                ),
                unitSystem = UnitSystem.METRIC,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutCardCompletedPreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            WorkoutCard(
                day = PlanDay(
                    date = "2026-04-06",
                    workout = Workout(
                        type = WorkoutType.easy,
                        label = "Easy Run",
                        description = "Flat route is ideal",
                        target_distance_m = 8000.0,
                        target_duration_s = 2400,
                        target_pace_s_per_km = 360.0,
                        intensity = WorkoutIntensity.easy,
                        notes = "Try to keep hr below 150bpm",
                    ),
                    status = PlanDayStatus.COMPLETED,
                    runId = "run_completed",
                    linkedRun = Run(
                        id = "run_completed",
                        provider = "strava",
                        sport_type = "Run",
                        title = "Lake Loop",
                        start_time = "2026-04-06T13:00:00Z",
                        distance_m = 8100.0,
                        duration_s = 2520,
                        avg_pace_s_per_km = 311.0,
                        elevation_gain_m = 20.0,
                        route = RunRoute(summary_polyline = "o}mlHlariVoBiC{A{B_BaCuBeDyF_Jw@yAeAuBmAaCkBuDeBkDoAsB}@cBqAeCqBiEiBkD{BkEaC_FuAgC"),
                    ),
                ),
                unitSystem = UnitSystem.METRIC,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutCardMissedPreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            WorkoutCard(
                day = PlanDay(
                    date = "2026-04-05",
                    workout = Workout(
                        type = WorkoutType.easy,
                        label = "Easy Run",
                        description = "Flat route is ideal",
                        target_distance_m = 8000.0,
                        target_duration_s = 2400,
                        target_pace_s_per_km = 360.0,
                        intensity = WorkoutIntensity.easy,
                        notes = "Try to keep hr below 150bpm",
                    ),
                    status = PlanDayStatus.MISSED,
                ),
                unitSystem = UnitSystem.METRIC,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutCardRestPreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            WorkoutCard(
                day = PlanDay(
                    date = "2026-04-07",
                    workout = Workout(
                        type = WorkoutType.rest,
                        label = "Rest Day",
                        notes = "Walk or stretch if it feels good.",
                    ),
                    status = PlanDayStatus.REST,
                ),
                unitSystem = UnitSystem.METRIC,
            )
        }
    }
}
