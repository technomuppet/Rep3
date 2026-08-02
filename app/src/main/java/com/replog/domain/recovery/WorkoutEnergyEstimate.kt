package com.replog.domain.recovery

import kotlin.math.roundToInt

/**
 * A deliberately broad estimate of energy used during a logged strength workout.
 *
 * This is not a measurement, a promise of calories burned, or dietary advice. It
 * uses elapsed session time, profile weight, completed-set density, and optional
 * logged RPE to give a useful range without adding sensors or network services.
 */
data class WorkoutEnergyEstimate(
    val lowCalories: Int,
    val highCalories: Int,
    val durationMinutes: Int
) {
    val label: String get() = "Estimated energy use: $lowCalories–$highCalories kcal"

    companion object {
        private const val MIN_VALID_WEIGHT_KG = 30.0
        private const val MAX_VALID_WEIGHT_KG = 300.0
        private const val MIN_DURATION_MINUTES = 5.0
        private const val MAX_DURATION_MINUTES = 360.0
        private const val CALORIE_FACTOR = 3.5 / 200.0

        /**
         * Returns null when the workout or profile is not sufficient for a
         * responsible estimate. The MET-style calculation is intentionally
         * presented as a range because logged strength sessions vary widely in
         * rest time and effort.
         */
        fun estimate(
            startTimeMillis: Long,
            endTimeMillis: Long?,
            weightKg: Double?,
            completedSetCount: Int,
            averageRpe: Double?
        ): WorkoutEnergyEstimate? {
            val weight = weightKg ?: return null
            if (weight !in MIN_VALID_WEIGHT_KG..MAX_VALID_WEIGHT_KG) return null
            if (completedSetCount <= 0) return null

            val durationMinutes = (endTimeMillis?.minus(startTimeMillis)?.toDouble() ?: return null) / 60_000.0
            if (durationMinutes !in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES) return null

            val safeRpe = averageRpe?.takeIf { it.isFinite() }?.coerceIn(1.0, 10.0)
            val effortMet = when {
                safeRpe == null -> 4.75
                safeRpe >= 8.5 -> 5.5
                safeRpe >= 7.0 -> 5.0
                else -> 4.25
            }

            // More completed sets per minute usually means less passive rest;
            // use only a small adjustment so duration remains the main input.
            val setsPerMinute = completedSetCount / durationMinutes
            val densityAdjustment = when {
                setsPerMinute >= 0.25 -> 0.5
                setsPerMinute >= 0.15 -> 0.25
                else -> 0.0
            }
            val centralMet = effortMet + densityAdjustment
            val lowMet = (centralMet - 0.5).coerceAtLeast(3.5)
            val highMet = (centralMet + 0.75).coerceAtMost(7.0)

            val low = (lowMet * weight * durationMinutes * CALORIE_FACTOR).roundToInt().coerceAtLeast(1)
            val high = (highMet * weight * durationMinutes * CALORIE_FACTOR)
                .roundToInt().coerceAtLeast(low)

            return WorkoutEnergyEstimate(
                lowCalories = low,
                highCalories = high,
                durationMinutes = durationMinutes.roundToInt()
            )
        }
    }
}
