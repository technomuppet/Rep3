package com.replog.util

import com.replog.data.model.BodyweightLog
import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SetLog
import com.replog.data.model.SetType
import com.replog.data.model.WorkoutSession
import com.replog.data.repository.BodyweightRepository
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.WorkoutRepository

object DemoDataGenerator {
    suspend fun generate(
        exerciseRepository: ExerciseRepository,
        workoutRepository: WorkoutRepository,
        bodyweightRepository: BodyweightRepository
    ): Int {
        val required = listOf(
            "Barbell Bench Press",
            "Barbell Back Squat",
            "Barbell Deadlift",
            "Barbell Overhead Press",
            "Barbell Bent Over Row",
            "Pull Ups"
        ).mapNotNull { exerciseRepository.getExerciseByName(it) }

        if (required.size < 4) return 0

        val now = System.currentTimeMillis()
        val day = 24L * 60L * 60L * 1000L
        var sessionsCreated = 0

        repeat(10) { index ->
            val start = now - ((10 - index) * 3L * day)
            val sessionId = workoutRepository.insertSession(
                WorkoutSession(
                    templateName = if (index % 2 == 0) "Demo Strength A" else "Demo Strength B",
                    startTime = start,
                    endTime = start + 58L * 60L * 1000L,
                    notes = if (index % 3 == 0) "Felt strong" else null,
                    qualityScore = 75 + index,
                    totalVolume = 0.0,
                    totalSets = 0,
                    totalReps = 0,
                    prCount = 0
                )
            ).toInt()

            val exercises = if (index % 2 == 0) required.take(3) else required.drop(3).take(3)
            exercises.forEachIndexed { order, exercise ->
                val sessionExerciseId = workoutRepository.insertSessionExercise(
                    SessionExercise(
                        sessionId = sessionId,
                        exerciseId = exercise.id,
                        orderIndex = order,
                        supersetGroup = if (order in 1..2) "A" else null,
                        notes = if (order == 0) "Focus form" else ""
                    )
                ).toInt()
                seedSets(workoutRepository, sessionExerciseId, exercise, index, start)
            }
            sessionsCreated++
        }

        repeat(8) { index ->
            bodyweightRepository.insertBodyweight(
                BodyweightLog(
                    weight = 82.0 - index * 0.15,
                    timestamp = now - ((8 - index) * 4L * day),
                    note = "Demo bodyweight"
                )
            )
        }

        return sessionsCreated
    }

    private suspend fun seedSets(
        workoutRepository: WorkoutRepository,
        sessionExerciseId: Int,
        exercise: Exercise,
        blockIndex: Int,
        startTime: Long
    ) {
        val base = baseWeight(exercise.name) + blockIndex * 1.25
        workoutRepository.insertSet(
            SetLog(
                sessionExerciseId = sessionExerciseId,
                setNumber = 1,
                weight = base * 0.6,
                reps = 8,
                isPR = false,
                prType = null,
                setType = SetType.WARMUP,
                timestamp = startTime + 5_000L,
                completed = true
            )
        )
        repeat(3) { setIndex ->
            val isPr = blockIndex >= 8 && setIndex == 2
            workoutRepository.insertSet(
                SetLog(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = setIndex + 2,
                    weight = base,
                    reps = 5 + (blockIndex % 3),
                    rpe = 7.5 + (setIndex * 0.5),
                    isPR = isPr,
                    prType = if (isPr) "weight" else null,
                    setType = if (setIndex == 2 && blockIndex % 4 == 0) SetType.AMRAP else SetType.WORKING,
                    timestamp = startTime + ((setIndex + 2) * 90_000L),
                    completed = true
                )
            )
        }
    }

    private fun baseWeight(name: String): Double = when {
        name.contains("Squat", true) -> 110.0
        name.contains("Deadlift", true) -> 140.0
        name.contains("Bench", true) -> 85.0
        name.contains("Overhead", true) -> 55.0
        name.contains("Row", true) -> 75.0
        name.contains("Pull", true) -> 0.0
        else -> 50.0
    }
}
