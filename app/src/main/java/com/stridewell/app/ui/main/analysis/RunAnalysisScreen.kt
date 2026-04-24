package com.stridewell.app.ui.main.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.model.CardiacDrift
import com.stridewell.app.model.ExecutionAnalysis
import com.stridewell.app.model.HRAnalysis
import com.stridewell.app.model.HRZoneDistribution
import com.stridewell.app.model.PlannedVsActual
import com.stridewell.app.model.RunAnalysisResponse
import com.stridewell.app.model.TrendContext
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunAnalysisScreen(
    runId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(runId) { viewModel.load(runId) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Run Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val s = uiState.screenState) {
                RunAnalysisViewModel.ScreenState.Loading -> CenteredProgress()
                RunAnalysisViewModel.ScreenState.NotReady -> CenteredMessage(
                    "Analysis is still being computed. Check back in a few moments."
                )
                is RunAnalysisViewModel.ScreenState.Error -> CenteredMessage(s.message, onRetry = viewModel::retry)
                is RunAnalysisViewModel.ScreenState.Loaded -> AnalysisContent(s.analysis, uiState.unitSystem)
            }
        }
    }
}

// ── Content ─────────────────────────────────────────────────────────────────

@Composable
private fun AnalysisContent(analysis: RunAnalysisResponse, unit: UnitSystem) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        analysis.planned_vs_actual?.let { item { PlannedVsActualCard(it, unit) } }
        analysis.execution_analysis?.let { item { ExecutionCard(it, unit) } }
        analysis.hr_analysis?.let { item { HRCard(it) } }
        analysis.trend_context?.let { item { TrendCard(it) } }

        if (analysis.execution_analysis == null &&
            analysis.hr_analysis == null &&
            analysis.trend_context == null &&
            analysis.planned_vs_actual == null
        ) {
            item {
                Text(
                    "No analysis data available for this run.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlannedVsActualCard(pva: PlannedVsActual, unit: UnitSystem) {
    SectionCard("Planned vs Actual") {
        DetailRow("As planned", if (pva.completed_as_planned) "Yes" else "No")
        pva.distance_delta_m?.let {
            DetailRow("Distance delta", signedMeters(it, unit))
        }
        pva.pace_delta_s_per_km?.let { DetailRow("Pace delta", "%+.0f s/km".format(it)) }
        pva.duration_delta_s?.let { DetailRow("Duration delta", "%+.0f s".format(it)) }
        if (pva.notes.isNotBlank()) {
            Text(
                pva.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExecutionCard(ea: ExecutionAnalysis, unit: UnitSystem) {
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
                    "Factors: ${it.factors.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ea.stopped_time_s?.let { DetailRow("Stopped time", FormatUtils.duration(it)) }
    }
}

@Composable
private fun HRCard(hr: HRAnalysis) {
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
            d.interpretation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrendCard(t: TrendContext) {
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

// ── Primitives ──────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.md),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String, onRetry: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(Spacing.lg)
        ) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            if (onRetry != null) {
                androidx.compose.material3.TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

private fun String.humanize(): String =
    replace("_", " ").replaceFirstChar { it.uppercase() }

private fun signedMeters(m: Double, unit: UnitSystem): String {
    val sign = if (m >= 0) "+" else "-"
    return "$sign${FormatUtils.formatDistance(kotlin.math.abs(m), unit)}"
}
