package com.replog.domain.trainingdna

import com.replog.data.model.SessionWithExercises
import com.replog.data.model.TrainingDnaSnapshot
import com.replog.util.PRCalculator
import java.util.Calendar

class TrainingDnaEngine(
    private val config: TrainingDnaConfig = TrainingDnaConfig()
) {

    fun generate(sessions: List<SessionWithExercises>): TrainingDnaResult {
        val completed = sessions.filter { it.session.endTime != null }
        val now = config.nowMillis

        val progressionScores = calculateProgressionScores(completed)
        val plateauDetections = PlateauDetector.detect(completed, config)

        val snapshot = if (completed.size >= config.minSessionsForDna && totalHardSets(completed) >= config.minSetsForDna) {
            buildSnapshot(completed, progressionScores, plateauDetections, now)
        } else {
            buildEmptySnapshot(now)
        }

        return TrainingDnaResult(
            snapshot = snapshot,
            progressionScores = progressionScores.map { it.toEntity(now) },
            plateauEvents = plateauDetections.map { it.toEntity(now) }
        )
    }

    private fun buildSnapshot(
        sessions: List<SessionWithExercises>,
        progressionScores: List<ExerciseProgression>,
        plateauDetections: List<PlateauDetection>,
        now: Long
    ): TrainingDnaSnapshot {
        val allEntries = sessions.flatMap { it.exercises }
        val hardSets = allEntries.flatMap { it.sets }.filter { it.setType != com.replog.data.model.SetType.WARMUP }

        val preferredRepRange = preferredRepRange(hardSets)
        val preferredVolumeRange = preferredVolumeRange(sessions)
        val preferredFrequency = preferredFrequency(sessions)
        val muscleStrengths = muscleStrengths(allEntries)
        val strongest = muscleStrengths.take(3).map { it.muscle }
        val weakest = muscleStrengths.filter { it.muscle !in strongest }.takeLast(3).map { it.muscle }
        val fastest = progressionScores
            .sortedByDescending { it.score30Day }
            .take(3)
            .map { it.exerciseName }
        val stalled = plateauDetections.map { it.exerciseName }
        val avgDuration = averageWorkoutDuration(sessions)
        val avgRecovery = averageRecoveryHours(sessions)
        val monthlyPrs = monthlyPRCount(sessions, now)
        val volumeTolerance = calculateVolumeToleranceScore(sessions)

        return TrainingDnaSnapshot(
            generatedAt = now,
            preferredRepRange = preferredRepRange,
            preferredVolumeRange = preferredVolumeRange,
            preferredFrequency = preferredFrequency,
            strongestMuscles = strongest.joinToString(", "),
            weakestMuscles = weakest.joinToString(", "),
            fastestProgressingExercises = fastest.joinToString(", "),
            stalledExercises = stalled.joinToString(", "),
            averageWorkoutDuration = avgDuration,
            averageRecoveryHours = avgRecovery,
            monthlyPRCount = monthlyPrs,
            volumeToleranceScore = volumeTolerance
        )
    }

    private fun buildEmptySnapshot(now: Long): TrainingDnaSnapshot = TrainingDnaSnapshot(
        generatedAt = now,
        preferredRepRange = "Not enough data",
        preferredVolumeRange = "Not enough data",
        preferredFrequency = "Not enough data",
        strongestMuscles = "",
        weakestMuscles = "",
        fastestProgressingExercises = "",
        stalledExercises = "",
        averageWorkoutDuration = 0.0,
        averageRecoveryHours = 0.0,
        monthlyPRCount = 0,
        volumeToleranceScore = 0.0
    )

    private fun calculateProgressionScores(sessions: List<SessionWithExercises>): List<ExerciseProgression> =
        sessions
            .flatMap { it.exercises }
            .groupBy { it.exercise.id }
            .map { (exerciseId, entries) ->
                ProgressionScorer.calculateExerciseProgression(
                    sessions,
                    exerciseId,
                    entries.first().exercise.name,
                    config.nowMillis
                )
            }

    private fun preferredRepRange(sets: List<com.replog.data.model.SetLog>): String {
        if (sets.isEmpty()) return "Not enough data"
        val bucket = sets.groupBy { repBucket(it.reps) }.maxByOrNull { it.value.size }?.key ?: "Unknown"
        return bucket
    }

    private fun preferredVolumeRange(sessions: List<SessionWithExercises>): String {
        if (sessions.isEmpty()) return "Not enough data"
        val volumes = sessions.map { session ->
            session.exercises.flatMap { it.sets }
                .filter { it.setType != com.replog.data.model.SetType.WARMUP }
                .sumOf { it.weight * it.reps }
        }
        val avg = volumes.average()
        return when {
            avg < 1500 -> "Low (< 1.5k/session)"
            avg < 4000 -> "Moderate (1.5–4k/session)"
            avg < 8000 -> "High (4–8k/session)"
            else -> "Very high (8k+/session)"
        }
    }

    private fun preferredFrequency(sessions: List<SessionWithExercises>): String {
        if (sessions.size < 2) return "Not enough data"
        val days = ((sessions.maxOf { it.session.startTime } - sessions.minOf { it.session.startTime }) / DAY).coerceAtLeast(1)
        val weekly = sessions.size.toDouble() / days * 7.0
        return when {
            weekly < 2 -> "Low (~1x/week)"
            weekly < 3 -> "Moderate (~2x/week)"
            weekly < 4.5 -> "High (~3x/week)"
            else -> "Very high (4x+/week)"
        }
    }

    private fun muscleStrengths(entries: List<com.replog.data.model.SessionExerciseWithSets>): List<MuscleStrength> {
        val muscleVolumes = mutableMapOf<String, Double>()
        val muscleOneRms = mutableMapOf<String, MutableList<Double>>()

        entries.forEach { entry ->
            val hardSets = entry.sets.filter { it.setType != com.replog.data.model.SetType.WARMUP }
            val volume = hardSets.sumOf { it.weight * it.reps }
            val primary = splitCsv(entry.exercise.primaryMuscles.ifBlank { entry.exercise.muscles }).filter { it.isNotBlank() }
            val secondary = splitCsv(entry.exercise.secondaryMuscles).filter { it.isNotBlank() }

            primary.forEach { muscle ->
                muscleVolumes[muscle] = (muscleVolumes[muscle] ?: 0.0) + volume
            }
            secondary.forEach { muscle ->
                muscleVolumes[muscle] = (muscleVolumes[muscle] ?: 0.0) + volume * 0.5
            }

            hardSets.forEach { set ->
                val e1rm = PRCalculator.epley1RM(set.weight, set.reps)
                primary.forEach { muscle ->
                    muscleOneRms.getOrPut(muscle) { mutableListOf() } += e1rm
                }
            }
        }

        val totalVolume = muscleVolumes.values.sum().takeIf { it > 0.0 } ?: 1.0
        return muscleVolumes.map { (muscle, volume) ->
            val avgOneRm = muscleOneRms[muscle]?.average() ?: 0.0
            MuscleStrength(
                muscle = muscle,
                score = volume / totalVolume + avgOneRm * 0.001,
                totalVolume = volume,
                estimatedOneRm = avgOneRm
            )
        }.sortedByDescending { it.score }
    }

    private fun averageWorkoutDuration(sessions: List<SessionWithExercises>): Double =
        sessions.mapNotNull { session ->
            session.session.endTime?.let { endTime ->
                ((endTime - session.session.startTime) / 60_000.0).coerceAtLeast(1.0)
            }
        }.takeIf { it.isNotEmpty() }?.average() ?: 0.0

    private fun averageRecoveryHours(sessions: List<SessionWithExercises>): Double {
        if (sessions.size < 2) return 0.0
        val sorted = sessions.map { it.session.startTime }.sorted()
        val gaps = sorted.zipWithNext { a, b -> (b - a) / (3_600_000.0) }.filter { it > 0 }
        return gaps.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun monthlyPRCount(sessions: List<SessionWithExercises>, now: Long): Int {
        val cutoff = now - 30L * DAY
        return sessions
            .filter { it.session.startTime >= cutoff }
            .flatMap { it.exercises }
            .flatMap { it.sets }
            .count { it.isPR }
    }

    private fun calculateVolumeToleranceScore(sessions: List<SessionWithExercises>): Double {
        if (sessions.size < 4) return 0.0
        val weeklyVolume = sessions
            .groupBy { weekStart(it.session.startTime) }
            .map { (_, weekSessions) ->
                weekSessions.flatMap { it.exercises }
                    .flatMap { it.sets }
                    .filter { it.setType != com.replog.data.model.SetType.WARMUP }
                    .sumOf { it.weight * it.reps }
            }
            .filter { it > 0.0 }
        if (weeklyVolume.size < 2) return 0.0
        val avg = weeklyVolume.average()
        val variance = weeklyVolume.map { (it - avg) * (it - avg) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val coefficientOfVariation = if (avg > 0) stdDev / avg else 0.0
        val frequencyScore = (weeklyVolume.size.coerceAtMost(8) / 8.0) * 40.0
        val consistencyScore = (1.0 - coefficientOfVariation.coerceIn(0.0, 1.0)) * 60.0
        return (frequencyScore + consistencyScore).coerceIn(0.0, 100.0)
    }

    private fun totalHardSets(sessions: List<SessionWithExercises>): Int =
        sessions.flatMap { it.exercises }.flatMap { it.sets }.count { it.setType != com.replog.data.model.SetType.WARMUP }

    private fun repBucket(reps: Int): String = when (reps) {
        in 1..3 -> "1–3"
        in 4..6 -> "4–6"
        in 7..10 -> "7–10"
        in 11..15 -> "11–15"
        else -> "16+"
    }

    private fun splitCsv(value: String): List<String> = value.split(",").map { it.trim() }.filter { it.isNotBlank() }

    private fun weekStart(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun ExerciseProgression.toEntity(now: Long): com.replog.data.model.TrainingDnaProgressionScore =
        com.replog.data.model.TrainingDnaProgressionScore(
            exerciseId = exerciseId,
            calculatedAt = now,
            score30Day = score30Day,
            score90Day = score90Day,
            scoreLifetime = scoreLifetime,
            estimatedOneRm30Day = estimatedOneRm30Day,
            estimatedOneRm90Day = estimatedOneRm90Day,
            estimatedOneRmLifetime = estimatedOneRmLifetime
        )

    private fun PlateauDetection.toEntity(now: Long): com.replog.data.model.PlateauEvent =
        com.replog.data.model.PlateauEvent(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            detectedAt = now,
            periodDays = periodDays,
            reason = reasons.joinToString(", ") { it.displayName() },
            lastLoad = lastLoad,
            lastReps = lastReps,
            lastVolume = lastVolume
        )

    private companion object {
        const val DAY = 24L * 60L * 60L * 1000L
    }
}
