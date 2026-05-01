package com.stridewell.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Vertically-stacked label-over-value statistic for the run detail header
 * (e.g. "AVG PACE / 5:30 /km").
 *
 * Renders the value first (large, semibold) and the label below in caps with
 * a smaller, muted style. No-wrap: the value gets ellipsis if it overflows.
 *
 * Mirrors iOS `ActivityStat` in `Views/Shared/Components/CardStat.swift`.
 */
@Composable
fun ActivityStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun ActivityStatPreview() {
    StridewellTheme {
        ActivityStat(label = "Avg Pace", value = "5:30 /km")
    }
}
