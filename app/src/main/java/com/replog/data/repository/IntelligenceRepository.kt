package com.replog.data.repository

import com.replog.domain.forecast.ForecastConfidence
import com.replog.domain.forecast.ProgressionForecaster
import com.replog.domain.genome.TrainingGenomeEngine
import com.replog.domain.intelligence.IntelligenceEngine
import com.replog.domain.intelligence.IntelligenceInputs
import com.replog.domain.intelligence.TodaysBriefing
import com.replog.domain.musclegap.MuscleGapAnalyzer
import com.replog.domain.recommendation.MuscleRecoveryStatus
import com.replog.domain.recommendation.RecoveryAnalyzer
import com.replog.domain.recovery.RecoveryDashboard
import com.replog.domain.volume.VolumeLandmarks
import com.replog.domain.volume.VolumeStatus
import com.replog.util.PreferencesManager
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gathers inputs from the existing engines (Recovery, Genome, Muscle Gap, Volume,
 * Forecast, Goal, History) using bounded/aggregate queries and composes them via
 * the pure IntelligenceEngine into one explainable TodaysBriefing.
 *
 * This is the only place data-gathering for the intelligence dashboard happens,
 * so no engine logic is duplicated and the heavy work runs once per request
 * (the ViewModel caches the result). Offline-only: every value comes from Room.
 */
@Singleton
class IntelligenceRepository @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val bodyweightRepository: BodyweightRepository,
    private val exerciseRepository: ExerciseRepository,
    private val trainingDNARepository: TrainingDNARepository,
    private val goalRepository: GoalRepository,
    private val recommendationRepository: RecommendationRepository,
    private val prefs: PreferencesManager
) {

    suspend fun buildBriefing(): TodaysBriefing {
        val now = System.currentTimeMillis()
        // Bounded history window (Priority 7) - recovery/genome/volume only need recent data.
        val sessions = workoutRepository.getRecentCompletedSessions(60).first()
        val useKg = prefs.useKg.first()

        if (sessions.size < 3) {
            return IntelligenceEngine.build(IntelligenceInputs(hasEnoughData = false, recoveryScore = null))
        }

        // --- Recovery (reuse analyzer + dashboard mapping) ---
        val bodyweights = bodyweightRepository.getAllBodyweights().first()
        val overall = runCatching { RecoveryAnalyzer.overallRecovery(sessions, bodyweights, now) }.getOrNull()
        val recoveryState = overall?.let { RecoveryDashboard.from(it) }
        val muscleRecovery = runCatching { RecoveryAnalyzer.muscleRecovery(sessions, now) }.getOrNull().orEmpty()
        val ready = muscleRecovery
            .filter { it.status == MuscleRecoveryStatus.FRESH || it.status == MuscleRecoveryStatus.RECOVERED }
            .sortedByDescending { it.recoveryScore }
            .map { it.muscle }
            .distinct().take(3)
        val fatigued = muscleRecovery
            .filter { it.status == MuscleRecoveryStatus.FATIGUED || it.status == MuscleRecoveryStatus.VERY_FATIGUED }
            .sortedBy { it.recoveryScore }
            .map { it.muscle }
            .distinct().take(3)
        val restRecommended = (recoveryState?.score ?: 100) < 45

        // --- Recommendation focus (reuse recommendation engine) ---
        val recommendedFocus = runCatching {
            recommendationRepository.generateRecommendation().title
        }.getOrNull()

        // --- Training Genome (best rep range) ---
        val genome = runCatching { TrainingGenomeEngine.analyze(sessions, now) }.getOrNull()
        val bestRepRange = genome?.takeIf { it.hasEnoughData }
            ?.traits?.firstOrNull { it.dimension.equals("Rep range", true) }?.bestValue

        // --- Weekly volume (under-target groups) ---
        val underVolume = runCatching {
            VolumeLandmarks.analyze(sessions, now, weeks = 1)
                .filter { it.status == VolumeStatus.UNDER }
                .map { it.muscleGroup }
                .take(3)
        }.getOrNull().orEmpty()

        // --- Muscle gap (neglected muscles from latest DNA snapshot) ---
        val weakMuscles = runCatching {
            trainingDNARepository.getLatestDNA().first()
                ?.weakestMuscles?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        }.getOrNull().orEmpty()
        val neglected = runCatching {
            val library = exerciseRepository.getAllExercises().first()
            MuscleGapAnalyzer.analyze(weakMuscles, library).map { it.muscle }
        }.getOrNull().orEmpty()

        // --- Top progression forecast (reuse forecaster on the best progression score) ---
        var topForecastLabel: String? = null
        var topForecastHigh = false
        runCatching {
            val scores = trainingDNARepository.getProgressionScores().first()
            val best = scores.maxByOrNull { it.estimatedOneRm30Day }
            if (best != null && best.estimatedOneRm30Day > 0) {
                val f = ProgressionForecaster.forecast(best)
                val name = exerciseRepository.getExerciseById(best.exerciseId)?.name ?: "Your main lift"
                topForecastLabel = "$name ${f.projectionLabel}"
                topForecastHigh = f.confidence == ForecastConfidence.HIGH
            }
        }

        // --- Goal summary (reuse goal engine) ---
        val goalSummary = runCatching {
            goalRepository.getActive().first().firstOrNull()?.let { goalRepository.forecastFor(it, useKg).summaryLine }
        }.getOrNull()

        // --- History-derived coach signals (Priority 6) ---
        val strongestDay = strongestDayOfWeek(sessions)
        val prsAfterRest = typicalRestDaysBeforePr(sessions)
        val slowAfterLegs = slowRecoveryAfterHighVolumeLegs(muscleRecovery)

        return IntelligenceEngine.build(
            IntelligenceInputs(
                hasEnoughData = true,
                recoveryScore = recoveryState?.score,
                recoveryStatusLabel = recoveryState?.statusLabel,
                recoveryDirective = recoveryState?.directive,
                recoveryFactors = recoveryState?.factors.orEmpty(),
                isRestRecommended = restRecommended,
                readyMuscleGroups = ready,
                fatiguedMuscleGroups = fatigued,
                recommendedFocus = recommendedFocus,
                genomeBestRepRange = bestRepRange,
                underVolumeGroups = underVolume,
                neglectedMuscles = neglected,
                topForecastLabel = topForecastLabel,
                topForecastConfidenceHigh = topForecastHigh,
                goalSummary = goalSummary,
                strongestDayOfWeek = strongestDay,
                prsAfterRestDays = prsAfterRest,
                slowRecoveryAfterHighVolume = slowAfterLegs,
                hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            )
        )
    }

    /**
     * Priority 5: compute the RepLog Score from component values derived by the
     * existing engines (recovery, volume, forecasts, goals). No raw-session
     * analysis here - it reuses bounded reads and feeds the pure score engine.
     */
    suspend fun buildRepLogScore(): com.replog.domain.intelligence.RepLogScoreResult? {
        val now = System.currentTimeMillis()
        val sessions = workoutRepository.getRecentCompletedSessions(60).first()
        if (sessions.size < 3) return null
        val bodyweights = bodyweightRepository.getAllBodyweights().first()

        // Recovery (0-100 already)
        val recovery = runCatching {
            RecoveryAnalyzer.overallRecovery(sessions, bodyweights, now).score.toInt()
        }.getOrNull() ?: 60

        // Volume quality + muscle balance from weekly landmarks.
        val landmarks = runCatching { VolumeLandmarks.analyze(sessions, now, weeks = 1) }.getOrNull().orEmpty()
        val trained = landmarks.filter { it.status != VolumeStatus.NONE }
        val volumeQuality = if (trained.isEmpty()) 50 else
            (trained.count { it.status == VolumeStatus.IN_RANGE } * 100 / trained.size)
        // Balance: penalise groups below optimal (neglected) among those trained.
        val muscleBalance = if (trained.isEmpty()) 50 else
            (100 - trained.count { it.status == VolumeStatus.UNDER } * 100 / trained.size).coerceIn(0, 100)

        // Progressive overload: share of tracked lifts trending up.
        val scores = runCatching { trainingDNARepository.getProgressionScores().first() }.getOrNull().orEmpty()
        val overload = if (scores.isEmpty()) 50 else {
            val rising = scores.count { it.estimatedOneRm30Day >= it.estimatedOneRm90Day && it.estimatedOneRm30Day > 0 }
            (rising * 100 / scores.size)
        }

        // Goal adherence: average progress across active goals.
        val useKg = prefs.useKg.first()
        val goals = runCatching { goalRepository.getActive().first() }.getOrNull().orEmpty()
        val goalAdherence = if (goals.isEmpty()) 60 else runCatching {
            goals.map { goalRepository.forecastFor(it, useKg).progressPercent }.average().toInt()
        }.getOrNull() ?: 60

        // Consistency: sessions in the last 4 weeks vs the user's typical frequency.
        val consistency = consistencyScore(sessions, now)
        // Recovery discipline: not training while very fatigued / honouring rest.
        val recoveryDiscipline = recoveryDisciplineScore(sessions, now)

        return RepLogScoreEngineCompute(
            consistency, recovery, overload, volumeQuality, goalAdherence, muscleBalance, recoveryDiscipline
        )
    }

    private fun RepLogScoreEngineCompute(
        consistency: Int, recovery: Int, overload: Int, volumeQuality: Int,
        goalAdherence: Int, muscleBalance: Int, recoveryDiscipline: Int
    ) = com.replog.domain.intelligence.RepLogScoreEngine.compute(
        com.replog.domain.intelligence.RepLogScoreInputs(
            consistency = consistency, recovery = recovery, progressiveOverload = overload,
            volumeQuality = volumeQuality, goalAdherence = goalAdherence,
            muscleBalance = muscleBalance, recoveryDiscipline = recoveryDiscipline
        )
    )

    /** Sessions/week over the last 4 weeks scaled to a 0-100 score (3+/wk = 100). */
    private fun consistencyScore(sessions: List<com.replog.data.model.SessionWithExercises>, now: Long): Int {
        val fourWeeksAgo = now - 28L * 86_400_000L
        val recent = sessions.count { it.session.startTime >= fourWeeksAgo }
        val perWeek = recent / 4.0
        return ((perWeek / 3.0) * 100).toInt().coerceIn(0, 100)
    }

    /** Penalise back-to-back sessions hitting the same muscles very hard (overreaching). */
    private fun recoveryDisciplineScore(sessions: List<com.replog.data.model.SessionWithExercises>, now: Long): Int {
        val sorted = sessions.sortedBy { it.session.startTime }
        if (sorted.size < 2) return 80
        var tooSoon = 0
        for (i in 1 until sorted.size) {
            val gapH = (sorted[i].session.startTime - sorted[i - 1].session.startTime) / 3_600_000.0
            val highRpe = sorted[i - 1].exercises.any { e -> e.sets.any { (it.rpe ?: 0.0) >= 9.0 } }
            if (gapH < 18 && highRpe) tooSoon++
        }
        val ratio = tooSoon.toDouble() / (sorted.size - 1)
        return (100 - (ratio * 100).toInt()).coerceIn(0, 100)
    }

    /** Most common day-of-week for completed sessions (>=4 sessions to be meaningful). */
    private fun strongestDayOfWeek(sessions: List<com.replog.data.model.SessionWithExercises>): String? {
        if (sessions.size < 4) return null
        val cal = Calendar.getInstance()
        val counts = IntArray(8)
        sessions.forEach {
            cal.timeInMillis = it.session.startTime
            counts[cal.get(Calendar.DAY_OF_WEEK)]++
        }
        val top = (1..7).maxByOrNull { counts[it] } ?: return null
        if (counts[top] < 2) return null
        return when (top) {
            Calendar.MONDAY -> "Monday"; Calendar.TUESDAY -> "Tuesday"; Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"; Calendar.FRIDAY -> "Friday"; Calendar.SATURDAY -> "Saturday"
            else -> "Sunday"
        }
    }

    /** Typical whole-day gap before a PB session, from sessions that contain a PR. */
    private fun typicalRestDaysBeforePr(sessions: List<com.replog.data.model.SessionWithExercises>): Int? {
        val sorted = sessions.sortedBy { it.session.startTime }
        val gaps = mutableListOf<Int>()
        for (idx in 1 until sorted.size) {
            val s = sorted[idx]
            val hasPr = s.exercises.any { e -> e.sets.any { it.isPR } }
            if (!hasPr) continue
            val gapDays = ((s.session.startTime - sorted[idx - 1].session.startTime) / 86_400_000L).toInt()
            if (gapDays in 1..7) gaps += gapDays
        }
        if (gaps.size < 2) return null
        return gaps.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }

    /** True when leg-related muscle groups read most fatigued (data-derived). */
    private fun slowRecoveryAfterHighVolumeLegs(
        muscleRecovery: List<com.replog.domain.recommendation.MuscleRecovery>
    ): Boolean {
        val legKeys = listOf("quad", "hamstring", "glute", "calf")
        val leg = muscleRecovery.filter { mr -> legKeys.any { mr.muscle.lowercase().contains(it) } }
        if (leg.isEmpty()) return false
        val legWorst = leg.minOf { it.recoveryScore }
        val others = muscleRecovery.filter { mr -> legKeys.none { mr.muscle.lowercase().contains(it) } }
        val othersAvg = others.map { it.recoveryScore }.average().takeIf { !it.isNaN() } ?: 100.0
        // Legs noticeably more fatigued than the rest, with meaningful recent volume.
        return legWorst < 50 && legWorst < othersAvg - 15 && leg.any { it.volumeLast7Days > 3000 }
    }
}
