package com.stridewell.app.ui.main.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stridewell.app.model.CardiacDrift
import com.stridewell.app.model.ExecutionAnalysis
import com.stridewell.app.model.HRAnalysis
import com.stridewell.app.model.HRZoneDistribution
import com.stridewell.app.model.PlannedVsActual
import com.stridewell.app.model.RunAnalysisResponse
import com.stridewell.app.model.TrendContext
import com.stridewell.app.ui.components.DetailRow
import com.stridewell.app.ui.components.SectionCard
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

/**
 * Composable that renders the four V2 analysis cards (planned-vs-actual,
 * execution, HR, trends) for a [RunAnalysisResponse].
 *
 * Stateless and ViewModel-free: callers pass model + [UnitSystem] directly so
 * `RunDetailScreen` can drop this into its bottom sheet without owning a
 * second analysis ViewModel.
 *
 * Mirrors iOS `RunAnalysisSections` (`Views/Main/RunAnalysisSections.swift`).
 *
 * Empty fallback: when all four blocks are null, a single SectionCard with the
 * "No analysis details available" message is rendered.
 */
@Composable
fun RunAnalysisSections(
    analysis: RunAnalysisResponse,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
    status: String? = analysis.status,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (status == "partial") {
            PartialBanner()
        }

        analysis.planned_vs_actual?.let { PlannedVsActualCard(it, unit) }
        analysis.execution_analysis?.let { ExecutionCard(it, unit) }
        analysis.hr_analysis?.let { HRCard(it) }
        analysis.trend_context?.let { TrendCard(it) }

        if (analysis.execution_analysis == null &&
            analysis.hr_analysis == null &&
            analysis.trend_context == null &&
            analysis.planned_vs_actual == null
        ) {
            SectionCard("Analysis") {
                Text(
                    text = "No analysis details available for this run.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PartialBanner() {
    SectionCard("Heads up") {
        Text(
            text = "Some analysis blocks are still being computed. Check back soon for the full picture.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PlannedVsActualCard(pva: PlannedVsActual, unit: UnitSystem) {
    SectionCard("Planned vs Actual") {
        DetailRow("As planned", if (pva.completed_as_planned) "Yes" else "No")
        pva.distance_delta_m?.let {
            DetailRow("Distance delta", signedMeters(it, unit))
        }
        pva.pace_delta_s_per_km?.let { DetailRow("Pace delta", "%+.0f s/km".format(it)) }
        pva.duration_delta_s?.let { DetailRow("Duration delta", "%+.0f s".format(it)) }
        if (pva.notes.isNotBlank()) {
            Text(
                text = pva.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ExecutionCard(ea: ExecutionAnalysis, unit: UnitSystem) {
    SectionCard("Execution") {
        val pc = ea.pace_consistency
        DetailRow("Split profile", pc.split_profile.humanize())
        DetailRow("Variation", pc.classification.humanize())
        DetailRow("CV", "%.1f%%".format(pc.coefficient_of_variation * 100))
        DetailRow("First half pace", FormatUtils.pace(pc.first_half_avg_pace_s_per_km, unit))
        DetailRow("Second half pace", FormatUtils.pace(pc.second_half_avg_pace_s_per_km, unit))
        ea.execution_quality?.let {
            DetailRow("Quality", it.score.humanize())
            if (it.factors.isNotEmpty()) {
                Text(
                    text = "Factors: ${it.factors.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ea.stopped_time_s?.let { DetailRow("Stopped time", FormatUtils.duration(it)) }
    }
}

@Composable
fun HRCard(hr: HRAnalysis) {
    SectionCard("Heart Rate") {
        DetailRow("Avg HR", "${hr.avg_hr_bpm} bpm")
        hr.max_hr_bpm?.let { DetailRow("Max HR", "$it bpm") }
        hr.cardiac_drift?.let { DriftRows(it) }
        hr.efficiency?.let {
            DetailRow("Efficiency trend", it.trend.humanize())
        }
        hr.zone_distribution?.let { ZoneDistributionRows(it) }
    }
}

@Composable
private fun DriftRows(d: CardiacDrift) {
    DetailRow("Cardiac drift", "${d.drift_bpm} bpm (${"%.1f".format(d.drift_pct)}%)")
    if (d.interpretation.isNotBlank()) {
        Text(
            text = d.interpretation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ZoneDistributionRows(z: HRZoneDistribution) {
    DetailRow("Primary zone", "Z${z.primary_zone}")
    DetailRow("Z1", "%.0f%%".format(z.zone_1_pct))
    DetailRow("Z2", "%.0f%%".format(z.zone_2_pct))
    DetailRow("Z3", "%.0f%%".format(z.zone_3_pct))
    DetailRow("Z4", "%.0f%%".format(z.zone_4_pct))
    DetailRow("Z5", "%.0f%%".format(z.zone_5_pct))
    z.zone_note?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TrendCard(t: TrendContext) {
    SectionCard("Trends") {
        DetailRow("This week", "%.1f km".format(t.weekly_volume.current_week_km))
        DetailRow("Last week", "%.1f km".format(t.weekly_volume.previous_week_km))
        DetailRow("4-week avg", "%.1f km".format(t.weekly_volume.four_week_avg_km))
        DetailRow("Volume trend", t.weekly_volume.trend.humanize())
        DetailRow("Pace trend (${t.pace_trend.workout_type.humanize()})", t.pace_trend.trend.humanize())
        DetailRow("Runs this week", t.runs_this_week.toString())
        DetailRow("Current streak", "${t.streak_days} days")
        val c = t.compliance_last_14_days
        DetailRow("Compliance (14d)", "${c.completed}/${c.planned} · ${(c.rate * 100).toInt()}%")
    }
}

private fun String.humanize(): String =
    replace("_", " ").replaceFirstChar { it.uppercase() }

private fun signedMeters(m: Double, unit: UnitSystem): String {
    val sign = if (m >= 0) "+" else "-"
    return "$sign${FormatUtils.formatDistance(kotlin.math.abs(m), unit)}"
}

@Preview
@Composable
private fun RunAnalysisSectionsEmptyPreview() {
    StridewellTheme {
        // Empty fallback case (all blocks null).
        RunAnalysisSections(
            analysis = RunAnalysisResponse(
                run_id = "preview",
                status = null,
                computed_at = "2026-04-29T00:00:00Z",
                planned_vs_actual = null,
                execution_analysis = null,
                hr_analysis = null,
                trend_context = null,
            ),
            unit = UnitSystem.METRIC,
        )
    }
}
