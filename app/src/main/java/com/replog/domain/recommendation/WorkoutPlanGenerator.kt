package com.replog.domain.recommendation

import com.replog.data.model.Exercise
import com.replog.data.model.PlateauEvent
import com.replog.data.model.SessionWithExercises

object WorkoutPlanGenerator {

    private val splitMuscleGroups = mapOf(
        WorkoutSplit.PUSH to setOf("chest", "pectorals", "shoulders", "deltoids", "triceps", "front delt"),
        WorkoutSplit.PULL to setOf("back", "lats", "latissimus", "traps", "rhomboids", "biceps", "rear delt"),
        WorkoutSplit.LEGS to setOf("quads", "quadriceps", "hamstrings", "glutes", "calves", "adductors"),
        WorkoutSplit.UPPER to setOf("chest", "pectorals", "shoulders", "deltoids", "triceps", "back", "lats", "latissimus", "biceps", "rear delt"),
        WorkoutSplit.LOWER to setOf("quads", "quadriceps", "hamstrings", "glutes", "calves", "adductors"),
        WorkoutSplit.FULL_BODY to setOf("chest", "pectorals", "back", "lats", "quads", "quadriceps", "hamstrings", "glutes")
    )

    private val splitNames = mapOf(
        WorkoutSplit.FULL_BODY to "Full Body",
        WorkoutSplit.UPPER to "Upper Body",
        WorkoutSplit.LOWER to "Lower Body",
        WorkoutSplit.PUSH to "Push",
        WorkoutSplit.PULL to "Pull",
        WorkoutSplit.LEGS to "Legs"
    )

    fun generate(
        split: WorkoutSplit,
        exercises: List<Exercise>,
        muscleRecovery: List<MuscleRecovery>,
        stalledExercises: List<PlateauEvent>,
        undertrainedMuscles: List<String>,
        recentSessions: List<SessionWithExercises>,
        dnaRepRange: String
    ): WorkoutPlan {
        val targetMuscles = splitMuscleGroups[split] ?: emptySet()
        val candidates = exercises.filter { exercise ->
            val muscles = (exercise.primaryMuscles + "," + exercise.secondaryMuscles + "," + exercise.muscles)
                .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
            muscles.any { it in targetMuscles }
        }

        val fatiguedMuscles = muscleRecovery
            .filter { it.status == MuscleRecoveryStatus.FATIGUED || it.status == MuscleRecoveryStatus.VERY_FATIGUED }
            .map { it.muscle.lowercase() }
            .toSet()

        val recentlyUsedExerciseIds = recentSessions
            .takeLast(7)
            .flatMap { it.exercises }
            .map { it.exercise.id }
            .toSet()

        val stalledExerciseIds = stalledExercises.map { it.exerciseId }.toSet()

        val scored = candidates.map { exercise ->
            val muscles = (exercise.primaryMuscles + "," + exercise.secondaryMuscles + "," + exercise.muscles)
                .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
            val score = when {
                exercise.id in stalledExerciseIds -> 100.0
                exercise.id in recentlyUsedExerciseIds -> 30.0
                muscles.any { it in fatiguedMuscles } -> 20.0
                muscles.any { it in undertrainedMuscles.map { it.lowercase() } } -> 80.0
                else -> 50.0
            }
            exercise to score
        }.sortedByDescending { it.second }

        val selected = scored.take(6).map { it.first }

        return WorkoutPlan(
            split = split,
            exercises = selected.map { exercise ->
                val baseReps = repTargetFromDna(dnaRepRange)
                val baseSets = 3
                PlannedExercise(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    targetSets = baseSets,
                    targetReps = baseReps,
                    targetWeight = null,
                    progression = ProgressionDecision.MAINTAIN,
                    reason = "Selected for ${splitNames[split]} focus."
                )
            }
        )
    }

    fun splitName(split: WorkoutSplit): String = splitNames[split] ?: split.name

    fun repTargetFromDna(dnaRepRange: String): Int = when {
        dnaRepRange.contains("1–3") -> 3
        dnaRepRange.contains("4–6") -> 6
        dnaRepRange.contains("7–10") -> 8
        dnaRepRange.contains("11–15") -> 12
        dnaRepRange.contains("16+") -> 15
        else -> 8
    }
}
