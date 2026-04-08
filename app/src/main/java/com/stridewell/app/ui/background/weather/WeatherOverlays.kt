package com.stridewell.app.ui.background.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import com.stridewell.R
import com.stridewell.app.model.StormCondition
import kotlin.math.PI

@Composable
fun StormOverlayView(
    condition: StormCondition,
    modifier: Modifier = Modifier
) {
    when (condition) {
        StormCondition.CLEAR -> Unit
        StormCondition.RAIN -> {
            StormView(
                type = StormContents.RAIN,
                directionDegrees = 20f,
                strength = 250,
                modifier = modifier
            )
        }
        StormCondition.SNOW -> {
            StormView(
                type = StormContents.SNOW,
                directionDegrees = 0f,
                strength = 150,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun StormView(
    type: StormContents,
    directionDegrees: Float,
    strength: Int,
    modifier: Modifier = Modifier
) {
    val engine = remember(type, directionDegrees, strength) {
        StormEngine(type = type, directionDegrees = directionDegrees, strength = strength)
    }
    val sprite = androidx.compose.ui.graphics.ImageBitmap.imageResource(
        id = if (type == StormContents.SNOW) R.drawable.snow else R.drawable.rain
    )

    var width by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableFloatStateOf(0f) }
    var frameNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(engine) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val deltaSeconds = (now - lastNanos) / 1_000_000_000f
                    engine.update(deltaSeconds = deltaSeconds, widthPx = width, heightPx = height)
                }
                lastNanos = now
                frameNanos = now
            }
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        width = size.width
        height = size.height
        frameNanos

        for (drop in engine.drops) {
            val xPos = drop.x * size.width
            val yPos = drop.y * size.height
            val rotationDeg = ((drop.directionRad + drop.rotationRad) * 180f / PI.toFloat()) - 90f
            withTransform({
                translate(left = xPos, top = yPos)
                rotate(rotationDeg)
                scale(scaleX = drop.xScale, scaleY = drop.yScale)
            }) {
                drawImage(
                    image = sprite,
                    topLeft = Offset.Zero,
                    alpha = drop.opacity
                )
            }
        }
    }
}

@Composable
fun ResidueView(
    type: StormContents,
    strength: Float,
    modifier: Modifier = Modifier
) {
    val engine = remember(type, strength) {
        ResidueEngine(type = type, strength = strength)
    }
    // iOS uses the snow sprite for residue across both rain and snow states.
    val sprite = androidx.compose.ui.graphics.ImageBitmap.imageResource(id = R.drawable.snow)

    var width by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableFloatStateOf(0f) }
    var frameNanos by remember { mutableLongStateOf(0L) }
    var drops by remember { mutableStateOf<List<ResidueDrop>>(emptyList()) }

    LaunchedEffect(engine) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                val deltaSeconds = if (lastNanos == 0L) 0f else (now - lastNanos) / 1_000_000_000f
                drops = engine.update(
                    nowSec = now / 1_000_000_000.0,
                    deltaSeconds = deltaSeconds,
                    widthPx = width,
                    heightPx = height
                )
                lastNanos = now
                frameNanos = now
            }
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        width = size.width
        height = size.height
        frameNanos

        for (drop in drops) {
            val xPos = drop.x * size.width
            val yPos = drop.y * size.height
            withTransform({
                translate(left = xPos, top = yPos)
                scale(scaleX = drop.scale, scaleY = drop.scale)
            }) {
                drawImage(
                    image = sprite,
                    topLeft = Offset.Zero,
                    alpha = drop.opacity
                )
            }
        }
    }
}
