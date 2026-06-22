package com.replog.domain.trainingdna

import com.replog.data.model.SessionWithExercises

object PlateauDetector {

    fun detect(
        sessions: List<SessionWithExercises>,
        config: TrainingDnaConfig = TrainingDnaConfig()
    ): List<PlateauDetection> {
        val completed = sessions.filter { it.session.endTime != null }
        if (completed.isEmpty()) return emptyList()

        return completed
            .flatMap { session -> session.exercises.map { session.session.startTime to it } }
            .groupBy { it.second.exercise.id }
            .mapNotNull { (exerciseId, datedEntries) ->
                val exerciseName = datedEntries.first().second.exercise.name
                val points = datedEntries
                    .sortedBy { it.first }
                    .map { (time, entry) ->
                        val hardSets = entry.sets.filter { it.setType != com.replog.data.model.SetType.WARMUP }
                        ExercisePoint(
                            time = time,
                            bestLoad = hardSets.maxOfOrNull { it.weight } ?: 0.0,
                            bestReps = hardSets.maxOfOrNull { it.reps } ?: 0,
                            totalVolume = hardSets.sumOf { it.weight * it.reps }
                        )
                    }
                detectForExercise(exerciseId, exerciseName, points, config)
            }
    }

    private fun detectForExercise(
        exerciseId: Int,
        exerciseName: String,
        points: List<ExercisePoint>,
        config: TrainingDnaConfig
    ): PlateauDetection? {
        if (points.size < config.minSessionsForPlateau) return null

        val now = config.nowMillis
        val windowStart = now - config.plateauPeriodDays * DAY
        val previousWindowStart = windowStart - config.plateauPeriodDays * DAY

        val recentPoints = points.filter { it.time in windowStart..now }
        if (recentPoints.size < 2) return null

        val previousPoints = points.filter { it.time in previousWindowStart until windowStart }
        val baseline = previousPoints.lastOrNull() ?: points.lastOrNull { it.time < windowStart } ?: return null

        val recentBestLoad = recentPoints.maxOfOrNull { it.bestLoad } ?: 0.0
        val recentBestReps = recentPoints.maxOfOrNull { it.bestReps } ?: 0
        val recentBestVolume = recentPoints.maxOfOrNull { it.totalVolume } ?: 0.0

        val reasons = mutableListOf<PlateauReason>()
        if (recentBestLoad <= baseline.bestLoad) reasons += PlateauReason.NO_LOAD_INCREASE
        if (recentBestReps <= baseline.bestReps) reasons += PlateauReason.NO_REP_INCREASE
        if (recentBestVolume <= baseline.totalVolume) reasons += PlateauReason.NO_VOLUME_INCREASE

        if (reasons.isEmpty()) return null

        val latest = recentPoints.lastOrNull() ?: baseline
        return PlateauDetection(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            periodDays = config.plateauPeriodDays,
            reasons = reasons,
            lastLoad = latest.bestLoad,
            lastReps = latest.bestReps,
            lastVolume = latest.totalVolume
        )
    }

    private data class ExercisePoint(
        val time: Long,
        val bestLoad: Double,
        val bestReps: Int,
        val totalVolume: Double
    )

    private const val DAY = 24L * 60L * 60L * 1000L
}
