package com.replog.domain.recommendation

import com.replog.data.model.BodyweightLog
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.util.PRCalculator

object RecoveryAnalyzer {

    fun overallRecovery(
        sessions: List<SessionWithExercises>,
        bodyweights: List<BodyweightLog>,
        nowMillis: Long
    ): OverallRecovery {
        if (sessions.size < 2) {
            return OverallRecovery(
                score = 75.0,
                label = "Normal",
                reasons = listOf("Not enough history to calculate a strong recovery signal.")
            )
        }

        val completed = sessions.filter { it.session.endTime != null }.sortedByDescending { it.session.startTime }
        val lastSession = completed.firstOrNull()
        val hoursSinceLastSession = lastSession?.let { (nowMillis - it.session.startTime) / (3_600_000.0) } ?: Double.MAX_VALUE

        val last7 = completed.filter { it.session.startTime >= nowMillis - 7L * DAY }
        val last14 = completed.filter { it.session.startTime >= nowMillis - 14L * DAY }
        val frequency7 = last7.size
        val frequency14 = last14.size

        val weeklyVolume = completed
            .groupBy { weekStart(it.session.startTime) }
            .map { (_, weekSessions) -> weekSessions.sumOf { it.volume() } }
            .filter { it > 0.0 }
        val recentVolume = weeklyVolume.lastOrNull() ?: 0.0
        val previousVolume = weeklyVolume.dropLast(1).lastOrNull() ?: recentVolume
        val volumeRatio = if (previousVolume > 0) recentVolume / previousVolume else 1.0

        val highRpeSetsLast7 = last7.flatMap { it.exercises }.flatMap { it.sets }.count { (it.rpe ?: 0.0) >= 9.0 }
        val avgRpeLast7 = last7.flatMap { it.exercises }.flatMap { it.sets }.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()

        val bwTrend = bodyweightTrend(bodyweights)

        var score = 75
        val reasons = mutableListOf<String>()

        if (frequency7 >= 6) { score -= 20; reasons += "Very high training frequency this week" }
        else if (frequency7 in 4..5) { score -= 8; reasons += "Above-average frequency this week" }
        else if (frequency7 <= 1) { score += 8; reasons += "Low frequency this week" }

        if (frequency14 >= 10) { score -= 10; reasons += "Dense two-week workload" }

        if (volumeRatio > 1.35) { score -= 18; reasons += "Weekly volume jumped ${((volumeRatio - 1.0) * 100).toInt()}%" }
        else if (volumeRatio < 0.65) { score += 8; reasons += "Weekly volume dropped recently" }

        if (avgRpeLast7 != null && avgRpeLast7 >= 9.0) { score -= 15; reasons += "Average RPE is very high" }
        else if (avgRpeLast7 != null && avgRpeLast7 <= 7.5) { score += 8; reasons += "Average RPE is manageable" }

        if (highRpeSetsLast7 >= 4) { score -= 10; reasons += "Multiple high-RPE sets this week" }

        if (hoursSinceLastSession < 18) { score -= 15; reasons += "Last session was very recent" }
        else if (hoursSinceLastSession in 18.0..36.0) { score -= 5; reasons += "Last session was recent" }
        else if (hoursSinceLastSession > 72) { score += 8; reasons += "Long recovery window since last session" }

        if (bwTrend < -0.7) { score -= 10; reasons += "Bodyweight is trending down" }
        else if (bwTrend > 0.5) { score += 3; reasons += "Bodyweight is stable or trending up" }

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
        val completed = sessions.filter { it.session.endTime != null }
        if (completed.isEmpty()) return emptyList()

        data class MuscleSession(
            val startTime: Long,
            val sets: List<SetLog>
        )

        val muscleData = mutableMapOf<String, MutableList<MuscleSession>>()
        completed.forEach { session ->
            session.exercises.forEach { entry ->
                val primary = splitCsv(entry.exercise.primaryMuscles.ifBlank { entry.exercise.muscles }).filter { it.isNotBlank() }
                val secondary = splitCsv(entry.exercise.secondaryMuscles).filter { it.isNotBlank() }
                val muscleSession = MuscleSession(session.session.startTime, entry.sets)
                primary.forEach { muscle -> muscleData.getOrPut(muscle) { mutableListOf() } += muscleSession }
                secondary.forEach { muscle -> muscleData.getOrPut(muscle) { mutableListOf() } += muscleSession.copy(sets = entry.sets.map { it.copy() }) }
            }
        }

        return muscleData.map { (muscle, sessionsForMuscle) ->
            val sorted = sessionsForMuscle.sortedByDescending { it.startTime }
            val lastTrained = sorted.firstOrNull()?.startTime
            val last7 = sorted.filter { it.startTime >= nowMillis - 7L * DAY }
            val volumeLast7 = last7.sumOf { it.sets.hardSetsVolume() }
            val highRpeSets = last7.flatMap { it.sets }.count { (it.rpe ?: 0.0) >= 9.0 }
            val hoursSinceLastTrained = lastTrained?.let { (nowMillis - it) / (3_600_000.0) } ?: Double.MAX_VALUE

            val score = when {
                hoursSinceLastTrained > 72 -> 90.0
                hoursSinceLastTrained > 48 -> 80.0 - (volumeLast7 / 5000.0).coerceIn(0.0, 20.0)
                hoursSinceLastTrained > 24 -> 60.0 - (volumeLast7 / 3000.0).coerceIn(0.0, 30.0) - highRpeSets * 5.0
                else -> 40.0 - (volumeLast7 / 2000.0).coerceIn(0.0, 30.0) - highRpeSets * 8.0
            }.coerceIn(0.0, 100.0)

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
        exercises.flatMap { it.sets }.filter { it.setType != com.replog.data.model.SetType.WARMUP }.sumOf { it.weight * it.reps }

    private fun List<SetLog>.hardSetsVolume(): Double = filter { it.setType != com.replog.data.model.SetType.WARMUP }.sumOf { it.weight * it.reps }

    private fun bodyweightTrend(bodyweights: List<BodyweightLog>): Double {
        if (bodyweights.size < 4) return 0.0
        val recent = bodyweights.takeLast(2).map { it.weight }.average()
        val previous = bodyweights.takeLast(4).take(2).map { it.weight }.average()
        return recent - previous
    }

    private fun splitCsv(value: String): List<String> = value.split(",").map { it.trim() }.filter { it.isNotBlank() }

    private fun weekStart(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.firstDayOfWeek = java.util.Calendar.MONDAY
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private const val DAY = 24L * 60L * 60L * 1000L
}
