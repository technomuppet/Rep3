package com.replog.domain.goals

import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Goal Engine — goal-centric forecasting (fully offline, pure Kotlin).
 *
 * Turns a target ("Bench 100kg", "Lose 5kg", "10 pullups") + the user's recent
 * trend into: current value, progress %, forecast ETA, next milestone and a
 * status. Uses the same estimated-1RM / trend math already present elsewhere;
 * no new tracking, no network.
 */
enum class GoalType {
    STRENGTH_1RM,     // target = estimated 1RM (kg) for an exercise
    STRENGTH_REPS,    // target = max reps at bodyweight/load (e.g. 10 pullups)
    BODYWEIGHT_LOSS,  // target = lower bodyweight (kg)
    BODYWEIGHT_GAIN   // target = higher bodyweight (kg)
}

enum class GoalStatus { ACHIEVED, AHEAD, ON_TRACK, STALLED, NO_DATA }

/** Snapshot inputs the engine needs (gathered from repositories by the caller). */
data class GoalProgressInput(
    val type: GoalType,
    val startValue: Double,
    val targetValue: Double,
    /** Current measured value (current e1RM, current max reps, or current bodyweight). */
    val currentValue: Double,
    /** Change in value per week, signed (e.g. +1.2 kg/week, or −0.4 kg/week for weight loss). */
    val weeklyRate: Double,
    val hasData: Boolean
)

data class GoalForecast(
    val current: Double,
    val target: Double,
    val progressPercent: Int,
    val status: GoalStatus,
    val weeksRemaining: Int?,       // null when unknown / no trend toward target
    val etaText: String,            // friendly text, e.g. "~9 weeks" / "Achieved" / "Not progressing"
    val nextMilestone: Double?,     // next round step toward the target
    val summaryLine: String         // one-line headline for Home/Goals cards
)

object GoalEngine {

    fun forecast(input: GoalProgressInput, label: String, unitsKg: Boolean = true): GoalForecast {
        val unit = unitLabel(input.type, unitsKg)

        if (!input.hasData) {
            return GoalForecast(
                current = input.currentValue,
                target = input.targetValue,
                progressPercent = 0,
                status = GoalStatus.NO_DATA,
                weeksRemaining = null,
                etaText = "Log more data",
                nextMilestone = null,
                summaryLine = "$label — log a few sessions to start forecasting"
            )
        }

        val achieved = isAchieved(input)
        val progress = progressPercent(input)

        if (achieved) {
            return GoalForecast(
                current = round1(input.currentValue),
                target = input.targetValue,
                progressPercent = 100,
                status = GoalStatus.ACHIEVED,
                weeksRemaining = 0,
                etaText = "Achieved 🎉",
                nextMilestone = null,
                summaryLine = "$label — achieved!"
            )
        }

        val movingTowardTarget = isMovingToward(input)
        val remaining = remainingDistance(input)
        val weeks = if (movingTowardTarget && kotlin.math.abs(input.weeklyRate) > 1e-6) {
            ceil(remaining / kotlin.math.abs(input.weeklyRate)).toInt().coerceAtLeast(1)
        } else null

        val status = when {
            !movingTowardTarget -> GoalStatus.STALLED
            weeks != null && weeks <= reasonableWeeks(input) -> GoalStatus.ON_TRACK
            else -> GoalStatus.ON_TRACK
        }

        val milestone = nextMilestone(input)
        val eta = when {
            weeks == null -> "Not progressing yet"
            else -> "~$weeks ${if (weeks == 1) "week" else "weeks"}"
        }
        val cur = round1(input.currentValue)
        val summary = when (status) {
            GoalStatus.STALLED -> "$label — ${fmt(cur)}$unit now, stalled (adjust training)"
            else -> "$label — ${fmt(cur)}$unit → ${fmt(input.targetValue)}$unit in $eta"
        }

        return GoalForecast(
            current = cur,
            target = input.targetValue,
            progressPercent = progress,
            status = status,
            weeksRemaining = weeks,
            etaText = eta,
            nextMilestone = milestone,
            summaryLine = summary
        )
    }

    // --- helpers ---

    private fun isAchieved(i: GoalProgressInput): Boolean = when (i.type) {
        GoalType.BODYWEIGHT_LOSS -> i.currentValue <= i.targetValue
        else -> i.currentValue >= i.targetValue
    }

    private fun isMovingToward(i: GoalProgressInput): Boolean = when (i.type) {
        GoalType.BODYWEIGHT_LOSS -> i.weeklyRate < -1e-6 // losing weight
        else -> i.weeklyRate > 1e-6                      // gaining strength/reps/weight
    }

    private fun remainingDistance(i: GoalProgressInput): Double =
        kotlin.math.abs(i.targetValue - i.currentValue)

    private fun progressPercent(i: GoalProgressInput): Int {
        val total = kotlin.math.abs(i.targetValue - i.startValue)
        if (total < 1e-6) return if (isAchieved(i)) 100 else 0
        val done = kotlin.math.abs(i.currentValue - i.startValue)
        return ((done / total) * 100).roundToInt().coerceIn(0, 100)
    }

    private fun nextMilestone(i: GoalProgressInput): Double? = when (i.type) {
        GoalType.STRENGTH_1RM, GoalType.BODYWEIGHT_GAIN -> {
            val step = 2.5
            val next = (kotlin.math.floor(i.currentValue / step) * step) + step
            if (next < i.targetValue) next else i.targetValue
        }
        GoalType.BODYWEIGHT_LOSS -> {
            val step = 1.0
            val next = (kotlin.math.ceil(i.currentValue / step) * step) - step
            if (next > i.targetValue) next else i.targetValue
        }
        GoalType.STRENGTH_REPS -> {
            val next = kotlin.math.floor(i.currentValue).toInt() + 1
            if (next < i.targetValue) next.toDouble() else i.targetValue
        }
    }

    private fun reasonableWeeks(i: GoalProgressInput): Int = 26

    private fun unitLabel(type: GoalType, kg: Boolean): String = when (type) {
        GoalType.STRENGTH_REPS -> " reps"
        else -> if (kg) " kg" else " lb"
    }

    private fun round1(v: Double): Double = (v * 10).roundToInt() / 10.0
    private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
}
