package com.replog.util

data class PlateLoad(
    val targetWeight: Double,
    val barWeight: Double,
    val perSideWeight: Double,
    val platesPerSide: List<Double>,
    val loadedWeight: Double,
    val remainingWeight: Double
)

object PlateCalculator {
    val metricPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val imperialPlates = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    fun calculate(
        targetWeight: Double,
        barWeight: Double = 20.0,
        useKg: Boolean = true,
        customPlates: List<Double>? = null
    ): PlateLoad {
        val available = (customPlates?.takeIf { it.isNotEmpty() } ?: if (useKg) metricPlates else imperialPlates)
            .filter { it > 0.0 }
            .distinct()
            .sortedDescending()
        val perSideTarget = ((targetWeight - barWeight) / 2.0).coerceAtLeast(0.0)
        var remaining = perSideTarget
        val plates = mutableListOf<Double>()

        available.forEach { plate ->
            while (remaining + 0.0001 >= plate) {
                plates += plate
                remaining -= plate
            }
        }

        val perSideLoaded = plates.sum()
        val loaded = barWeight + perSideLoaded * 2.0
        return PlateLoad(
            targetWeight = targetWeight,
            barWeight = barWeight,
            perSideWeight = perSideLoaded,
            platesPerSide = plates,
            loadedWeight = loaded,
            remainingWeight = targetWeight - loaded
        )
    }

    fun parsePlates(input: String): List<Double> = input
        .split(",", " ", ";", "\n")
        .mapNotNull { it.trim().takeIf { value -> value.isNotBlank() }?.toDoubleOrNull() }
        .filter { it > 0.0 }
        .distinct()
        .sortedDescending()

    fun formatPlates(plates: List<Double>): String = plates
        .sortedDescending()
        .joinToString(", ") { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
}
