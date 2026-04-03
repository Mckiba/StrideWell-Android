package com.stridewell.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RouteThumbnail(
    coordinates: List<RoutePoint>,
    strokeColor: Color,
    placeholderColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 32.dp, height = 34.dp)) {
        if (coordinates.size < 2) {
            drawRect(
                color = placeholderColor,
                style = Stroke(width = 1.dp.toPx())
            )
            return@Canvas
        }

        val lats = coordinates.map { it.latitude }
        val lngs = coordinates.map { it.longitude }
        val minLat = lats.min()
        val maxLat = lats.max()
        val minLng = lngs.min()
        val maxLng = lngs.max()
        val latSpan = maxLat - minLat
        val lngSpan = maxLng - minLng

        if (latSpan == 0.0 && lngSpan == 0.0) {
            drawRect(
                color = placeholderColor,
                style = Stroke(width = 1.dp.toPx())
            )
            return@Canvas
        }

        val scale = minOf(
            if (latSpan > 0.0) size.height / latSpan.toFloat() else Float.MAX_VALUE,
            if (lngSpan > 0.0) size.width / lngSpan.toFloat() else Float.MAX_VALUE
        )

        val drawWidth = lngSpan.toFloat() * scale
        val drawHeight = latSpan.toFloat() * scale
        val xOffset = (size.width - drawWidth) / 2f
        val yOffset = (size.height - drawHeight) / 2f

        val pathPoints = coordinates.map { coord ->
            Offset(
                x = xOffset + ((coord.longitude - minLng).toFloat() * scale),
                y = yOffset + ((maxLat - coord.latitude).toFloat() * scale)
            )
        }

        for (i in 1 until pathPoints.size) {
            drawLine(
                color = strokeColor,
                start = pathPoints[i - 1],
                end = pathPoints[i],
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
