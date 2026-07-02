package com.replog.util

import com.replog.data.model.BodyweightLog
import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.SetType
import com.replog.data.model.WorkoutPrescription
import com.replog.data.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupJsonFullRoundTripTest {
    @Test
    fun roundTripPreservesAdvancedWorkoutData() {
        val session = sampleSession()
        val prescription = WorkoutPrescription(
            sessionId = 1,
            exerciseId = 10,
            source = "Adaptive",
            targetSets = 3,
            targetReps = 6,
            targetWeight = 102.5,
            adjustment = "Add 1 rep",
            reason = "Controlled overload",
            createdAt = 999L
        )
        val json = BackupJson.encode(
            sessions = listOf(session),
            bodyweights = listOf(BodyweightLog(weight = 82.5, timestamp = 500L, note = "morning")),
            prescriptionsBySessionId = mapOf(1 to listOf(prescription))
        )

        val decoded = BackupJson.decode(json)
        val restoredSession = decoded.sessions.first()
        val restoredExercise = restoredSession.exercises.first()
        val restoredSet = restoredExercise.sets.first()
        val restoredPrescription = restoredSession.prescriptions.first()

        assertEquals(4, decoded.schemaVersion)
        assertEquals("Push Day", restoredSession.templateName)
        assertEquals("Bench Press", restoredExercise.exerciseName)
        assertEquals("Push • Horizontal Press", restoredExercise.movementPattern)
        assertEquals("exercise_media/bench.gif", restoredExercise.mediaAsset)
        assertEquals("A", restoredExercise.supersetGroup)
        assertEquals(SetType.AMRAP, restoredSet.setType)
        assertEquals(9.0, restoredSet.rpe ?: 0.0, 0.001)
        assertEquals("3-1-1", restoredSet.tempo)
        assertEquals("Adaptive", restoredPrescription.source)
        assertEquals(102.5, restoredPrescription.targetWeight ?: 0.0, 0.001)
        assertNotNull(decoded.bodyweights.first())
    }

    private fun sampleSession(): SessionWithExercises {
        val exercise = Exercise(
            id = 10,
            name = "Bench Press",
            category = "Chest",
            equipment = "Barbell",
            muscles = "Pectorals, Triceps",
            primaryMuscles = "Pectorals",
            secondaryMuscles = "Triceps",
            movementPattern = "Push • Horizontal Press",
            difficulty = "Intermediate",
            mediaAsset = "exercise_media/bench.gif"
        )
        val sessionExercise = SessionExercise(
            id = 20,
            sessionId = 1,
            exerciseId = 10,
            orderIndex = 0,
            supersetGroup = "A"
        )
        return SessionWithExercises(
            session = WorkoutSession(id = 1, templateName = "Push Day", startTime = 100L, endTime = 200L, notes = "good"),
            exercises = listOf(
                SessionExerciseWithSets(
                    sessionExercise = sessionExercise,
                    exercise = exercise,
                    sets = listOf(
                        SetLog(
                            sessionExerciseId = 20,
                            setNumber = 1,
                            weight = 100.0,
                            reps = 8,
                            isPR = true,
                            timestamp = 150L,
                            setType = SetType.AMRAP,
                            rpe = 9.0,
                            tempo = "3-1-1"
                        )
                    )
                )
            )
        )
    }
}
