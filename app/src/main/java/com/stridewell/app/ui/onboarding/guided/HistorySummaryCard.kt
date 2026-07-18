package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridewell.app.model.HistoryWeekVolume
import com.stridewell.app.model.StravaHistorySummary
import com.stridewell.app.ui.theme.AccentLight
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Summary of the computed Strava history: a weekly-volume trend chart with the latest week
 * emphasized, plus 4-week average, 12-week peak, and runs-per-week stats. Falls back to the
 * stats row when there aren't enough weeks to plot.
 */
@Composable
fun HistorySummaryCard(
    summary: StravaHistorySummary,
    modifier: Modifier = Modifier
) {
    val points = (summary.weekly_volumes ?: emptyList())
        .map { OnboardingUnits.displayValueFromKm(it.volume_km) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (points.size >= 2) {
            VolumeChart(
                points = points,
                lastLabel = OnboardingUnits.formatted(points.last()),
                modifier = Modifier.fillMaxWidth().height(96.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Weekly avg", volumeLabel(summary.avg_weekly_volume_km_4wk))
            Stat("Peak week", volumeLabel(summary.peak_weekly_volume_km_12wk))
            Stat("Runs / wk", summary.avg_runs_per_week_4wk?.let { "%.1f".format(it) } ?: "—")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VolumeChart(
    points: List<Double>,
    lastLabel: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(color = onSurface, fontSize = 11.sp)

    Canvas(modifier = modifier) {
        val n = points.size
        val maxY = (points.maxOrNull() ?: 0.0).coerceAtLeast(1.0) * 1.2
        val topPadding = 18f // headroom for the last-week value label
        val chartHeight = size.height - topPadding
        val stepX = if (n > 1) size.width / (n - 1) else size.width

        fun x(i: Int) = stepX * i
        fun y(v: Double) = topPadding + (chartHeight - (v / maxY * chartHeight)).toFloat()

        val linePath = Path().apply {
            moveTo(x(0), y(points[0]))
            for (i in 1 until n) lineTo(x(i), y(points[i]))
        }
        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(x(n - 1), size.height)
            lineTo(x(0), size.height)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(AccentLight.copy(alpha = 0.30f), AccentLight.copy(alpha = 0f)),
                startY = topPadding,
                endY = size.height
            )
        )
        drawPath(path = linePath, color = AccentLight, style = Stroke(width = 6f))

        for (i in 0 until n) {
            drawCircle(color = AccentLight, radius = 4f, center = Offset(x(i), y(points[i])))
        }
        // Emphasize the latest week and label its value.
        val lastCenter = Offset(x(n - 1), y(points[n - 1]))
        drawCircle(color = AccentLight, radius = 8f, center = lastCenter)

        val measured = textMeasurer.measure(lastLabel, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                (lastCenter.x - measured.size.width).coerceAtLeast(0f),
                (lastCenter.y - measured.size.height - 6f).coerceAtLeast(0f)
            )
        )
    }
}

private fun volumeLabel(km: Double?): String =
    km?.let { OnboardingUnits.formatted(OnboardingUnits.displayValueFromKm(it)) } ?: "—"

@Preview
@Composable
private fun HistorySummaryCardPreview() = StridewellTheme {
    val volumes = listOf(0.0, 30.0, 55.0, 28.0, 32.0, 22.0, 18.0, 15.0, 32.0, 4.0, 20.0, 11.0)
    val summary = StravaHistorySummary(
        avg_weekly_volume_km_4wk = 40.0,
        peak_weekly_volume_km_12wk = 55.0,
        avg_runs_per_week_4wk = 4.0,
        inferred_training_phase = "base",
        weekly_volumes = volumes.mapIndexed { i, v -> HistoryWeekVolume(week_start = "2026-0${(i % 9) + 1}-01", volume_km = v) }
    )
    HistorySummaryCard(summary = summary, modifier = Modifier.padding(Spacing.md))
}
