package com.replog.data.model

data class ExerciseSetHistory(
    val setId: Int,
    val sessionId: Int,
    val exerciseId: Int,
    val exerciseName: String,
    val workoutStartTime: Long,
    val weight: Double,
    val reps: Int,
    val isPR: Boolean
) {
    val volume: Double get() = weight * reps
    val estimatedOneRm: Double get() = if (reps <= 1) weight else weight * (1.0 + reps / 30.0)
}

data class ExerciseInsight(
    val exercise: Exercise,
    val history: List<ExerciseSetHistory>,
    val bestWeight: Double,
    val bestReps: Int,
    val bestEstimatedOneRm: Double,
    val bestVolumeSet: Double,
    val totalVolume: Double,
    val totalSets: Int,
    val prCount: Int
)
