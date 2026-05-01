package com.stridewell.app.ui.main.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stridewell.app.model.RunSplit
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

/**
 * Three-column splits table: MILE · PACE · ELEV. Pace and elevation are right-aligned.
 *
 * Mirrors iOS `SplitsTableView` (`Views/Shared/Components/SplitsTableView.swift`).
 *
 * @param splits per-mile splits (lap-sourced or computed). Empty list renders the
 *   "No splits" empty state.
 * @param unit user's selected unit system (label only — pace input remains s/km).
 */
@Composable
fun SplitsTable(
    splits: List<RunSplit>,
    unit: UnitSystem,
    modifier: Modifier = Modifier,
) {
    if (splits.isEmpty()) {
        EmptyState(modifier = modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        HeaderRow()
        splits.forEach { split -> SplitRow(split = split, unit = unit) }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "MILE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(MILE_COLUMN_WIDTH),
        )
        Text(
            text = "PACE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f),
            textAlign = TextAlign.End,
        )
        Text(
            text = "ELEV",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(ELEV_COLUMN_WIDTH),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SplitRow(split: RunSplit, unit: UnitSystem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = split.index.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(MILE_COLUMN_WIDTH),
        )
        Text(
            text = FormatUtils.formatPace(split.avg_pace_s_per_km, unit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
        Text(
            text = formatElevation(split.elevation_gain_m),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(ELEV_COLUMN_WIDTH),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = "No splits",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Splits will appear here once the run has GPS data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatElevation(meters: Double?): String =
    when {
        meters == null -> "—"
        meters >= 0 -> "+%.0f m".format(meters)
        else -> "%.0f m".format(meters)
    }

private val MILE_COLUMN_WIDTH: Dp = 48.dp
private val ELEV_COLUMN_WIDTH: Dp = 80.dp

@Preview
@Composable
private fun SplitsTablePreview() {
    StridewellTheme {
        SplitsTable(
            splits = listOf(
                RunSplit(1, 1609.0, 480, 5 * 60.0, source = "lap", elevation_gain_m = 4.0),
                RunSplit(2, 1609.0, 470, 5 * 60.0 - 10, source = "lap", elevation_gain_m = -2.0),
                RunSplit(3, 1609.0, 465, 5 * 60.0 - 15, source = "lap", elevation_gain_m = 7.0),
            ),
            unit = UnitSystem.IMPERIAL,
        )
    }
}

@Preview
@Composable
private fun SplitsTableEmptyPreview() {
    StridewellTheme {
        SplitsTable(splits = emptyList(), unit = UnitSystem.METRIC)
    }
}
