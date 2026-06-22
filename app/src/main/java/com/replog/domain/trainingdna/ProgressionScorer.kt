package com.replog.domain.trainingdna

import com.replog.data.model.SessionWithExercises
import com.replog.util.PRCalculator

object ProgressionScorer {

    fun calculateExerciseProgression(
        sessions: List<SessionWithExercises>,
        exerciseId: Int,
        exerciseName: String,
        nowMillis: Long
    ): ExerciseProgression {
        val perSession = perSessionBestEstimatedOneRm(sessions, exerciseId)
            .filter { it.second > 0.0 }
            .sortedBy { it.first }

        if (perSession.size < 2) {
            return ExerciseProgression(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                score30Day = 0.0,
                score90Day = 0.0,
                scoreLifetime = 0.0,
                estimatedOneRm30Day = 0.0,
                estimatedOneRm90Day = 0.0,
                estimatedOneRmLifetime = 0.0
            )
        }

        val latest = perSession.last().second
        val first = perSession.first().second
        val e1rm30 = bestBefore(perSession, nowMillis, 30)
        val e1rm90 = bestBefore(perSession, nowMillis, 90)

        return ExerciseProgression(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            score30Day = scoreFor(e1rm30, latest),
            score90Day = scoreFor(e1rm90, latest),
            scoreLifetime = scoreFor(first, latest),
            estimatedOneRm30Day = e1rm30,
            estimatedOneRm90Day = e1rm90,
            estimatedOneRmLifetime = first
        )
    }

    private fun perSessionBestEstimatedOneRm(
        sessions: List<SessionWithExercises>,
        exerciseId: Int
    ): List<Pair<Long, Double>> = sessions
        .filter { it.session.endTime != null }
        .mapNotNull { session ->
            val best = session.exercises
                .filter { it.exercise.id == exerciseId }
                .flatMap { it.sets }
                .filter { it.setType != com.replog.data.model.SetType.WARMUP }
                .maxOfOrNull { PRCalculator.epley1RM(it.weight, it.reps) }
            best?.let { session.session.startTime to it }
        }

    private fun bestBefore(
        points: List<Pair<Long, Double>>,
        nowMillis: Long,
        days: Int
    ): Double {
        val cutoff = nowMillis - days * DAY
        return points
            .filter { it.first <= cutoff }
            .maxOfOrNull { it.second }
            ?: points.first().second
    }

    private fun scoreFor(previous: Double, current: Double): Double {
        if (previous <= 0.0) return 0.0
        return (current - previous) / previous
    }

    private const val DAY = 24L * 60L * 60L * 1000L
}
