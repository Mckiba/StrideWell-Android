package com.stridewell.app.ui.main.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stridewell.app.model.RunDetail
import com.stridewell.app.model.RunDetailResponse
import com.stridewell.app.model.RunSplit
import com.stridewell.app.model.RunStreams
import com.stridewell.app.ui.components.ActivityStat
import com.stridewell.app.ui.components.CenteredMessage
import com.stridewell.app.ui.components.DetailRow
import com.stridewell.app.ui.components.skeleton.RunAnalysisSkeleton
import com.stridewell.app.ui.components.skeleton.RunDetailSheetSkeleton
import com.stridewell.app.ui.main.analysis.RunAnalysisSections
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

/**
 * Scrollable body of [RunDetailScreen]'s bottom sheet — five sections:
 * header, splits, full stats, analysis, charts. Top-level switch handles the
 * loading / error / loaded shape; loaded content is composed of stateless
 * sections that take only their slice of the response.
 */
@Composable
internal fun RunDetailSheetBody(
    state: RunDetailViewModel.DetailScreenState,
    unit: UnitSystem,
    onRetry: () -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    when (state) {
        RunDetailViewModel.DetailScreenState.Loading ->
            RunDetailSheetSkeleton(modifier = Modifier.fillMaxSize())

        is RunDetailViewModel.DetailScreenState.Error ->
            CenteredMessage(message = state.message, onRetry = onRetry)

        is RunDetailViewModel.DetailScreenState.Loaded ->
            LoadedContent(
                detail = state.detail,
                analysis = state.analysis,
                unit = unit,
                lazyListState = lazyListState,
            )
    }
}

@Composable
private fun LoadedContent(
    detail: RunDetailResponse,
    analysis: RunDetailViewModel.AnalysisState,
    unit: UnitSystem,
    lazyListState: LazyListState,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item { HeaderSection(run = detail.run, unit = unit) }
        item { HorizontalDivider() }
        item { SplitsSection(splits = detail.splits, unit = unit) }
        item { HorizontalDivider() }
        item { StatsSection(run = detail.run, unit = unit) }
        item { HorizontalDivider() }
        item { AnalysisSlot(state = analysis, unit = unit) }
        chartsSection(streams = detail.streams)
        item { Box(modifier = Modifier.fillMaxWidth().padding(Spacing.xl)) }
    }
}

// ── Header ─────────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(run: RunDetail, unit: UnitSystem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = run.title ?: run.sport_type.replace("_", " ")
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        run.description?.takeIf { it.isNotBlank() }?.let { ExpandableText(text = it) }

        Row(modifier = Modifier.fillMaxWidth()) {
            ActivityStat(
                label = "Avg Pace",
                value = run.avg_pace_s_per_km?.let { FormatUtils.formatPace(it, unit) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            ActivityStat(
                label = "Time",
                value = FormatUtils.formatDuration(run.duration_s),
                modifier = Modifier.weight(1f),
            )
            ActivityStat(
                label = "Calories",
                value = run.calories_kcal?.let { "$it kcal" } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            ActivityStat(
                label = "Elevation",
                value = run.elevation_gain_m?.let { "+%.0fm".format(it) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            ActivityStat(
                label = "Avg HR",
                value = run.avg_hr_bpm?.let { "$it bpm" } ?: "—",
                modifier = Modifier.weight(1f),
            )
            ActivityStat(
                label = "Cadence",
                value = run.avg_cadence_spm?.let { "$it spm" } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Splits ─────────────────────────────────────────────────────────────────

@Composable
private fun SplitsSection(splits: List<RunSplit>, unit: UnitSystem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "Splits",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        SplitsTable(splits = splits, unit = unit)
    }
}

// ── Full stats ─────────────────────────────────────────────────────────────

@Composable
private fun StatsSection(run: RunDetail, unit: UnitSystem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Details",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )
        statRow("Distance", FormatUtils.formatDistance(run.distance_m, unit))
        statRow("Avg Pace", run.avg_pace_s_per_km?.let { FormatUtils.formatPace(it, unit) } ?: "—")
        statRow("Fastest Pace", run.best_pace_s_per_km?.let { FormatUtils.formatPace(it, unit) } ?: "—")
        statRow("Running Time", FormatUtils.formatDuration(run.duration_s))
        statRow("Elapsed Time", run.elapsed_time_s?.let { FormatUtils.formatDuration(it) } ?: "—")
        statRow("Calories", run.calories_kcal?.let { "$it kcal" } ?: "—")
        statRow("Avg Cadence", run.avg_cadence_spm?.let { "$it spm" } ?: "—")
        statRow("Elevation Gain", run.elevation_gain_m?.let { "+%.0f m".format(it) } ?: "—")
        statRow("Elevation Loss", run.elevation_loss_m?.let { "%.0f m".format(it) } ?: "—")
        statRow("Avg HR", run.avg_hr_bpm?.let { "$it bpm" } ?: "—")
        statRow("Max HR", run.max_hr_bpm?.let { "$it bpm" } ?: "—")
    }
}

@Composable
private fun statRow(label: String, value: String) {
    DetailRow(
        label = label,
        value = value,
        modifier = Modifier.padding(horizontal = Spacing.md),
    )
}

// ── Analysis slot ──────────────────────────────────────────────────────────

@Composable
private fun AnalysisSlot(state: RunDetailViewModel.AnalysisState, unit: UnitSystem) {
    when (state) {
        RunDetailViewModel.AnalysisState.Loading ->
            RunAnalysisSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
            )

        RunDetailViewModel.AnalysisState.NotReady ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = "Analysis not ready",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "We're still processing this run.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        RunDetailViewModel.AnalysisState.Error ->
            Box(modifier = Modifier.fillMaxWidth())

        is RunDetailViewModel.AnalysisState.Loaded ->
            RunAnalysisSections(
                analysis = state.response,
                unit = unit,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
    }
}

// ── Charts ─────────────────────────────────────────────────────────────────

private fun LazyListScope.chartsSection(streams: RunStreams?) {
    if (streams == null) return
    item { HorizontalDivider() }
    item { ChartBlock("Elevation", streams.distance_m, streams.altitude_m, StreamChartType.Elevation) }
    item { HorizontalDivider() }
    item { ChartBlock("Heart Rate", streams.distance_m, streams.heartrate, StreamChartType.HeartRate) }
    item { HorizontalDivider() }
    item { ChartBlock("Cadence", streams.distance_m, streams.cadence, StreamChartType.Cadence) }
}

@Composable
private fun ChartBlock(
    title: String,
    distances: List<Double>,
    values: List<Double>?,
    type: StreamChartType,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        RunStreamChart(distances = distances, values = values, type = type)
    }
}
