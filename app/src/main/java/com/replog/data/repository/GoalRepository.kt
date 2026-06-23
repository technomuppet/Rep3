package com.replog.data.repository

import com.replog.data.db.GoalDao
import com.replog.data.model.BodyweightLog
import com.replog.data.model.Goal
import com.replog.data.model.SetLog
import com.replog.domain.goals.GoalEngine
import com.replog.domain.goals.GoalForecast
import com.replog.domain.goals.GoalProgressInput
import com.replog.domain.goals.GoalType
import com.replog.util.PRCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** A goal plus its freshly computed forecast, ready for the UI. */
data class GoalWithForecast(val goal: Goal, val forecast: GoalForecast)

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val setLogDao: com.replog.data.db.SetLogDao,
    private val bodyweightRepository: BodyweightRepository
) {
    fun getAll(): Flow<List<Goal>> = goalDao.getAll()
    fun getActive(): Flow<List<Goal>> = goalDao.getActive()

    suspend fun createStrengthGoal(
        exerciseId: Int,
        exerciseName: String,
        target1rm: Double,
        useKg: Boolean
    ): Long {
        val current = currentE1rm(exerciseId)
        return goalDao.insert(
            Goal(
                goalType = GoalType.STRENGTH_1RM.name,
                title = "$exerciseName ${fmt(target1rm)}${unit(useKg)}",
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                targetValue = target1rm,
                startValue = current
            )
        )
    }

    suspend fun createRepGoal(exerciseId: Int, exerciseName: String, targetReps: Int): Long {
        val current = currentMaxReps(exerciseId).toDouble()
        return goalDao.insert(
            Goal(
                goalType = GoalType.STRENGTH_REPS.name,
                title = "$targetReps× $exerciseName",
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                targetValue = targetReps.toDouble(),
                targetReps = targetReps,
                startValue = current
            )
        )
    }

    suspend fun createBodyweightGoal(targetKg: Double, useKg: Boolean): Long {
        val current = bodyweightRepository.getLatestBodyweight()?.weight ?: targetKg
        val type = if (targetKg < current) GoalType.BODYWEIGHT_LOSS else GoalType.BODYWEIGHT_GAIN
        return goalDao.insert(
            Goal(
                goalType = type.name,
                title = "${if (type == GoalType.BODYWEIGHT_LOSS) "Reach" else "Reach"} ${fmt(targetKg)}${unit(useKg)} bodyweight",
                targetValue = targetKg,
                startValue = current
            )
        )
    }

    suspend fun delete(goal: Goal) = goalDao.delete(goal)

    /** Compute the live forecast for a goal from current data. */
    suspend fun forecastFor(goal: Goal, useKg: Boolean): GoalForecast {
        val type = runCatching { GoalType.valueOf(goal.goalType) }.getOrDefault(GoalType.STRENGTH_1RM)
        val input = when (type) {
            GoalType.STRENGTH_1RM -> {
                val sets = goal.exerciseId?.let { setLogDao.getAllSetsForExercise(it) } ?: emptyList()
                val (current, rate) = e1rmTrend(sets)
                GoalProgressInput(type, goal.startValue, goal.targetValue, current, rate, sets.size >= 2)
            }
            GoalType.STRENGTH_REPS -> {
                val sets = goal.exerciseId?.let { setLogDao.getAllSetsForExercise(it) } ?: emptyList()
                val (current, rate) = repsTrend(sets)
                GoalProgressInput(type, goal.startValue, goal.targetValue, current, rate, sets.size >= 2)
            }
            GoalType.BODYWEIGHT_LOSS, GoalType.BODYWEIGHT_GAIN -> {
                val logs = bodyweightRepository.getAllBodyweights().first()
                val (current, rate) = bodyweightTrend(logs)
                GoalProgressInput(type, goal.startValue, goal.targetValue, current, rate, logs.size >= 2)
            }
        }
        val forecast = GoalEngine.forecast(input, goal.title, useKg)
        // Auto-mark achieved (best-effort persistence).
        if (forecast.status == com.replog.domain.goals.GoalStatus.ACHIEVED && goal.status == "active") {
            goalDao.update(goal.copy(status = "achieved", achievedAt = System.currentTimeMillis()))
        }
        return forecast
    }

    // --- data extraction helpers ---

    private suspend fun currentE1rm(exerciseId: Int): Double {
        val sets = setLogDao.getAllSetsForExercise(exerciseId)
        return sets.maxOfOrNull { PRCalculator.epley1RM(it.weight, it.reps) } ?: 0.0
    }

    private suspend fun currentMaxReps(exerciseId: Int): Int {
        val sets = setLogDao.getAllSetsForExercise(exerciseId)
        return sets.maxOfOrNull { it.reps } ?: 0
    }

    /** Returns (currentBestE1rm, weeklyRateKg) using best-e1RM per recent window. */
    private fun e1rmTrend(sets: List<SetLog>): Pair<Double, Double> {
        if (sets.isEmpty()) return 0.0 to 0.0
        val sorted = sets.sortedBy { it.timestamp }
        val current = sorted.maxOf { PRCalculator.epley1RM(it.weight, it.reps) }
        return current to weeklyRate(sorted.map { it.timestamp to PRCalculator.epley1RM(it.weight, it.reps) })
    }

    private fun repsTrend(sets: List<SetLog>): Pair<Double, Double> {
        if (sets.isEmpty()) return 0.0 to 0.0
        val sorted = sets.sortedBy { it.timestamp }
        val current = sorted.maxOf { it.reps }.toDouble()
        return current to weeklyRate(sorted.map { it.timestamp to it.reps.toDouble() })
    }

    private fun bodyweightTrend(logs: List<BodyweightLog>): Pair<Double, Double> {
        if (logs.isEmpty()) return 0.0 to 0.0
        val sorted = logs.sortedBy { it.timestamp }
        val current = sorted.last().weight
        return current to weeklyRate(sorted.map { it.timestamp to it.weight })
    }

    /**
     * Simple least-effort weekly rate: (last running-best − value ~8 weeks ago) / weeks.
     * Robust to noise: uses the cumulative max for strength/reps via the caller,
     * and raw values for bodyweight (already monotone-ish over time).
     */
    private fun weeklyRate(points: List<Pair<Long, Double>>): Double {
        if (points.size < 2) return 0.0
        val now = points.last().first
        val window = 56L * 24 * 60 * 60 * 1000 // 8 weeks
        val recent = points.filter { it.first >= now - window }
        val first = recent.first()
        val last = recent.last()
        val weeks = ((last.first - first.first) / (7.0 * 24 * 60 * 60 * 1000)).coerceAtLeast(1.0)
        return (last.second - first.second) / weeks
    }

    private fun unit(useKg: Boolean) = if (useKg) "kg" else "lb"
    private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
}
