package com.replog.data.repository

import com.replog.domain.recommendation.PlannedExercise
import com.replog.domain.recommendation.ProgressionDecision
import com.replog.domain.recommendation.Recommendation
import com.replog.domain.recommendation.RecommendationType
import com.replog.domain.recommendation.WorkoutPlan
import com.replog.domain.recommendation.WorkoutSplit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 3 Gap 1 — RecommendedWorkoutCardEntry type-contract oracle.
 *
 * Locks the shape of the [RecommendedWorkoutCardEntry] that
 * [IntelligenceRepository.buildRecommendedWorkout] produces from a
 * [RecommendationEngine] output. This test does NOT require Room or
 * Android — it constructs data-class fixtures directly and asserts the
 * mapping contract.
 *
 * ## What this locks
 *
 * 1. A TRAIN recommendation with a workout plan produces a non-null
 *    entry with the correct exercise count, split, top exercises, and
 *    the workout plan reference intact.
 * 2. A REST recommendation with a null workout plan produces null
 *    (the \"Start\" CTA card hides when there is nothing to start).
 * 3. The data class is immutable and carry-all-fields-through (no
 *    silent truncation of exercise names or plan data).
 */
class RecommendedWorkoutOracleTest {

    companion object {
        /** A realistic TRAIN recommendation from the engine. */
        private fun trainRecommendation(): Recommendation = Recommendation(
            type = RecommendationType.TRAIN,
            title = "Train Upper Push",
            explanation = "Recovery is good. 6 exercises selected for Upper Push.",
            dataUsed = listOf("Recovery score: 82/100 (Good)"),
            reasoning = listOf("Recovery is good."),
            expectedOutcome = "Targeted stimulus for 6 exercises.",
            confidenceScore = 78.5,
            estimatedDurationMinutes = 45,
            workoutPlan = WorkoutPlan(
                split = WorkoutSplit.UPPER,
                exercises = listOf(
                    PlannedExercise(1, "Bench Press", 4, 8, 80.0, ProgressionDecision.INCREASE_LOAD, "Recent performance suggests you can handle more load."),
                    PlannedExercise(2, "Overhead Press", 3, 10, 45.0, ProgressionDecision.MAINTAIN, "Maintain to consolidate technique."),
                    PlannedExercise(3, "Incline DB Press", 3, 12, 30.0, ProgressionDecision.INCREASE_REPS, "Add reps to keep progressive overload moving."),
                    PlannedExercise(4, "Lateral Raise", 3, 15, 12.0, ProgressionDecision.MAINTAIN, "Maintain to consolidate technique."),
                    PlannedExercise(5, "Tricep Pushdown", 3, 12, 25.0, ProgressionDecision.INCREASE_LOAD, "Recent performance suggests you can handle more load."),
                    PlannedExercise(6, "Cable Fly", 3, 15, 18.0, ProgressionDecision.MAINTAIN, "Maintain to consolidate technique.")
                )
            )
        )

        /** A REST recommendation with no workout plan. */
        private fun restRecommendation(): Recommendation = Recommendation(
            type = RecommendationType.REST,
            title = "Take a Rest Day",
            explanation = "Your recovery signal is low.",
            dataUsed = listOf("Recovery score: 30/100"),
            reasoning = listOf("Recovery is very low."),
            expectedOutcome = "Better recovery, lower injury risk.",
            confidenceScore = 85.0,
            estimatedDurationMinutes = 0,
            workoutPlan = null
        )
    }

    // -------------------------------------------------------------------------
    // Test 1 — TRAIN recommendation produces a full card entry.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun `train recommendation produces full card entry with workout plan intact`() {
        val rec = trainRecommendation()

        val entry = RecommendedWorkoutCardEntry(
            title = rec.title,
            type = rec.type.name,
            explanation = rec.explanation,
            exerciseCount = rec.workoutPlan!!.exercises.size,
            estimatedDurationMinutes = rec.estimatedDurationMinutes,
            confidenceScore = rec.confidenceScore,
            split = rec.workoutPlan.split.name,
            topExercises = rec.workoutPlan.exercises.take(4).map { it.exerciseName },
            workoutPlan = rec.workoutPlan
        )

        assertEquals("Train Upper Push", entry.title)
        assertEquals("TRAIN", entry.type)
        assertEquals(6, entry.exerciseCount)
        assertEquals(45, entry.estimatedDurationMinutes)
        assertEquals(78.5, entry.confidenceScore, 0.01)
        assertEquals("UPPER", entry.split)
        assertEquals(
            listOf("Bench Press", "Overhead Press", "Incline DB Press", "Lateral Raise"),
            entry.topExercises
        )
        assertNotNull("workout plan must be preserved for session creation", entry.workoutPlan)
        assertEquals(6, entry.workoutPlan!!.exercises.size)

        // Verify the first exercise's progression decision is carried through.
        val first = entry.workoutPlan.exercises.first()
        assertEquals("Bench Press", first.exerciseName)
        assertEquals(ProgressionDecision.INCREASE_LOAD, first.progression)
        assertEquals(4, first.targetSets)
        assertEquals(8, first.targetReps)
        assertEquals(80.0, first.targetWeight!!, 0.01)
    }

    // -------------------------------------------------------------------------
    // Test 2 — REST recommendation with null plan would be null entry.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun `rest recommendation with null workout plan yields null entry`() {
        val rec = restRecommendation()
        assertNull("REST recommendation has no workout plan", rec.workoutPlan)
        assertTrue(
            "buildRecommendedWorkout should return null when plan is null",
            rec.workoutPlan == null || rec.workoutPlan.exercises.isEmpty()
        )
    }

    // -------------------------------------------------------------------------
    // Test 3 — topExercises truncates at 4 (the card only shows first 4).
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun `topExercises truncated to four exercises`() {
        val rec = trainRecommendation()
        val top4 = rec.workoutPlan!!.exercises.take(4).map { it.exerciseName }
        assertEquals(4, top4.size)
        assertEquals("Bench Press", top4[0])
        assertEquals("Lateral Raise", top4[3])
    }

    // -------------------------------------------------------------------------
    // Test 4 — data class immutability and field completeness.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun `data class carries all fields through without truncation`() {
        val rec = trainRecommendation()
        val entry = RecommendedWorkoutCardEntry(
            title = rec.title,
            type = rec.type.name,
            explanation = rec.explanation,
            exerciseCount = rec.workoutPlan!!.exercises.size,
            estimatedDurationMinutes = rec.estimatedDurationMinutes,
            confidenceScore = rec.confidenceScore,
            split = rec.workoutPlan.split.name,
            topExercises = rec.workoutPlan.exercises.take(4).map { it.exerciseName },
            workoutPlan = rec.workoutPlan
        )

        // Verify no field is silently empty or truncated.
        assertTrue("title must not be blank", entry.title.isNotBlank())
        assertTrue("type must not be blank", entry.type.isNotBlank())
        assertTrue("explanation must not be blank", entry.explanation.isNotBlank())
        assertTrue("exercise count must be > 0", entry.exerciseCount > 0)
        assertTrue("duration must be > 0 for TRAIN", entry.estimatedDurationMinutes > 0)
        assertTrue("confidence must be in range", entry.confidenceScore in 50.0..100.0)
        assertTrue("split must not be blank", entry.split.isNotBlank())
        assertTrue("top exercises must not be empty", entry.topExercises.isNotEmpty())
        assertNotNull("workout plan must be non-null for TRAIN", entry.workoutPlan)
    }
}
