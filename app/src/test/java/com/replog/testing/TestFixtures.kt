package com.replog.testing

import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.WorkoutSession

object TestFixtures {
    fun exercise(id: Int = 1, name: String = "Bench Press"): Exercise = Exercise(
        id = id,
        name = name,
        category = "Chest",
        equipment = "Barbell",
        movementPattern = "Push • Horizontal Press",
        primaryMuscles = "Pectorals",
        secondaryMuscles = "Triceps"
    )

    fun session(
        id: Int = 1,
        exercise: Exercise = exercise(),
        sets: List<SetLog> = listOf(set())
    ): SessionWithExercises {
        val sessionExercise = SessionExercise(id = id, sessionId = id, exerciseId = exercise.id, orderIndex = 0)
        return SessionWithExercises(
            session = WorkoutSession(id = id, templateName = "Test", startTime = id * 1000L, endTime = id * 1000L + 500L),
            exercises = listOf(SessionExerciseWithSets(sessionExercise, sets, exercise))
        )
    }

    fun set(
        sessionExerciseId: Int = 1,
        setNumber: Int = 1,
        weight: Double = 100.0,
        reps: Int = 5
    ): SetLog = SetLog(
        sessionExerciseId = sessionExerciseId,
        setNumber = setNumber,
        weight = weight,
        reps = reps
    )
}
