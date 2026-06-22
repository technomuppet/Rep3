package com.replog.domain.recommendation

import com.replog.data.model.Exercise
import com.replog.data.model.PlateauEvent
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.TrainingDnaProgressionScore
import com.replog.data.model.TrainingDnaSnapshot

data class RecommendationContext(
    val sessions: List<SessionWithExercises>,
    val exercises: List<Exercise>,
    val dnaSnapshot: TrainingDnaSnapshot?,
    val plateauEvents: List<PlateauEvent>,
    val progressionScores: List<TrainingDnaProgressionScore>,
    val bodyweights: List<com.replog.data.model.BodyweightLog>,
    val nowMillis: Long
)

enum class RecommendationType {
    TRAIN,
    REST,
    DELOAD,
    REPEAT,
    PROGRESS
}

enum class WorkoutSplit {
    FULL_BODY,
    UPPER,
    LOWER,
    PUSH,
    PULL,
    LEGS
}

enum class ProgressionDecision {
    INCREASE_LOAD,
    INCREASE_REPS,
    INCREASE_SETS,
    MAINTAIN,
    DELOAD
}

data class WorkoutPlan(
    val split: WorkoutSplit,
    val exercises: List<PlannedExercise>
)

data class PlannedExercise(
    val exerciseId: Int,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double?,
    val progression: ProgressionDecision,
    val reason: String
)

data class Recommendation(
    val type: RecommendationType,
    val title: String,
    val explanation: String,
    val dataUsed: List<String>,
    val reasoning: List<String>,
    val expectedOutcome: String,
    val confidenceScore: Double,
    val estimatedDurationMinutes: Int,
    val workoutPlan: WorkoutPlan? = null
)

data class MuscleRecovery(
    val muscle: String,
    val recoveryScore: Double,
    val lastTrainedMillis: Long?,
    val volumeLast7Days: Double,
    val status: MuscleRecoveryStatus
)

enum class MuscleRecoveryStatus {
    FRESH,
    RECOVERED,
    FATIGUED,
    VERY_FATIGUED
}

data class OverallRecovery(
    val score: Double,
    val label: String,
    val reasons: List<String>
)
