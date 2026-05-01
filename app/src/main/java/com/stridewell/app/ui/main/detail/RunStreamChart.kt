package com.stridewell.app.ui.main.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * The three stream types shown on the Run Detail screen.
 *
 *  - [Elevation] renders as a filled area + line
 *  - [HeartRate], [Cadence] render as simple line charts
 *
 * Mirrors iOS `StreamChartType`.
 */
enum class StreamChartType(val label: String, val yAxisLabel: String) {
    Elevation(label = "elevation", yAxisLabel = "m"),
    HeartRate(label = "heart rate", yAxisLabel = "bpm"),
    Cadence(label = "cadence", yAxisLabel = "spm"),
}

/**
 * Renders one stream metric (elevation / HR / cadence) over distance using
 * Compose Canvas — no external charting dep.
 *
 * Mirrors iOS `RunStreamChart` (Swift Charts on iOS, Canvas here). Empty state
 * shown when [values] is null or empty.
 *
 * @param distances metres along the run (index axis)
 * @param values metric value at each [distances] index. Same length when present.
 * @param type chart kind — controls fill style and y-axis label
 */
@Composable
fun RunStreamChart(
    distances: List<Double>,
    values: List<Double>?,
    type: StreamChartType,
    modifier: Modifier = Modifier,
) {
    if (values.isNullOrEmpty() || distances.isEmpty()) {
        EmptyChart(type = type, modifier = modifier)
        return
    }

    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val density = LocalDensity.current
    val strokePx = with(density) { 2.dp.toPx() }
    val labelPx = with(density) { 11.sp.toPx() }

    val minV = values.min()
    val maxV = values.max()
    val rangeV = (maxV - minV).coerceAtLeast(1e-3)
    val maxD = distances.last().coerceAtLeast(1.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val plotLeft = X_AXIS_LABEL_INSET_PX
            val plotRight = size.width - 8f
            val plotTop = 8f
            val plotBottom = size.height - Y_AXIS_LABEL_INSET_PX
            val plotW = (plotRight - plotLeft).coerceAtLeast(1f)
            val plotH = (plotBottom - plotTop).coerceAtLeast(1f)

            // Build the line path.
            val linePath = Path()
            for (i in values.indices) {
                val xN = (distances[i] / maxD).toFloat().coerceIn(0f, 1f)
                val yN = ((values[i] - minV) / rangeV).toFloat().coerceIn(0f, 1f)
                val x = plotLeft + xN * plotW
                // Y inverts: 0 at bottom of plot, 1 at top.
                val y = plotBottom - yN * plotH
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }

            // Elevation gets a filled area underneath.
            if (type == StreamChartType.Elevation) {
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(plotRight, plotBottom)
                    lineTo(plotLeft, plotBottom)
                    close()
                }
                drawPath(areaPath, color = accent.copy(alpha = 0.25f))
            }

            drawPath(
                path = linePath,
                color = accent,
                style = Stroke(width = strokePx),
            )

            // Axis ticks (4 each, integer labels).
            drawAxisTicks(
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = plotTop,
                plotBottom = plotBottom,
                minV = minV,
                maxV = maxV,
                maxDistanceMeters = maxD,
                yLabelSuffix = type.yAxisLabel,
                labelPx = labelPx,
                tickColor = muted.copy(alpha = 0.5f),
                labelColor = muted,
            )
        }
    }
}

@Composable
private fun EmptyChart(type: StreamChartType, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "No data",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "No ${type.label} recorded for this run.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun DrawScope.drawAxisTicks(
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    minV: Double,
    maxV: Double,
    maxDistanceMeters: Double,
    yLabelSuffix: String,
    labelPx: Float,
    tickColor: Color,
    labelColor: Color,
) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint().apply {
        color = labelColor.toArgb()
        textSize = labelPx
        isAntiAlias = true
    }

    // X ticks: 0, 1/3, 2/3, full distance — formatted in miles for parity with iOS.
    val xMarks = 4
    val miles = maxDistanceMeters * 0.000621371
    for (i in 0 until xMarks) {
        val frac = i / (xMarks - 1f)
        val x = plotLeft + frac * (plotRight - plotLeft)
        drawLine(
            color = tickColor,
            start = Offset(x, plotBottom),
            end = Offset(x, plotBottom + 4f),
        )
        val mileVal = miles * frac
        val label = "%.1f mi".format(mileVal)
        // Right-align last label so it doesn't overflow the canvas; left-align first.
        val xLabel = when (i) {
            0 -> x
            xMarks - 1 -> x - paint.measureText(label)
            else -> x - paint.measureText(label) / 2f
        }
        canvas.drawText(label, xLabel, plotBottom + 16f, paint)
    }

    // Y ticks: ~4 levels.
    val yMarks = 4
    for (i in 0 until yMarks) {
        val frac = i / (yMarks - 1f)
        val y = plotBottom - frac * (plotBottom - plotTop)
        drawLine(
            color = tickColor,
            start = Offset(plotLeft - 4f, y),
            end = Offset(plotLeft, y),
        )
        val v = minV + (maxV - minV) * frac
        val label = "${v.toInt()} $yLabelSuffix"
        canvas.drawText(label, 4f, y + labelPx / 3f, paint)
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)

private const val X_AXIS_LABEL_INSET_PX = 56f
private const val Y_AXIS_LABEL_INSET_PX = 22f

@Preview(heightDp = 200)
@Composable
private fun RunStreamChartElevationPreview() {
    StridewellTheme {
        RunStreamChart(
            distances = (0..50).map { it * 200.0 },
            values = (0..50).map { 100.0 + 30.0 * kotlin.math.sin(it * 0.3) },
            type = StreamChartType.Elevation,
        )
    }
}

@Preview(heightDp = 200)
@Composable
private fun RunStreamChartEmptyPreview() {
    StridewellTheme {
        RunStreamChart(
            distances = (0..10).map { it * 200.0 },
            values = null,
            type = StreamChartType.HeartRate,
        )
    }
}
