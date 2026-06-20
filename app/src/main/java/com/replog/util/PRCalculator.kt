package com.replog.util

import kotlin.math.roundToInt

object PRCalculator {
    fun epley1RM(weight: Double, reps: Int): Double = if (reps <= 1) weight else weight * (1 + reps / 30.0)
    fun brzycki1RM(weight: Double, reps: Int): Double = if (reps <= 1) weight else weight * (36.0 / (37.0 - reps.coerceAtMost(36)))
    fun percentageOf(oneRM: Double, percentage: Double): Double = (oneRM * percentage / 100.0).roundToInt().toDouble()
    fun volume(weight: Double, reps: Int): Double = weight * reps
}
