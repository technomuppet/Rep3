package com.replog.util

import com.replog.data.model.BodyweightLog
import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.WorkoutPrescription
import com.replog.data.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupJsonTest {
    @Test
    fun emptyBackupRoundTrips() {
        val json = BackupJson.encode(emptyList(), listOf(BodyweightLog(weight = 82.5, timestamp = 1234L, note = "test")))
        val decoded = BackupJson.decode(json)
        assertEquals(4, decoded.schemaVersion)
        assertEquals(1, decoded.bodyweights.size)
        assertEquals(82.5, decoded.bodyweights.first().weight, 0.001)
    }

    @Test
    fun backupIncludesPrescriptions() {
        val exercise = Exercise(id = 7, name = "Bench Press", category = "Chest", equipment = "Barbell")
        val session = SessionWithExercises(
            session = WorkoutSession(id = 3, templateName = "Push", startTime = 100L, endTime = 200L),
            exercises = listOf(
                SessionExerciseWithSets(
                    sessionExercise = SessionExercise(id = 4, sessionId = 3, exerciseId = 7, orderIndex = 0),
                    sets = listOf(SetLog(sessionExerciseId = 4, setNumber = 1, weight = 100.0, reps = 5)),
                    exercise = exercise
                )
            )
        )
        val prescription = WorkoutPrescription(
            sessionId = 3,
            exerciseId = 7,
            source = "Adaptive",
            targetSets = 3,
            targetReps = 5,
            targetWeight = 100.0,
            adjustment = "Add 1 rep",
            reason = "Test"
        )

        val json = BackupJson.encode(
            sessions = listOf(session),
            bodyweights = emptyList(),
            prescriptionsBySessionId = mapOf(3 to listOf(prescription))
        )
        val decoded = BackupJson.decode(json)
        assertEquals(1, decoded.sessions.size)
        assertEquals(1, decoded.sessions.first().prescriptions.size)
        assertEquals("Bench Press", decoded.sessions.first().prescriptions.first().exerciseName)
        assertEquals(3, decoded.sessions.first().prescriptions.first().targetSets)
    }
}
