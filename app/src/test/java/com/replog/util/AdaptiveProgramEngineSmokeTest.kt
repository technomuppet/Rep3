package com.replog.util

import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.WorkoutSession
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveProgramEngineSmokeTest {
    @Test(timeout = 1_000L)
    fun buildsPlanFromLargeSyntheticHistoryQuickly() {
        val exercise = Exercise(
            id = 1,
            name = "Barbell Bench Press",
            category = "Chest",
            equipment = "Barbell",
            movementPattern = "Push • Horizontal Press",
            primaryMuscles = "Pectorals"
        )
        val now = System.currentTimeMillis()
        val sessions = (0 until 150).map { index ->
            val sessionExercise = SessionExercise(id = index + 1, sessionId = index + 1, exerciseId = 1, orderIndex = 0)
            SessionWithExercises(
                session = WorkoutSession(
                    id = index + 1,
                    templateName = "Synthetic Push",
                    startTime = now - ((150 - index) * 86_400_000L),
                    endTime = now - ((150 - index) * 86_400_000L) + 3_600_000L
                ),
                exercises = listOf(
                    SessionExerciseWithSets(
                        sessionExercise = sessionExercise,
                        exercise = exercise,
                        sets = listOf(
                            SetLog(sessionExerciseId = sessionExercise.id, setNumber = 1, weight = 80.0 + index * 0.1, reps = 5, rpe = 8.0),
                            SetLog(sessionExerciseId = sessionExercise.id, setNumber = 2, weight = 80.0 + index * 0.1, reps = 5, rpe = 8.5),
                            SetLog(sessionExerciseId = sessionExercise.id, setNumber = 3, weight = 80.0 + index * 0.1, reps = 5, rpe = 9.0)
                        )
                    )
                )
            )
        }

        val plan = AdaptiveProgramEngine.buildPlan(sessions)
        assertNotNull(plan)
        assertTrue(plan!!.targets.isNotEmpty())
    }
}
