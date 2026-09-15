package com.stridewell.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stridewell.app.model.RunSummaryTotals
import com.stridewell.app.ui.theme.ActivityOverviewStatLabelStyle
import com.stridewell.app.ui.theme.ActivityOverviewStatValueStyle
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

/** Runs / Avg. Pace / Time totals under the Activities overview distance. */
@Composable
fun ActivityStatsRow(
    totals: RunSummaryTotals,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
) {
    val pace = totals.avg_pace_s_per_km?.let { FormatUtils.pace(it, unitSystem) }
        ?: if (totals.run_count == 0) "0" else "—"
    val time = if (totals.run_count == 0) "0" else FormatUtils.duration(totals.duration_s)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        OverviewStat(label = "Runs", value = totals.run_count.toString())
        OverviewStat(label = "Avg. Pace", value = pace)
        OverviewStat(label = "Time", value = time)
    }
}

@Composable
private fun OverviewStat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = value,
            style = ActivityOverviewStatValueStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            text = label,
            style = ActivityOverviewStatLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityStatsRowPreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            ActivityStatsRow(
                totals = RunSummaryTotals(
                    distance_m = 32_186.0,
                    run_count = 5,
                    duration_s = 10_800,
                    avg_pace_s_per_km = 335.6
                ),
                unitSystem = UnitSystem.IMPERIAL
            )
            ActivityStatsRow(
                totals = RunSummaryTotals(
                    distance_m = 0.0,
                    run_count = 0,
                    duration_s = 0,
                    avg_pace_s_per_km = null
                ),
                unitSystem = UnitSystem.IMPERIAL
            )
        }
    }
}
