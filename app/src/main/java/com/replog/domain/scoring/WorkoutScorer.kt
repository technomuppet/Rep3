package com.replog.domain.scoring

import com.replog.data.model.SessionWithExercises
import kotlin.math.roundToInt

data class WorkoutScore(
    val score: Int,
    val completionPct: Int,
    val volume: Double,
    val intensity: Double,
    val consistency: Double,
    val breakdown: String
)

object WorkoutScorer {
    fun score(session: SessionWithExercises, durationMinutes: Long, prCount: Int): WorkoutScore {
        val sets = session.exercises.sumOf { it.sets.size }
        val reps = session.exercises.sumOf { e -> e.sets.sumOf { it.reps } }
        val volume = session.exercises.sumOf { e -> e.sets.sumOf { it.weight * it.reps } }
        val avgRpe = session.exercises.flatMap { it.sets }.mapNotNull { it.rpe }.average().takeIf { !it.isNaN() } ?: 7.0
        
        val completion = 100 // in-workout completion is 100% by definition for logged sets
        val intensityScore = ((avgRpe / 10.0) * 40.0).coerceIn(0.0, 40.0)
        val volumeScore = (kotlin.math.log10(1 + volume / 1000.0) * 15.0).coerceIn(0.0, 30.0)
        val prScore = (prCount * 5.0).coerceAtMost(20.0)
        val densityScore = if (durationMinutes > 0) (sets.toDouble() / durationMinutes * 60.0).coerceIn(0.0, 10.0) else 5.0
        
        val total = (intensityScore + volumeScore + prScore + densityScore).roundToInt().coerceIn(0, 100)
        return WorkoutScore(
            score = total,
            completionPct = completion,
            volume = volume,
            intensity = avgRpe,
            consistency = densityScore,
            breakdown = "Intensity ${intensityScore.roundToInt()}/40 • Volume ${volumeScore.roundToInt()}/30 • PRs ${prScore.roundToInt()}/20 • Density ${densityScore.roundToInt()}/10"
        )
    }
}
