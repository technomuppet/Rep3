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
import com.replog.domain.recovery.RecoveryCalendar
import com.replog.domain.recovery.RecoveryCalendarDay
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
/** One muscle's recovery state for the Recovery Centre (from RecoveryAnalyzer). */
data class MuscleRecoveryUi(
    val muscle: String,
    val score: Int,                 // 0-100
    val status: String,             // FRESH / RECOVERED / FATIGUED / VERY_FATIGUED
    val hoursUntilReady: Int        // estimated hours until ready (0 if ready now)
)

/** Recovery Centre screen data (Priority 1) - all from RecoveryAnalyzer/RecoveryDashboard/RecoveryCalendar. */
data class RecoveryCentreData(
    val hasData: Boolean,
    val score: Int = 0,
    val statusLabel: String = "",
    val directive: String = "",
    val directiveDetail: String = "",
    val factors: List<String> = emptyList(),
    val recovered: List<MuscleRecoveryUi> = emptyList(),
    val fatigued: List<MuscleRecoveryUi> = emptyList(),
    val calendar: List<RecoveryCalendarDay> = emptyList(),
    val todayScore: Int = 0,
    val tomorrowScore: Int = 0,         // projected
    val in48hScore: Int = 0,            // projected
    val estimatedFullRecoveryHours: Int = 0,
    val suggestedIntensity: String = "",
    val suggestedDurationMinutes: Int = 0,
    val suggestedType: String = "",
    val improvements: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/** One muscle group's volume/balance line (from VolumeLandmarks). */
data class MuscleBalanceRow(
    val muscleGroup: String,
    val weeklySets: Int,
    val optimalLow: Int,
    val optimalHigh: Int,
    val status: String,                 // UNDER / IN_RANGE / ABOVE / NONE
    val severity: Int,                  // 0-100, how far below optimal (gap severity)
    val recommendedExercises: List<String>
)

/** Muscle Balance Centre data (Priority 2) - from VolumeLandmarks + MuscleGapAnalyzer + DNA snapshot. */
data class MuscleBalanceData(
    val hasData: Boolean,
    val balanceScore: Int = 0,          // overall balance 0-100
    val weakest: List<String> = emptyList(),
    val strongest: List<String> = emptyList(),
    val rows: List<MuscleBalanceRow> = emptyList(),
    val estimatedWeeksToBalance: Int = 0
)

/** One DNA snapshot flattened for evolution timelines (from stored snapshots). */
data class DnaEvolutionPoint(
    val generatedAt: Long,
    val preferredRepRange: String,
    val workoutDurationMinutes: Int,
    val recoveryHours: Int,
    val frequency: String,
    val volumeTolerance: Int,
    val monthlyPrCount: Int
)

/** DNA Evolution data (Priority 3) - built ONLY from stored historical snapshots. */
data class DnaEvolutionData(
    val hasData: Boolean,
    val points: List<DnaEvolutionPoint> = emptyList(),
    val genomeMaturity: String = "",
    val consistencyTrend: String = ""
)

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

    // -------------------------------------------------------------------------
    // Priority 1: Recovery Centre (reuses RecoveryAnalyzer / Dashboard / Calendar)
    // -------------------------------------------------------------------------
    suspend fun buildRecoveryCentre(): RecoveryCentreData {
        val now = System.currentTimeMillis()
        val sessions = workoutRepository.getRecentCompletedSessions(60).first()
        if (sessions.size < 3) return RecoveryCentreData(hasData = false)
        val bodyweights = bodyweightRepository.getAllBodyweights().first()

        val overall = RecoveryAnalyzer.overallRecovery(sessions, bodyweights, now)
        val state = RecoveryDashboard.from(overall)
        val muscle = RecoveryAnalyzer.muscleRecovery(sessions, now)

        fun toUi(mr: com.replog.domain.recommendation.MuscleRecovery): MuscleRecoveryUi {
            // Reuse the analyzer's score; estimate hours-to-ready from its own score curve.
            val hours = when {
                mr.recoveryScore >= 80 -> 0
                mr.recoveryScore >= 60 -> 18
                mr.recoveryScore >= 40 -> 36
                else -> 54
            }
            return MuscleRecoveryUi(mr.muscle, mr.recoveryScore.toInt(), mr.status.name, hours)
        }
        val recovered = muscle.filter { it.status == MuscleRecoveryStatus.FRESH || it.status == MuscleRecoveryStatus.RECOVERED }
            .sortedByDescending { it.recoveryScore }.map { toUi(it) }
        val fatigued = muscle.filter { it.status == MuscleRecoveryStatus.FATIGUED || it.status == MuscleRecoveryStatus.VERY_FATIGUED }
            .sortedBy { it.recoveryScore }.map { toUi(it) }

        val calendar = RecoveryCalendar.build(
            sessions.map { c -> c.session.startTime to c.exercises.sumOf { e -> e.sets.sumOf { it.weight * it.reps } } },
            now, days = 14
        )

        // Today/tomorrow/48h projection from the analyzer score plus per-muscle readiness.
        val today = state.score
        val tomorrow = (today + (100 - today) / 3).coerceIn(0, 100)
        val in48 = (today + (100 - today) * 2 / 3).coerceIn(0, 100)
        val fullHours = fatigued.maxOfOrNull { it.hoursUntilReady } ?: 0

        val intensity = when {
            today >= 80 -> "Heavy"
            today >= 60 -> "Moderate"
            today >= 45 -> "Light"
            else -> "Rest / mobility"
        }
        val duration = when {
            today >= 80 -> 60
            today >= 60 -> 45
            today >= 45 -> 30
            else -> 0
        }
        val type = when {
            recovered.isNotEmpty() -> "${recovered.first().muscle} focus"
            today < 45 -> "Recovery day"
            else -> "Balanced session"
        }
        val improvements = mutableListOf<String>()
        if (recovered.isNotEmpty()) improvements += "${recovered.size} muscle group${if (recovered.size == 1) "" else "s"} fully recovered."
        if (today >= 80) improvements += "Overall recovery is excellent today."
        val warnings = mutableListOf<String>()
        if (today < 45) warnings += "Recovery is low - training hard today may set you back."
        fatigued.firstOrNull()?.let { warnings += "${it.muscle} is still fatigued (${it.hoursUntilReady}h to go)." }

        return RecoveryCentreData(
            hasData = true, score = state.score, statusLabel = state.statusLabel,
            directive = state.directive, directiveDetail = state.directiveDetail, factors = state.factors,
            recovered = recovered, fatigued = fatigued, calendar = calendar,
            todayScore = today, tomorrowScore = tomorrow, in48hScore = in48,
            estimatedFullRecoveryHours = fullHours, suggestedIntensity = intensity,
            suggestedDurationMinutes = duration, suggestedType = type,
            improvements = improvements, warnings = warnings
        )
    }

    // -------------------------------------------------------------------------
    // Priority 2: Muscle Balance Centre (reuses VolumeLandmarks + MuscleGapAnalyzer)
    // -------------------------------------------------------------------------
    suspend fun buildMuscleBalance(): MuscleBalanceData {
        val now = System.currentTimeMillis()
        val sessions = workoutRepository.getRecentCompletedSessions(60).first()
        if (sessions.size < 3) return MuscleBalanceData(hasData = false)
        val library = exerciseRepository.getAllExercises().first()
        val landmarks = VolumeLandmarks.analyze(sessions, now, weeks = 1)

        val rows = landmarks.map { lm ->
            val severity = when (lm.status) {
                VolumeStatus.UNDER -> (((lm.optimalLow - lm.weeklySets) / lm.optimalLow.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
                VolumeStatus.NONE -> 100
                else -> 0
            }
            val recs = if (lm.status == VolumeStatus.UNDER || lm.status == VolumeStatus.NONE)
                MuscleGapAnalyzer.suggestionsFor(lm.muscleGroup, library, limit = 3).map { it.name }
            else emptyList()
            MuscleBalanceRow(
                muscleGroup = lm.muscleGroup, weeklySets = lm.weeklySets.toInt(),
                optimalLow = lm.optimalLow, optimalHigh = lm.optimalHigh, status = lm.status.name,
                severity = severity, recommendedExercises = recs
            )
        }
        val trained = landmarks.filter { it.status != VolumeStatus.NONE }
        val balanceScore = if (trained.isEmpty()) 0
        else (100 - trained.count { it.status == VolumeStatus.UNDER } * 100 / trained.size).coerceIn(0, 100)

        // Strongest/weakest from the latest DNA snapshot (already computed there).
        val dna = trainingDNARepository.getLatestDNA().first()
        val weakest = dna?.weakestMuscles?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: rows.filter { it.status == "UNDER" || it.status == "NONE" }.map { it.muscleGroup }
        val strongest = dna?.strongestMuscles?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        // Rough estimate: ~1 week per under-volume group to add the missing sets.
        val weeksToBalance = rows.count { it.status == "UNDER" || it.status == "NONE" }.coerceAtMost(8)

        return MuscleBalanceData(
            hasData = true, balanceScore = balanceScore, weakest = weakest.take(5),
            strongest = strongest.take(5), rows = rows, estimatedWeeksToBalance = weeksToBalance
        )
    }

    /** Resolve the recommended exercises for a muscle into a startable Quick Workout-style session via WorkoutStarter is handled in the ViewModel; here we expose the library lookup. */
    suspend fun startMuscleGapWorkout(muscle: String): Boolean {
        val library = exerciseRepository.getAllExercises().first()
        val exercises = MuscleGapAnalyzer.suggestionsFor(muscle, library, limit = 4)
        if (exercises.isEmpty()) return false
        val sessionId = workoutRepository.insertSession(
            com.replog.data.model.WorkoutSession(templateName = "$muscle focus", startTime = System.currentTimeMillis())
        ).toInt()
        exercises.forEachIndexed { index, ex ->
            workoutRepository.insertSessionExercise(
                com.replog.data.model.SessionExercise(sessionId = sessionId, exerciseId = ex.id, orderIndex = index, notes = "")
            )
        }
        prefs.setActiveSessionId(sessionId)
        return true
    }

    suspend fun addMuscleGapToTemplate(muscle: String): String? {
        val library = exerciseRepository.getAllExercises().first()
        val exercises = MuscleGapAnalyzer.suggestionsFor(muscle, library, limit = 4)
        if (exercises.isEmpty()) return null
        val name = "$muscle focus"
        val templateId = workoutRepository.insertTemplate(
            com.replog.data.model.WorkoutTemplate(name = name, isBuiltIn = false)
        ).toInt()
        exercises.forEachIndexed { index, ex ->
            workoutRepository.insertTemplateExercise(
                com.replog.data.model.TemplateExercise(templateId = templateId, exerciseId = ex.id, orderIndex = index, defaultSets = 3, targetReps = 10)
            )
        }
        return name
    }

    // -------------------------------------------------------------------------
    // Priority 3: DNA Evolution (built ONLY from stored historical snapshots)
    // -------------------------------------------------------------------------
    suspend fun buildDnaEvolution(): DnaEvolutionData {
        val snapshots = trainingDNARepository.getHistoricalDNA().first().sortedBy { it.generatedAt }
        if (snapshots.isEmpty()) return DnaEvolutionData(hasData = false)
        val points = snapshots.map { s ->
            DnaEvolutionPoint(
                generatedAt = s.generatedAt,
                preferredRepRange = s.preferredRepRange,
                workoutDurationMinutes = s.averageWorkoutDuration.toInt(),
                recoveryHours = s.averageRecoveryHours.toInt(),
                frequency = s.preferredFrequency,
                volumeTolerance = s.volumeToleranceScore.toInt(),
                monthlyPrCount = s.monthlyPRCount
            )
        }
        val maturity = when {
            snapshots.size >= 6 -> "Mature"
            snapshots.size >= 3 -> "Developing"
            else -> "Emerging"
        }
        val consistencyTrend = if (points.size >= 2) {
            val first = points.first().workoutDurationMinutes
            val last = points.last().workoutDurationMinutes
            if (last >= first) "Stable or improving" else "Variable"
        } else "Not enough history"
        return DnaEvolutionData(hasData = true, points = points, genomeMaturity = maturity, consistencyTrend = consistencyTrend)
    }

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

    /**
     * Phase 2 Gap 4 — dedicated muscle-gap card on Home.
     *
     * Reuses the weak-muscle list from the latest DNA snapshot (the same
     * `weakestMuscles` field `buildBriefing()` already splits + passes to
     * `MuscleGapAnalyzer`) and the full exercise library used by
     * `buildMuscleBalance()` and `startMuscleGapWorkout()`. Returns an
     * empty list when there is no DNA snapshot yet — mirrors the
     * briefing's `hasEnoughData` guard so the card stays hidden until
     * training history has produced actionable guidance.
     */
    suspend fun buildMuscleGapSuggestions(
        perMuscle: Int = 3
    ): List<com.replog.domain.musclegap.MuscleGapSuggestion> {
        val dna = trainingDNARepository.getLatestDNA().first()
        val weak = dna?.weakestMuscles?.split(",")?.map { it.trim() }
            ?.filter { it.isNotBlank() }.orEmpty()
        if (weak.isEmpty()) return emptyList()
        val library = exerciseRepository.getAllExercises().first()
        return MuscleGapAnalyzer.analyze(weak, library, perMuscle)
    }

    /**
     * Phase 2 Gap 5 — dedicated weekly-volume card on Home.
     *
     * Reuses the same `VolumeLandmarks.analyze(weeks = 1)` pass that
     * `buildBriefing()` already runs to populate the "Weekly Volume"
     * explainSection. Surfacing it as its own StateFlow lets Home draw
     * ten muscle-group rows with status colours rather than burying the
     * signal in the briefing's narrative — same engine, fresh UI wiring.
     *
     * Returns an empty list when there are too few completed sessions;
     * the Home card stays hidden until the user has real training
     * history, exactly matching the briefing's `hasEnoughData` guard.
     */
    suspend fun buildWeeklyLandmarks(): List<com.replog.domain.volume.VolumeLandmark> {
        val now = System.currentTimeMillis()
        val sessions = workoutRepository.getRecentCompletedSessions(60).first()
        if (sessions.size < 3) return emptyList()
        return VolumeLandmarks.analyze(sessions, now, weeks = 1)
    }
}
