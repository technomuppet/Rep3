package com.replog.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.BodyweightLog
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.repository.BodyweightRepository
import com.replog.data.repository.WorkoutRepository
import com.replog.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ExerciseRanking(
    val exerciseName: String,
    val bestWeight: Double,
    val bestEstimatedOneRm: Double,
    val totalVolume: Double,
    val totalSets: Int,
    val prCount: Int,
    val strengthToBodyweight: Double? = null
)

data class WeeklyVolumePoint(
    val weekStartMillis: Long,
    val volume: Double
)

data class Milestone(
    val title: String,
    val description: String,
    val achieved: Boolean,
    val progress: Double
)

data class BalancePoint(
    val label: String,
    val volume: Double,
    val percentage: Double
)

data class PlateauAlert(
    val exerciseName: String,
    val weeksStalled: Int,
    val bestEstimatedOneRm: Double,
    val suggestion: String
)

data class RecoveryInsight(
    val label: String = "Normal",
    val score: Int = 70,
    val explanation: String = "Log more sessions to build a reliable recovery signal."
)

data class StrengthForecast(
    val exerciseName: String,
    val currentEstimatedOneRm: Double,
    val projectedEstimatedOneRm: Double,
    val lowerBoundEstimatedOneRm: Double,
    val upperBoundEstimatedOneRm: Double,
    val targetEstimatedOneRm: Double,
    val targetDateMillis: Long?,
    val weeks: Int,
    val confidence: String,
    val trendPerWeek: Double
)

data class TrainingDnaSummary(
    val preferredRepRange: String = "Not enough data yet",
    val averageRpe: Double? = null,
    val hardSetsPerWeek: Double = 0.0,
    val topMovementPattern: String = "Unknown",
    val topExercise: String = "Unknown",
    val profileNote: String = "Log more completed workouts to build your Training DNA."
)

data class ActionRecommendation(
    val title: String,
    val message: String,
    val priority: String = "Medium"
)

data class ProgressUiState(
    val totalWorkouts: Int = 0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val totalVolume: Double = 0.0,
    val totalPRs: Int = 0,
    val currentStreakDays: Int = 0,
    val milestones: List<Milestone> = emptyList(),
    val rankings: List<ExerciseRanking> = emptyList(),
    val weeklyVolume: List<WeeklyVolumePoint> = emptyList(),
    val bodyweights: List<BodyweightLog> = emptyList(),
    val latestBodyweight: BodyweightLog? = null,
    val bodyweightGoal: Double? = null,
    val movementBalance: List<BalancePoint> = emptyList(),
    val muscleBalance: List<BalancePoint> = emptyList(),
    val recoveryInsight: RecoveryInsight = RecoveryInsight(),
    val plateauAlerts: List<PlateauAlert> = emptyList(),
    val forecasts: List<StrengthForecast> = emptyList(),
    val trainingDna: TrainingDnaSummary = TrainingDnaSummary(),
    val nextBestActions: List<ActionRecommendation> = emptyList(),
    val useKg: Boolean = true,
    val isLoading: Boolean = true
)

private data class DatedSet(
    val time: Long,
    val exerciseName: String,
    val set: SetLog,
    val estimatedOneRm: Double
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    private val bodyweightRepository: BodyweightRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val uiState: StateFlow<ProgressUiState> = combine(
        workoutRepository.getAllSessions(),
        bodyweightRepository.getAllBodyweights(),
        preferencesManager.useKg,
        preferencesManager.bodyweightGoal
    ) { sessions, bodyweights, useKg, goal ->
        buildProgressState(sessions.filter { it.session.endTime != null }, bodyweights, useKg, goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun addBodyweight(weight: Double, note: String? = null) = viewModelScope.launch {
        if (weight > 0) bodyweightRepository.insertBodyweight(BodyweightLog(weight = weight, note = note?.takeIf { it.isNotBlank() }))
    }

    fun deleteBodyweight(id: Int) = viewModelScope.launch { bodyweightRepository.deleteBodyweightById(id) }

    fun setBodyweightGoal(goal: Double?) = viewModelScope.launch { preferencesManager.setBodyweightGoal(goal) }

    private fun buildProgressState(
        sessions: List<SessionWithExercises>,
        bodyweights: List<BodyweightLog>,
        useKg: Boolean,
        goal: Double?
    ): ProgressUiState {
        val allEntries = sessions.flatMap { session -> session.exercises }
        val allSets = allEntries.flatMap { it.sets }
        val totalVolume = allSets.sumOf { it.weight * it.reps }
        val totalSets = allSets.size
        val totalReps = allSets.sumOf { it.reps }
        val totalPRs = allSets.count { it.isPR }
        val latestBodyweight = bodyweights.lastOrNull()
        val bw = latestBodyweight?.weight?.takeIf { it > 0 }
        val streak = currentStreakDays(sessions)
        val weeklyVolume = buildWeeklyVolume(sessions)
        val recovery = buildRecoveryInsight(sessions, bodyweights, weeklyVolume)
        val plateaus = buildPlateauAlerts(sessions)
        val movementBalance = buildMovementBalance(allEntries)
        val muscleBalance = buildMuscleBalance(allEntries)
        val forecasts = buildStrengthForecasts(sessions)
        val trainingDna = buildTrainingDna(sessions, allEntries)

        val rankings = allEntries
            .groupBy { it.exercise.id }
            .map { (_, entries) ->
                val exercise = entries.first().exercise
                val sets = entries.flatMap { it.sets }
                val bestEstimated = sets.maxOfOrNull { estimatedOneRm(it.weight, it.reps) } ?: 0.0
                ExerciseRanking(
                    exerciseName = exercise.name,
                    bestWeight = sets.maxOfOrNull { it.weight } ?: 0.0,
                    bestEstimatedOneRm = bestEstimated,
                    totalVolume = sets.sumOf { it.weight * it.reps },
                    totalSets = sets.size,
                    prCount = sets.count { it.isPR },
                    strengthToBodyweight = bw?.let { if (it > 0) bestEstimated / it else null }
                )
            }
            .sortedWith(compareByDescending<ExerciseRanking> { it.bestEstimatedOneRm }.thenByDescending { it.totalVolume })

        return ProgressUiState(
            totalWorkouts = sessions.size,
            totalSets = totalSets,
            totalReps = totalReps,
            totalVolume = totalVolume,
            totalPRs = totalPRs,
            currentStreakDays = streak,
            milestones = buildMilestones(sessions.size, totalSets, totalVolume, totalPRs, streak),
            rankings = rankings,
            weeklyVolume = weeklyVolume,
            bodyweights = bodyweights,
            latestBodyweight = latestBodyweight,
            bodyweightGoal = goal,
            movementBalance = movementBalance,
            muscleBalance = muscleBalance,
            recoveryInsight = recovery,
            plateauAlerts = plateaus,
            forecasts = forecasts,
            trainingDna = trainingDna,
            nextBestActions = buildNextBestActions(recovery, plateaus, movementBalance, muscleBalance, forecasts, trainingDna),
            useKg = useKg,
            isLoading = false
        )
    }

    private fun buildWeeklyVolume(sessions: List<SessionWithExercises>): List<WeeklyVolumePoint> = sessions
        .groupBy { weekStart(it.session.startTime) }
        .map { (weekStart, weekSessions) ->
            WeeklyVolumePoint(
                weekStartMillis = weekStart,
                volume = weekSessions.sumOf { session -> session.exercises.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } } }
            )
        }
        .sortedBy { it.weekStartMillis }
        .takeLast(12)

    private fun buildMovementBalance(entries: List<SessionExerciseWithSets>): List<BalancePoint> {
        val volumes = entries.groupBy { movementBucket(it.exercise.movementPattern.ifBlank { it.exercise.category }) }
            .mapValues { (_, entriesForBucket) -> entriesForBucket.sumOf { entry -> entry.sets.hardSetsVolume() } }
            .filterValues { it > 0.0 }
        val total = volumes.values.sum().takeIf { it > 0.0 } ?: return emptyList()
        return volumes.map { (label, volume) -> BalancePoint(label, volume, volume / total) }.sortedByDescending { it.volume }
    }

    private fun buildMuscleBalance(entries: List<SessionExerciseWithSets>): List<BalancePoint> {
        val muscleVolumes = mutableMapOf<String, Double>()
        entries.forEach { entry ->
            val volume = entry.sets.hardSetsVolume()
            val primary = splitCsv(entry.exercise.primaryMuscles.ifBlank { entry.exercise.muscles }).takeIf { it.isNotEmpty() }
            val secondary = splitCsv(entry.exercise.secondaryMuscles)
            primary?.forEach { muscle -> muscleVolumes[muscle] = (muscleVolumes[muscle] ?: 0.0) + volume }
            secondary.forEach { muscle -> muscleVolumes[muscle] = (muscleVolumes[muscle] ?: 0.0) + volume * 0.5 }
        }
        val total = muscleVolumes.values.sum().takeIf { it > 0.0 } ?: return emptyList()
        return muscleVolumes.map { (label, volume) -> BalancePoint(label, volume, volume / total) }.sortedByDescending { it.volume }.take(10)
    }

    private fun buildRecoveryInsight(
        sessions: List<SessionWithExercises>,
        bodyweights: List<BodyweightLog>,
        weeklyVolume: List<WeeklyVolumePoint>
    ): RecoveryInsight {
        if (sessions.size < 3) return RecoveryInsight()
        val now = System.currentTimeMillis()
        val last7 = sessions.count { it.session.startTime >= now - 7L * DAY }
        val last14 = sessions.count { it.session.startTime >= now - 14L * DAY }
        val recentVolume = weeklyVolume.lastOrNull()?.volume ?: 0.0
        val previousVolume = weeklyVolume.dropLast(1).lastOrNull()?.volume ?: recentVolume
        val volumeRatio = if (previousVolume > 0) recentVolume / previousVolume else 1.0
        val bwTrend = if (bodyweights.size >= 4) bodyweights.takeLast(2).map { it.weight }.average() - bodyweights.takeLast(4).take(2).map { it.weight }.average() else 0.0

        var score = 75
        val reasons = mutableListOf<String>()
        if (last7 >= 6) { score -= 25; reasons += "very high training frequency" }
        if (last14 >= 10) { score -= 10; reasons += "dense two-week workload" }
        if (volumeRatio > 1.35) { score -= 20; reasons += "weekly volume jumped ${((volumeRatio - 1.0) * 100).toInt()}%" }
        if (volumeRatio < 0.65) { score += 8; reasons += "volume has dropped recently" }
        if (bwTrend < -0.7) { score -= 10; reasons += "bodyweight is trending down" }
        if (last7 in 2..4 && volumeRatio in 0.8..1.2) { score += 10; reasons += "frequency and volume look stable" }
        score = score.coerceIn(0, 100)
        val label = when {
            score >= 80 -> "Recovered"
            score >= 55 -> "Normal"
            else -> "Fatigued"
        }
        return RecoveryInsight(label, score, reasons.takeIf { it.isNotEmpty() }?.joinToString(" • ") ?: "Training load looks steady.")
    }

    private fun buildPlateauAlerts(sessions: List<SessionWithExercises>): List<PlateauAlert> {
        if (sessions.size < 8) return emptyList()
        val sixWeeksAgo = System.currentTimeMillis() - 42L * DAY
        return sessions.flatMap { session -> session.exercises.map { entry -> session.session.startTime to entry } }
            .groupBy { it.second.exercise.id }
            .mapNotNull { (_, datedEntries) ->
                val exerciseName = datedEntries.first().second.exercise.name
                val allBest = datedEntries.flatMap { (_, entry) -> entry.sets.map { estimatedOneRm(it.weight, it.reps) } }.maxOrNull() ?: return@mapNotNull null
                val recentEntries = datedEntries.filter { it.first >= sixWeeksAgo }
                val recentBest = recentEntries.flatMap { (_, entry) -> entry.sets.map { estimatedOneRm(it.weight, it.reps) } }.maxOrNull() ?: return@mapNotNull null
                val recentSessions = recentEntries.map { it.first }.distinct().size
                if (recentSessions >= 4 && recentBest < allBest * 0.995) {
                    val hardSets = recentEntries.sumOf { it.second.sets.count { set -> set.setType != "Warmup" } }
                    val avgRpe = recentEntries.flatMap { it.second.sets.mapNotNull { set -> set.rpe } }.takeIf { it.isNotEmpty() }?.average()
                    val suggestion = when {
                        hardSets < 8 -> "Frequency or hard sets look low. Consider adding 2–4 quality sets weekly."
                        avgRpe != null && avgRpe > 9.0 -> "RPE is very high. Consider a lower-fatigue week or fewer failure sets."
                        hardSets > 24 -> "Volume is high. Consider reducing junk volume and improving recovery."
                        else -> "Review volume, frequency and proximity to failure for this lift."
                    }
                    PlateauAlert(exerciseName, 6, allBest, suggestion)
                } else null
            }
            .sortedByDescending { it.bestEstimatedOneRm }
            .take(5)
    }

    private fun buildStrengthForecasts(sessions: List<SessionWithExercises>): List<StrengthForecast> {
        val now = System.currentTimeMillis()
        return sessions.flatMap { session ->
            session.exercises.map { entry ->
                entry.exercise.id to DatedSet(
                    time = session.session.startTime,
                    exerciseName = entry.exercise.name,
                    set = entry.sets.maxByOrNull { estimatedOneRm(it.weight, it.reps) } ?: SetLog(sessionExerciseId = entry.sessionExercise.id, setNumber = 0, weight = 0.0, reps = 0, isPR=false, prType=null, completed=true),
                    estimatedOneRm = entry.sets.maxOfOrNull { estimatedOneRm(it.weight, it.reps) } ?: 0.0
                )
            }
        }
            .groupBy({ it.first }, { it.second })
            .mapNotNull { (_, datedSets) ->
                val useful = datedSets.filter { it.estimatedOneRm > 0.0 }.sortedBy { it.time }
                if (useful.size < 4) return@mapNotNull null
                val first = useful.first()
                val last = useful.last()
                val weeks = ((last.time - first.time).toDouble() / (7.0 * DAY)).coerceAtLeast(1.0)
                val trend = (last.estimatedOneRm - first.estimatedOneRm) / weeks
                if (kotlin.math.abs(trend) < 0.05) return@mapNotNull null
                val projected = (last.estimatedOneRm + trend * 5.0).coerceAtLeast(0.0)
                val confidence = when {
                    useful.size >= 10 && last.time >= now - 21L * DAY -> "High"
                    useful.size >= 6 -> "Medium"
                    else -> "Low"
                }
                val margin = when (confidence) {
                    "High" -> 0.025
                    "Medium" -> 0.05
                    else -> 0.085
                }
                val target = nextStrengthTarget(last.estimatedOneRm)
                val weeksToTarget = if (trend > 0) ((target - last.estimatedOneRm) / trend).takeIf { it.isFinite() && it > 0 } else null
                StrengthForecast(
                    exerciseName = last.exerciseName,
                    currentEstimatedOneRm = last.estimatedOneRm,
                    projectedEstimatedOneRm = projected,
                    lowerBoundEstimatedOneRm = projected * (1.0 - margin),
                    upperBoundEstimatedOneRm = projected * (1.0 + margin),
                    targetEstimatedOneRm = target,
                    targetDateMillis = weeksToTarget?.let { System.currentTimeMillis() + (it * 7.0 * DAY).toLong() },
                    weeks = 5,
                    confidence = confidence,
                    trendPerWeek = trend
                )
            }
            .sortedByDescending { kotlin.math.abs(it.trendPerWeek) }
            .take(5)
    }

    private fun buildTrainingDna(
        sessions: List<SessionWithExercises>,
        entries: List<SessionExerciseWithSets>
    ): TrainingDnaSummary {
        val allSets = entries.flatMap { it.sets }.filter { it.setType != "Warmup" }
        if (sessions.size < 5 || allSets.size < 20) return TrainingDnaSummary()
        val repBucket = allSets.groupBy { repBucket(it.reps) }.maxByOrNull { it.value.size }?.key ?: "Unknown"
        val avgRpe = allSets.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()
        val weeks = ((sessions.maxOf { it.session.startTime } - sessions.minOf { it.session.startTime }).toDouble() / (7.0 * DAY)).coerceAtLeast(1.0)
        val hardSetsPerWeek = allSets.size / weeks
        val topMovement = entries.groupBy { movementBucket(it.exercise.movementPattern.ifBlank { it.exercise.category }) }
            .maxByOrNull { (_, group) -> group.sumOf { it.sets.hardSetsVolume() } }?.key ?: "Unknown"
        val topExercise = entries.groupBy { it.exercise.name }
            .maxByOrNull { (_, group) -> group.sumOf { it.sets.hardSetsVolume() } }?.key ?: "Unknown"
        val note = "You currently log most work in $repBucket reps, average ${"%.1f".format(hardSetsPerWeek)} hard sets/week, with highest volume in $topMovement."
        return TrainingDnaSummary(repBucket, avgRpe, hardSetsPerWeek, topMovement, topExercise, note)
    }

    private fun buildNextBestActions(
        recovery: RecoveryInsight,
        plateaus: List<PlateauAlert>,
        movementBalance: List<BalancePoint>,
        muscleBalance: List<BalancePoint>,
        forecasts: List<StrengthForecast>,
        dna: TrainingDnaSummary
    ): List<ActionRecommendation> {
        val actions = mutableListOf<ActionRecommendation>()
        if (recovery.label == "Fatigued") {
            actions += ActionRecommendation("Reduce fatigue", "Your recovery signal is low. Consider reducing volume or avoiding failure work next session.", "High")
        }
        plateaus.firstOrNull()?.let {
            actions += ActionRecommendation("Address ${it.exerciseName}", it.suggestion, "High")
        }
        val push = movementBalance.firstOrNull { it.label.equals("Push", true) }?.percentage ?: 0.0
        val pull = movementBalance.firstOrNull { it.label.equals("Pull", true) }?.percentage ?: 0.0
        if (push > 0.0 && pull > 0.0 && pull < push * 0.7) {
            actions += ActionRecommendation("Balance pull volume", "Pull volume is much lower than push. Add rows, pulldowns or rear-delt work.", "Medium")
        }
        muscleBalance.lastOrNull()?.let {
            if (muscleBalance.size >= 4) actions += ActionRecommendation("Bring up ${it.label}", "This muscle is underrepresented. Consider adding ${suggestedExerciseForMuscle(it.label)}.", "Low")
        }
        forecasts.firstOrNull { it.trendPerWeek > 0 }?.let {
            actions += ActionRecommendation("Keep progressing ${it.exerciseName}", "Projected +${"%.1f".format(it.projectedEstimatedOneRm - it.currentEstimatedOneRm)} in ${it.weeks} weeks if trend holds.", "Medium")
        }
        if (actions.isEmpty() && dna.topMovementPattern != "Unknown") {
            actions += ActionRecommendation("Stay consistent", dna.profileNote, "Low")
        }
        return actions.take(4)
    }

    private fun buildMilestones(workouts: Int, sets: Int, volume: Double, prs: Int, streak: Int): List<Milestone> = listOf(
        milestone("First 10 workouts", "Build the logging habit", workouts, 10),
        milestone("30 workouts", "A real training block", workouts, 30),
        milestone("100 workouts", "Long-term consistency", workouts, 100),
        milestone("1,000 sets", "Serious volume banked", sets, 1_000),
        milestone("100 PRs", "Progress keeps stacking", prs, 100),
        milestone("7-day streak", "Training week locked in", streak, 7),
        milestone("1,000,000 lifted", "One million total load", volume.toInt(), 1_000_000)
    )

    private fun milestone(title: String, description: String, value: Int, target: Int): Milestone = Milestone(
        title = title,
        description = "$description • ${value.coerceAtMost(target)}/$target",
        achieved = value >= target,
        progress = (value.toDouble() / target.toDouble()).coerceIn(0.0, 1.0)
    )

    private fun currentStreakDays(sessions: List<SessionWithExercises>): Int {
        val days = sessions.map { dayStart(it.session.startTime) }.toSet()
        if (days.isEmpty()) return 0
        var cursor = dayStart(System.currentTimeMillis())
        if (cursor !in days) cursor -= DAY
        var streak = 0
        while (cursor in days) {
            streak++
            cursor -= DAY
        }
        return streak
    }

    private fun List<SetLog>.hardSetsVolume(): Double = filter { it.setType != "Warmup" }.sumOf { it.weight * it.reps }
    private fun splitCsv(value: String): List<String> = value.split(",").map { it.trim() }.filter { it.isNotBlank() }
    private fun movementBucket(pattern: String): String = pattern.substringBefore("•").trim().ifBlank { pattern.trim().ifBlank { "Other" } }
    private fun suggestedExerciseForMuscle(muscle: String): String = when {
        muscle.contains("Lat", true) -> "pulldowns, pull-ups or rows"
        muscle.contains("Hamstring", true) -> "Romanian deadlifts or leg curls"
        muscle.contains("Rear", true) || muscle.contains("Rhomboid", true) -> "face pulls or chest-supported rows"
        muscle.contains("Quad", true) -> "squats, leg press or split squats"
        muscle.contains("Tricep", true) -> "close-grip pressing or pushdowns"
        muscle.contains("Bicep", true) -> "curls or chin-ups"
        muscle.contains("Glute", true) -> "hip hinges, squats or hip thrusts"
        muscle.contains("Calf", true) -> "standing or seated calf raises"
        muscle.contains("Core", true) || muscle.contains("Abs", true) -> "planks, carries or leg raises"
        else -> "1–2 targeted accessory movements"
    }

    private fun nextStrengthTarget(current: Double): Double {
        val increment = when {
            current >= 200.0 -> 10.0
            current >= 100.0 -> 5.0
            else -> 2.5
        }
        return (kotlin.math.floor(current / increment) + 1.0) * increment
    }

    private fun repBucket(reps: Int): String = when (reps) {
        in 1..3 -> "1–3"
        in 4..6 -> "4–6"
        in 7..10 -> "7–10"
        in 11..15 -> "11–15"
        else -> "16+"
    }
    private fun estimatedOneRm(weight: Double, reps: Int): Double = if (reps <= 1) weight else weight * (1.0 + reps / 30.0)

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

    private fun dayStart(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private companion object {
        const val DAY = 24L * 60L * 60L * 1000L
    }
}
