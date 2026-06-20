package com.replog.domain.analytics

import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetType

interface AnalyticsEngine {
    fun volumeSummary(sessions: List<SessionWithExercises>): VolumeSummary
    fun exerciseEstimatedOneRepMaxTrend(sessions: List<SessionWithExercises>, exerciseId: Int): List<ProgressionPoint>
    fun trend(points: List<ProgressionPoint>): TrendSummary
}

class DefaultAnalyticsEngine : AnalyticsEngine {
    override fun volumeSummary(sessions: List<SessionWithExercises>): VolumeSummary {
        val sets = sessions.flatMap { session -> session.exercises.flatMap { it.sets } }
        val hardSets = sets.filter { it.setType != SetType.WARMUP }
        return VolumeSummary(
            totalVolume = sets.sumOf { it.weight * it.reps },
            totalSets = sets.size,
            totalReps = sets.sumOf { it.reps },
            hardSetCount = hardSets.size
        )
    }

    override fun exerciseEstimatedOneRepMaxTrend(
        sessions: List<SessionWithExercises>,
        exerciseId: Int
    ): List<ProgressionPoint> = sessions
        .filter { it.session.endTime != null }
        .mapNotNull { session ->
            val best = session.exercises
                .filter { it.exercise.id == exerciseId }
                .flatMap { it.sets }
                .maxOfOrNull { it.estimatedOneRepMax() }
            best?.let { ProgressionPoint(session.session.startTime, it) }
        }
        .sortedBy { it.timestamp }

    override fun trend(points: List<ProgressionPoint>): TrendSummary {
        if (points.size < 2) return TrendSummary(TrendDirection.INSUFFICIENT_DATA, 0.0, 0.0)
        val first = points.first().value
        val last = points.last().value
        val change = last - first
        val percent = if (first == 0.0) 0.0 else change / first
        val direction = when {
            kotlin.math.abs(percent) < 0.01 -> TrendDirection.FLAT
            change > 0 -> TrendDirection.UP
            else -> TrendDirection.DOWN
        }
        return TrendSummary(direction, change, percent)
    }
}
