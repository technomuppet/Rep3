package com.replog.widget

import com.replog.data.model.WorkoutSession
import com.replog.data.model.SessionWithExercises
import com.replog.util.AdaptiveWorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateFactoryTest {
    @Test
    fun quickStartStateReflectsActiveWorkout() {
        val state = WidgetStateFactory.quickStartState(
            activeSession = SessionWithExercises(
                session = WorkoutSession(id = 1, templateName = "Push Day", startTime = 100L),
                exercises = emptyList()
            ),
            adaptivePlan = null
        )

        assertTrue(state.hasActiveWorkout)
        assertEquals("Push Day", state.activeWorkoutName)
    }

    @Test
    fun todaysWorkoutStateReflectsAdaptivePlan() {
        val plan = AdaptiveWorkoutPlan(
            title = "Push Day vNext",
            summary = "Adaptive summary",
            sourceSessionId = 1,
            targets = emptyList(),
            fatigueModifier = "Maintain"
        )

        val state = WidgetStateFactory.todaysWorkoutState(plan)

        assertTrue(state.isAdaptive)
        assertEquals("Push Day vNext", state.title)
    }

    @Test
    fun todaysWorkoutStateFallsBackWithoutPlan() {
        val state = WidgetStateFactory.todaysWorkoutState(null)

        assertFalse(state.isAdaptive)
        assertEquals("Start workout", state.title)
    }
}
