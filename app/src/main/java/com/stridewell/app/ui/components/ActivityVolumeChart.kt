package com.stridewell.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.model.Run
import com.stridewell.app.model.RunSummaryBucket
import com.stridewell.app.ui.theme.ActivityNameStyle
import com.stridewell.app.ui.theme.ActivityTimestampStyle
import com.stridewell.app.ui.theme.ChartGridDark
import com.stridewell.app.ui.theme.ChartGridLight
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.ActivityRange
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem
import com.stridewell.app.ui.theme.CornerRadius as RadiusTokens

/**
 * Distance per bucket for the selected Activities period: days for a week or
 * month, months for a year, years for all time. Tapping a bar selects it;
 * tapping the selected bar or an empty bucket clears the selection.
 */
@Composable
fun ActivityVolumeChart(
    range: ActivityRange,
    buckets: List<RunSummaryBucket>,
    unitSystem: UnitSystem,
    selectedKey: String?,
    onSelectedKeyChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gridColor = if (isDarkTheme) ChartGridDark else ChartGridLight
    val barColor = MaterialTheme.colorScheme.primary
    val labelStyle = ActivityTimestampStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    val values = remember(buckets, unitSystem) {
        buckets.map { FormatUtils.distanceValue(it.distance_m, unitSystem) }
    }
    val labels = remember(buckets, range) {
        buckets.map { DateUtils.bucketAxisLabel(range, it.key) }
    }
    val currentSelectedKey by rememberUpdatedState(selectedKey)
    val currentOnSelectedKeyChange by rememberUpdatedState(onSelectedKeyChange)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .pointerInput(buckets) {
                detectTapGestures { offset ->
                    if (buckets.isEmpty()) return@detectTapGestures
                    val slot = size.width.toFloat() / buckets.size
                    val bucket = buckets[(offset.x / slot).toInt().coerceIn(0, buckets.lastIndex)]
                    currentOnSelectedKeyChange(
                        if (bucket.run_count == 0 || bucket.key == currentSelectedKey) null else bucket.key
                    )
                }
            }
    ) {
        val labelBand = 20.dp.toPx()
        val plotHeight = size.height - labelBand
        val stroke = 0.5.dp.toPx()

        // Six grid rows plus the plot border.
        for (row in 0..6) {
            val y = plotHeight * row / 6f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), stroke)
        }
        drawLine(gridColor, Offset(0f, 0f), Offset(0f, plotHeight), stroke)
        drawLine(gridColor, Offset(size.width, 0f), Offset(size.width, plotHeight), stroke)

        if (buckets.isEmpty()) return@Canvas
        val maxY = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0) * 1.15
        val slot = size.width / buckets.size
        val barWidth = slot * 0.6f

        buckets.forEachIndexed { index, bucket ->
            val barHeight = (values[index] / maxY * plotHeight).toFloat()
            if (barHeight > 0f) {
                val alpha = if (selectedKey == null || selectedKey == bucket.key) 1f else 0.35f
                drawRoundRect(
                    color = barColor.copy(alpha = alpha),
                    topLeft = Offset(slot * index + (slot - barWidth) / 2f, plotHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
            // A month labels every 7th day to avoid crowding.
            if (range != ActivityRange.MONTH || index % 7 == 0) {
                val measured = textMeasurer.measure(labels[index], labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        slot * index + (slot - measured.size.width) / 2f,
                        plotHeight + (labelBand - measured.size.height) / 2f
                    )
                )
            }
        }
    }
}

/**
 * Callout for a selected chart bucket: the run's card for a single-run bucket,
 * otherwise the bucket's distance and run count.
 */
@Composable
fun ActivityBucketCallout(
    range: ActivityRange,
    bucket: RunSummaryBucket,
    run: Run?,
    unitSystem: UnitSystem,
    onOpenRun: (Run) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bucket.run_count == 1 && run != null) {
        ActivityCard(
            run = run,
            unitSystem = unitSystem,
            modifier = modifier,
            onClick = { onOpenRun(run) }
        )
        return
    }

    val shape = RoundedCornerShape(RadiusTokens.md)
    val runsLabel = if (bucket.run_count == 1) "run" else "runs"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = DateUtils.bucketTitle(range, bucket.key),
            style = ActivityNameStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${FormatUtils.distance(bucket.distance_m, unitSystem)}, ${bucket.run_count} $runsLabel",
            style = ActivityTimestampStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// MARK: - Previews

@Preview(showBackground = true)
@Composable
private fun ActivityVolumeChartWeekPreview() {
    StridewellTheme {
        val days = listOf("2026-09-14", "2026-09-15", "2026-09-16", "2026-09-17", "2026-09-18", "2026-09-19", "2026-09-20")
        val distances = listOf(8_046.0, 0.0, 12_874.0, 4_828.0, 0.0, 19_312.0, 6_437.0)
        val buckets = days.zip(distances) { day, distance ->
            RunSummaryBucket(key = day, distance_m = distance, run_count = if (distance > 0) 1 else 0)
        }
        var selectedKey by remember { mutableStateOf<String?>("2026-09-16") }
        ActivityVolumeChart(
            range = ActivityRange.WEEK,
            buckets = buckets,
            unitSystem = UnitSystem.IMPERIAL,
            selectedKey = selectedKey,
            onSelectedKeyChange = { selectedKey = it },
            modifier = Modifier.padding(Spacing.md)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ActivityVolumeChartYearDarkPreview() {
    StridewellTheme(darkTheme = true) {
        val distances = listOf(96_000.0, 112_000.0, 128_000.0, 80_000.0, 144_000.0, 160_000.0,
            136_000.0, 150_000.0, 64_000.0, 0.0, 0.0, 0.0)
        val buckets = distances.mapIndexed { index, distance ->
            RunSummaryBucket(
                key = "2026-%02d".format(index + 1),
                distance_m = distance,
                run_count = (distance / 8_000).toInt()
            )
        }
        ActivityVolumeChart(
            range = ActivityRange.YEAR,
            buckets = buckets,
            unitSystem = UnitSystem.METRIC,
            selectedKey = null,
            onSelectedKeyChange = {},
            modifier = Modifier.padding(Spacing.md)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityVolumeChartEmptyMonthPreview() {
    StridewellTheme {
        val buckets = (1..30).map { day ->
            RunSummaryBucket(key = "2026-09-%02d".format(day), distance_m = 0.0, run_count = 0)
        }
        ActivityVolumeChart(
            range = ActivityRange.MONTH,
            buckets = buckets,
            unitSystem = UnitSystem.IMPERIAL,
            selectedKey = null,
            onSelectedKeyChange = {},
            modifier = Modifier.padding(Spacing.md)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityBucketCalloutPreview() {
    StridewellTheme {
        ActivityBucketCallout(
            range = ActivityRange.YEAR,
            bucket = RunSummaryBucket(key = "2026-03", distance_m = 128_000.0, run_count = 14),
            run = null,
            unitSystem = UnitSystem.IMPERIAL,
            onOpenRun = {},
            modifier = Modifier.padding(Spacing.md)
        )
    }
}
