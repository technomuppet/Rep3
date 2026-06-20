package com.replog.util

import com.replog.data.model.SetLog
import com.replog.data.model.WorkoutPrescription
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {
    @Test
    fun hitAtLowRpeSuggestsLoadIncrease() {
        val prescription = prescription(weight = 100.0, reps = 5, sets = 3)
        val result = ProgressionEngine.evaluate(
            prescription,
            listOf(
                set(100.0, 5, 7.0),
                set(100.0, 5, 7.5),
                set(100.0, 5, 7.5)
            )
        )
        assertTrue(result.hit)
        assertTrue(result.recommendation.contains("Increase load"))
    }

    @Test
    fun missedHighRpeSuggestsFatigueReduction() {
        val prescription = prescription(weight = 100.0, reps = 8, sets = 3)
        val result = ProgressionEngine.evaluate(
            prescription,
            listOf(
                set(100.0, 6, 9.5),
                set(100.0, 5, 10.0)
            )
        )
        assertFalse(result.hit)
        assertTrue(result.recommendation.contains("Reduce fatigue"))
    }

    private fun prescription(weight: Double, reps: Int, sets: Int) = WorkoutPrescription(
        sessionId = 1,
        exerciseId = 1,
        source = "Test",
        targetSets = sets,
        targetReps = reps,
        targetWeight = weight
    )

    private fun set(weight: Double, reps: Int, rpe: Double) = SetLog(
        sessionExerciseId = 1,
        setNumber = 1,
        weight = weight,
        reps = reps,
        rpe = rpe
    )
}
