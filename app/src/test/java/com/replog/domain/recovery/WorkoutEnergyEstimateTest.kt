package com.replog.domain.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutEnergyEstimateTest {
    private val start = 1_000_000L

    @Test
    fun missingOrInvalidInputsDoNotProduceAnEstimate() {
        assertNull(WorkoutEnergyEstimate.estimate(start, start + 45 * 60_000L, null, 12, 8.0))
        assertNull(WorkoutEnergyEstimate.estimate(start, start + 45 * 60_000L, 20.0, 12, 8.0))
        assertNull(WorkoutEnergyEstimate.estimate(start, start + 45 * 60_000L, 80.0, 0, 8.0))
        assertNull(WorkoutEnergyEstimate.estimate(start, start + 2 * 60_000L, 80.0, 12, 8.0))
        assertNull(WorkoutEnergyEstimate.estimate(start, null, 80.0, 12, 8.0))
    }

    @Test
    fun estimateUsesCompletedDurationAndReturnsAReadableRange() {
        val estimate = WorkoutEnergyEstimate.estimate(
            startTimeMillis = start,
            endTimeMillis = start + 60 * 60_000L,
            weightKg = 80.0,
            completedSetCount = 18,
            averageRpe = 8.0
        )

        requireNotNull(estimate)
        assertEquals(60, estimate.durationMinutes)
        assertTrue(estimate.lowCalories > 0)
        assertTrue(estimate.highCalories > estimate.lowCalories)
        assertTrue(estimate.label.startsWith("Estimated energy use:"))
    }

    @Test
    fun higherEffortAndDenseSessionIncreaseTheEstimate() {
        val easy = WorkoutEnergyEstimate.estimate(start, start + 60 * 60_000L, 80.0, 10, 6.0)
        val hard = WorkoutEnergyEstimate.estimate(start, start + 60 * 60_000L, 80.0, 24, 9.0)

        requireNotNull(easy)
        requireNotNull(hard)
        assertTrue(hard.lowCalories > easy.lowCalories)
        assertTrue(hard.highCalories > easy.highCalories)
    }
}
