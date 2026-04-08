package com.stridewell.app.ui.background.weather

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class StormContents {
    NONE,
    RAIN,
    SNOW
}

data class StormDrop(
    var x: Float,
    var y: Float,
    var xScale: Float,
    var yScale: Float,
    var speed: Float,
    var opacity: Float,
    var directionRad: Float,
    var rotationRad: Float,
    var rotationSpeedRad: Float
)

class StormEngine(
    type: StormContents,
    directionDegrees: Float,
    strength: Int
) {
    val drops: List<StormDrop>

    init {
        val cappedStrength = strength.coerceAtMost(1000).coerceAtLeast(0)
        drops = List(cappedStrength) { createDrop(type, directionDegrees) }
    }

    fun update(deltaSeconds: Float, widthPx: Float, heightPx: Float) {
        if (widthPx <= 0f || heightPx <= 0f) return
        val screenRatio = heightPx / widthPx
        drops.forEach { drop ->
            drop.x += cos(drop.directionRad) * drop.speed * deltaSeconds * screenRatio
            drop.y += sin(drop.directionRad) * drop.speed * deltaSeconds

            if (drop.x < -0.2f) {
                drop.x += 1.4f
            }
            if (drop.y > 1.2f) {
                drop.x = Random.nextFloat() * 1.4f - 0.2f
                drop.y -= 1.4f
            }
            drop.rotationRad += drop.rotationSpeedRad * deltaSeconds
        }
    }

    private fun createDrop(type: StormContents, directionDegrees: Float): StormDrop {
        val baseDirectionRad = Math.toRadians((directionDegrees + 90f).toDouble()).toFloat()
        val direction = if (type == StormContents.SNOW) {
            val wobble = Random.nextFloat() * 30f - 15f
            Math.toRadians((directionDegrees + 90f + wobble).toDouble()).toFloat()
        } else {
            baseDirectionRad
        }

        return when (type) {
            StormContents.SNOW -> {
                val xScale = Random.nextFloat() * (1f - 0.125f) + 0.125f
                StormDrop(
                    x = Random.nextFloat() * 1.4f - 0.2f,
                    y = Random.nextFloat() * 1.4f - 0.2f,
                    xScale = xScale,
                    yScale = xScale * (Random.nextFloat() * 0.5f + 0.5f),
                    speed = Random.nextFloat() * 0.4f + 0.2f,
                    opacity = Random.nextFloat() * 0.8f + 0.2f,
                    directionRad = direction,
                    rotationRad = Random.nextFloat() * (2f * Math.PI.toFloat()),
                    rotationSpeedRad = Math.toRadians((Random.nextFloat() * 720f - 360f).toDouble()).toFloat()
                )
            }
            else -> {
                val scale = Random.nextFloat() * 0.6f + 0.4f
                StormDrop(
                    x = Random.nextFloat() * 1.4f - 0.2f,
                    y = Random.nextFloat() * 1.4f - 0.2f,
                    xScale = scale,
                    yScale = scale,
                    speed = Random.nextFloat() + 1f,
                    opacity = Random.nextFloat() * 0.25f + 0.05f,
                    directionRad = direction,
                    rotationRad = 0f,
                    rotationSpeedRad = 0f
                )
            }
        }
    }
}
