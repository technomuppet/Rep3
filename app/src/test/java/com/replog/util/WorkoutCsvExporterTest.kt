package com.replog.util

import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.WorkoutPrescription
import com.replog.data.model.WorkoutSession
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCsvExporterTest {
    @Test
    fun exportsPrescriptionColumnsAndTargetHit() {
        val session = sampleSession()
        val prescription = WorkoutPrescription(
            sessionId = 1,
            exerciseId = 10,
            source = "Template",
            targetSets = 1,
            targetReps = 5,
            targetWeight = 100.0,
            adjustment = "Programmed",
            reason = "Test target"
        )

        val csv = WorkoutCsvExporter.toCsv(
            sessions = listOf(session),
            prescriptionsBySessionId = mapOf(1 to listOf(prescription))
        )

        assertTrue(csv.contains("prescription_source,target_sets,target_reps,target_weight,target_hit"))
        assertTrue(csv.contains("\"Template\",1,5,100.0,true"))
        assertTrue(csv.contains("\"Bench Press\""))
    }

    private fun sampleSession(): SessionWithExercises {
        val exercise = Exercise(
            id = 10,
            name = "Bench Press",
            category = "Chest",
            equipment = "Barbell",
            movementPattern = "Push • Horizontal Press",
            primaryMuscles = "Pectorals",
            secondaryMuscles = "Triceps"
        )
        val sessionExercise = SessionExercise(
            id = 20,
            sessionId = 1,
            exerciseId = 10,
            orderIndex = 0,
            supersetGroup = "A"
        )
        return SessionWithExercises(
            session = WorkoutSession(id = 1, templateName = "Test", startTime = 100L, endTime = 200L),
            exercises = listOf(
                SessionExerciseWithSets(
                    sessionExercise = sessionExercise,
                    exercise = exercise,
                    sets = listOf(
                        SetLog(
                            sessionExerciseId = 20,
                            setNumber = 1,
                            weight = 100.0,
                            reps = 5,
                            rpe = 8.0
                        )
                    )
                )
            )
        )
    }
}
