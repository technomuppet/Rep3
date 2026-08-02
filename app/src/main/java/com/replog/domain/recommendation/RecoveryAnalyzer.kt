package com.replog.domain.recommendation

import com.replog.data.model.BodyweightLog
import com.replog.data.model.SetLog
import com.replog.data.model.SetType
import com.replog.data.model.SessionWithExercises

object RecoveryAnalyzer {

    fun overallRecovery(
        sessions: List<SessionWithExercises>,
        bodyweights: List<BodyweightLog>,
        nowMillis: Long
    ): OverallRecovery {
        val completed = sessions
            .filter { session ->
                val endTime = session.session.endTime
                endTime != null && endTime <= nowMillis && session.hasHardSets()
            }
            .sortedByDescending { it.session.endTime ?: Long.MIN_VALUE }
        if (completed.size < 2) {
            return OverallRecovery(
                score = 75.0,
                label = "Normal",
                reasons = listOf("Not enough history to calculate a strong recovery signal.")
            )
        }

        val lastSession = completed.firstOrNull()
        val hoursSinceLastSession = lastSession?.let {
            ((nowMillis - (it.session.endTime ?: it.session.startTime)).coerceAtLeast(0L)) / HOUR_MS.toDouble()
        } ?: Double.MAX_VALUE

        val last7 = completed.filter { it.trainingTime() in (nowMillis - 7L * DAY)..nowMillis }
        val previous7 = completed.filter {
            it.trainingTime() in (nowMillis - 14L * DAY) until (nowMillis - 7L * DAY)
        }
        val frequency7 = last7.size
        val frequency14 = completed.count { it.trainingTime() >= nowMillis - 14L * DAY }

        // Compare equal rolling windows instead of calendar weeks. This avoids a
        // false workload drop when the user checks Recovery Centre mid-week.
        val recentVolume = last7.sumOf { it.volume() }
        val previousVolume = previous7.sumOf { it.volume() }
        val volumeRatio = if (previousVolume > 0.0) recentVolume / previousVolume else null

        val hardSetsLast7 = last7.flatMap { it.hardSets() }
        val highRpeSetsLast7 = hardSetsLast7.count { it.rpeValue() >= HIGH_RPE }
        val avgRpeLast7 = hardSetsLast7.mapNotNull { it.rpeValueOrNull() }.takeIf { it.isNotEmpty() }?.average()
        val rpeDensity = if (hardSetsLast7.isNotEmpty()) {
            highRpeSetsLast7.toDouble() / hardSetsLast7.size
        } else {
            0.0
        }

        val bwTrend = bodyweightTrend(bodyweights)

        var score = 75
        val reasons = mutableListOf<String>()

        if (frequency7 >= 6) {
            score -= 20
            reasons += "Very high training frequency this week"
        } else if (frequency7 in 4..5) {
            score -= 8
            reasons += "Above-average frequency this week"
        } else if (frequency7 <= 1) {
            score += 8
            reasons += "Low frequency this week"
        }

        if (frequency14 >= 10) {
            score -= 10
            reasons += "Dense two-week workload"
        }

        when {
            volumeRatio != null && volumeRatio > 1.35 -> {
                score -= 18
                reasons += "Recent 7-day volume jumped ${((volumeRatio - 1.0) * 100).toInt()}%"
            }
            volumeRatio != null && volumeRatio < 0.65 -> {
                score += 8
                reasons += "Recent 7-day volume dropped"
            }
        }

        if (avgRpeLast7 != null && avgRpeLast7 >= 9.0) {
            score -= 15
            reasons += "Average RPE is very high"
        } else if (avgRpeLast7 != null && avgRpeLast7 <= 7.5) {
            score += 8
            reasons += "Average RPE is manageable"
        }

        // Density is more informative than a raw count, but require a modest
        // sample so one maximal set cannot dominate a whole-week signal.
        if (hardSetsLast7.size >= MIN_SETS_FOR_DENSITY && rpeDensity >= HIGH_RPE_DENSITY) {
            score -= 10
            reasons += "A high proportion of recent sets were high-RPE"
        }

        when {
            hoursSinceLastSession < 18 -> {
                score -= 15
                reasons += "Last session ended very recently"
            }
            hoursSinceLastSession in 18.0..36.0 -> {
                score -= 5
                reasons += "Last session ended recently"
            }
            hoursSinceLastSession > 72 -> {
                score += 8
                reasons += "Long recovery window since last session"
            }
        }

        if (bwTrend < -0.7) {
            score -= 10
            reasons += "Bodyweight is trending down"
        } else if (bwTrend > 0.5) {
            score += 3
            reasons += "Bodyweight is stable or trending up"
        }

        score = score.coerceIn(0, 100)
        val label = when {
            score >= 80 -> "Recovered"
            score >= 60 -> "Normal"
            score >= 40 -> "Fatigued"
            else -> "Very Fatigued"
        }

        return OverallRecovery(
            score = score.toDouble(),
            label = label,
            reasons = reasons.takeIf { it.isNotEmpty() } ?: listOf("Training load looks steady.")
        )
    }

    fun muscleRecovery(
        sessions: List<SessionWithExercises>,
        nowMillis: Long
    ): List<MuscleRecovery> {
        val completed = sessions.filter { session ->
            val endTime = session.session.endTime
            endTime != null && endTime <= nowMillis && session.hasHardSets()
        }
        if (completed.isEmpty()) return emptyList()

        data class MuscleSession(
            val startTime: Long,
            val endTime: Long,
            val sets: List<SetLog>
        )

        val muscleData = mutableMapOf<String, MutableList<MuscleSession>>()
        completed.forEach { session ->
            session.exercises.forEach { entry ->
                val hardSets = entry.sets.hardSets()
                if (hardSets.isEmpty()) return@forEach
                val primary = splitCsv(entry.exercise.primaryMuscles.ifBlank { entry.exercise.muscles })
                val secondary = splitCsv(entry.exercise.secondaryMuscles)
                val muscleSession = MuscleSession(
                    startTime = session.session.startTime,
                    endTime = session.session.endTime ?: session.session.startTime,
                    sets = hardSets
                )
                (primary + secondary).distinct().forEach { muscle ->
                    muscleData.getOrPut(muscle) { mutableListOf() } += muscleSession
                }
            }
        }

        return muscleData.map { (muscle, sessionsForMuscle) ->
            val sorted = sessionsForMuscle.sortedByDescending { it.endTime }
            val latest = sorted.firstOrNull()
            val lastTrained = latest?.endTime
            val last7 = sorted.filter { it.endTime in (nowMillis - 7L * DAY)..nowMillis }
            val hardSetsLast7 = last7.flatMap { it.sets.hardSets() }
            // Keep tonnage as a display/context metric, but never use it as the
            // fatigue score: a squat's kilograms must not outweigh ten light curls.
            val volumeLast7 = hardSetsLast7.sumOf { it.weight * it.reps }
            val highRpeSets = hardSetsLast7.count { it.rpeValue() >= HIGH_RPE }
            val rpeDensity = if (hardSetsLast7.isNotEmpty()) {
                highRpeSets.toDouble() / hardSetsLast7.size
            } else {
                0.0
            }
            val hoursSinceLastTrained = latest?.let {
                ((nowMillis - it.endTime).coerceAtLeast(0L)) / HOUR_MS.toDouble()
            } ?: Double.MAX_VALUE

            val timeScore = when {
                hoursSinceLastTrained > 72 -> 90.0
                hoursSinceLastTrained > 48 -> 78.0
                hoursSinceLastTrained > 24 -> 60.0
                else -> 38.0
            }
            val workloadPenalty = when {
                hoursSinceLastTrained <= 24 -> (hardSetsLast7.size / 12.0 * 22.0).coerceIn(0.0, 22.0)
                hoursSinceLastTrained <= 48 -> (hardSetsLast7.size / 16.0 * 16.0).coerceIn(0.0, 16.0)
                hoursSinceLastTrained <= 72 -> (hardSetsLast7.size / 20.0 * 10.0).coerceIn(0.0, 10.0)
                else -> (hardSetsLast7.size / 24.0 * 5.0).coerceIn(0.0, 5.0)
            }
            val effortPenalty = if (hardSetsLast7.size >= MIN_SETS_FOR_DENSITY) {
                (rpeDensity * 10.0).coerceIn(0.0, 10.0)
            } else {
                0.0
            }
            val score = (timeScore - workloadPenalty - effortPenalty).coerceIn(0.0, 100.0)

            val status = when {
                score >= 80 -> MuscleRecoveryStatus.FRESH
                score >= 60 -> MuscleRecoveryStatus.RECOVERED
                score >= 40 -> MuscleRecoveryStatus.FATIGUED
                else -> MuscleRecoveryStatus.VERY_FATIGUED
            }

            MuscleRecovery(
                muscle = muscle,
                recoveryScore = score,
                lastTrainedMillis = lastTrained,
                volumeLast7Days = volumeLast7,
                status = status
            )
        }.sortedByDescending { it.recoveryScore }
    }

    private fun SessionWithExercises.volume(): Double =
        exercises.flatMap { it.sets.hardSets() }.sumOf { it.weight * it.reps }

    private fun SessionWithExercises.hasHardSets(): Boolean =
        exercises.any { it.sets.hardSets().isNotEmpty() }

    private fun SessionWithExercises.trainingTime(): Long =
        session.endTime ?: session.startTime

    private fun SessionWithExercises.hardSets(): List<SetLog> =
        exercises.flatMap { it.sets.hardSets() }

    private fun List<SetLog>.hardSets(): List<SetLog> =
        filter { it.completed && it.setType != SetType.WARMUP && it.reps > 0 }

    private fun SetLog.rpeValueOrNull(): Double? = rpe?.takeIf { it.isFinite() }?.coerceIn(1.0, 10.0)

    private fun SetLog.rpeValue(): Double = rpeValueOrNull() ?: 0.0

    private fun bodyweightTrend(bodyweights: List<BodyweightLog>): Double {
        val ordered = bodyweights
            .filter { it.weight.isFinite() && it.weight > 0.0 }
            .sortedBy { it.timestamp }
        if (ordered.size < 4) return 0.0
        val recent = ordered.takeLast(2).map { it.weight }.average()
        val previous = ordered.dropLast(2).takeLast(2).map { it.weight }.average()
        return recent - previous
    }

    private fun splitCsv(value: String): List<String> =
        value.split(",").map { it.trim() }.filter { it.isNotBlank() }

    private const val DAY = 24L * 60L * 60L * 1000L
    private const val HOUR_MS = 60L * 60L * 1000L
    private const val HIGH_RPE = 9.0
    private const val HIGH_RPE_DENSITY = 0.35
    private const val MIN_SETS_FOR_DENSITY = 6
}
