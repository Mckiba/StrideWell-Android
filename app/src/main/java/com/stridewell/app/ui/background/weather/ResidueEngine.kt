package com.stridewell.app.ui.background.weather

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ResidueDrop(
    val id: String = java.util.UUID.randomUUID().toString(),
    var destructionTimeSec: Double,
    var x: Float,
    var y: Float = 0.5f,
    var scale: Float,
    var speed: Float,
    var opacity: Float,
    var xMovement: Float,
    var yMovement: Float
)

class ResidueEngine(
    private val type: StormContents,
    strength: Float
) {
    private val drops = LinkedHashMap<String, ResidueDrop>()
    private var nextCreationAtSec = 0.0

    private val residueAmount: Int =
        if (type == StormContents.SNOW) 1 else 3

    private val lifetimeMin: Double =
        if (type == StormContents.SNOW) 1.0 else 0.9

    private val lifetimeMax: Double =
        if (type == StormContents.SNOW) 2.0 else 1.1

    private val creationDelayRange: ClosedFloatingPointRange<Double>? =
        when {
            strength <= 0f || type == StormContents.NONE -> null
            strength <= 200f -> 0.0..0.25
            strength <= 400f -> 0.0..0.10
            strength <= 800f -> 0.0..0.05
            else -> 0.0..0.02
        }

    fun update(nowSec: Double, deltaSeconds: Float, widthPx: Float, heightPx: Float): List<ResidueDrop> {
        val delay = creationDelayRange ?: return drops.values.toList()
        if (widthPx <= 0f || heightPx <= 0f) return drops.values.toList()

        val divisor = heightPx / widthPx
        val iterator = drops.values.iterator()
        while (iterator.hasNext()) {
            val drop = iterator.next()
            drop.x += drop.xMovement * drop.speed * deltaSeconds * divisor
            drop.y += drop.yMovement * drop.speed * deltaSeconds
            drop.yMovement += deltaSeconds * 2f

            if (drop.y > 0.5f && drop.x > 0.075f && drop.x < 0.925f) {
                drop.y = 0.5f
                drop.yMovement = 0f
            }
            if (drop.destructionTimeSec < nowSec) {
                iterator.remove()
            }
        }

        if (nextCreationAtSec < nowSec) {
            val dropX = Random.nextFloat() * (0.925f - 0.075f) + 0.075f
            repeat(residueAmount) {
                val newDrop = createResidueDrop(dropX, nowSec)
                drops[newDrop.id] = newDrop
            }
            nextCreationAtSec = nowSec + Random.nextDouble(delay.start, delay.endInclusive)
        }

        return drops.values.toList()
    }

    private fun createResidueDrop(xPosition: Float, nowSec: Double): ResidueDrop {
        val destruction = nowSec + Random.nextDouble(lifetimeMin, lifetimeMax)
        return if (type == StormContents.SNOW) {
            ResidueDrop(
                destructionTimeSec = destruction,
                x = xPosition,
                scale = Random.nextFloat() * (0.75f - 0.125f) + 0.125f,
                speed = 0f,
                opacity = Random.nextFloat() * 0.5f + 0.2f,
                xMovement = 0f,
                yMovement = 0f
            )
        } else {
            val direction = Math.toRadians((Random.nextFloat() * 90f + 225f).toDouble())
            ResidueDrop(
                destructionTimeSec = destruction,
                x = xPosition,
                scale = Random.nextFloat() * 0.1f + 0.4f,
                speed = 2f,
                opacity = Random.nextFloat() * 0.3f,
                xMovement = cos(direction).toFloat(),
                yMovement = (sin(direction) / 1.5).toFloat()
            )
        }
    }
}
