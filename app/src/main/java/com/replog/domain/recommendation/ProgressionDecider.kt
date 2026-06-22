package com.replog.domain.recommendation

import com.replog.data.model.Exercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.util.PRCalculator

object ProgressionDecider {

    fun decide(
        exercise: Exercise,
        sessions: List<SessionWithExercises>,
        muscleRecovery: MuscleRecovery?,
        isStalled: Boolean,
        overallRecoveryScore: Double
    ): ProgressionDecision {
        val history = exerciseHistory(exercise, sessions)
        if (history.isEmpty()) return ProgressionDecision.MAINTAIN

        val lastSession = history.lastOrNull() ?: return ProgressionDecision.MAINTAIN
        val lastHardSets = lastSession.sets.filter { it.setType != com.replog.data.model.SetType.WARMUP }
        if (lastHardSets.isEmpty()) return ProgressionDecision.MAINTAIN

        val bestSet = lastHardSets.maxByOrNull { PRCalculator.epley1RM(it.weight, it.reps) }
        val avgRpe = lastHardSets.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()

        return when {
            isStalled && overallRecoveryScore < 55 -> ProgressionDecision.DELOAD
            isStalled -> decideStalledVariation(history, bestSet, avgRpe)
            muscleRecovery?.status == MuscleRecoveryStatus.VERY_FATIGUED -> ProgressionDecision.MAINTAIN
            avgRpe != null && avgRpe >= 9.0 -> ProgressionDecision.MAINTAIN
            avgRpe != null && avgRpe <= 7.5 && bestSet != null && bestSet.reps >= 8 -> ProgressionDecision.INCREASE_LOAD
            bestSet != null && bestSet.reps >= 10 -> ProgressionDecision.INCREASE_LOAD
            bestSet != null && bestSet.reps >= 6 -> ProgressionDecision.INCREASE_REPS
            overallRecoveryScore >= 70 -> ProgressionDecision.INCREASE_SETS
            else -> ProgressionDecision.MAINTAIN
        }
    }

    private fun decideStalledVariation(
        history: List<SessionExerciseWithSets>,
        bestSet: SetLog?,
        avgRpe: Double?
    ): ProgressionDecision {
        val recentVolumes = history.takeLast(4).map { it.sets.filter { it.setType != com.replog.data.model.SetType.WARMUP }.sumOf { it.weight * it.reps } }
        val volumeTrend = if (recentVolumes.size >= 2 && recentVolumes.first() > 0) {
            recentVolumes.last() / recentVolumes.first()
        } else 1.0

        return when {
            avgRpe != null && avgRpe >= 9.0 -> ProgressionDecision.DELOAD
            volumeTrend > 1.15 -> ProgressionDecision.MAINTAIN
            bestSet != null && bestSet.reps >= 10 -> ProgressionDecision.INCREASE_LOAD
            bestSet != null && bestSet.reps >= 6 -> ProgressionDecision.INCREASE_REPS
            else -> ProgressionDecision.INCREASE_SETS
        }
    }

    private fun exerciseHistory(exercise: Exercise, sessions: List<SessionWithExercises>): List<SessionExerciseWithSets> =
        sessions
            .filter { it.session.endTime != null }
            .flatMap { it.exercises }
            .filter { it.exercise.id == exercise.id }
            .sortedBy { it.sets.minOfOrNull { it.timestamp } ?: 0L }
}
