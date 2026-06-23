package com.replog.domain.coachdash

import com.replog.domain.recommendation.PlannedExercise
import com.replog.domain.recommendation.ProgressionDecision
import com.replog.domain.recommendation.Recommendation
import com.replog.domain.recommendation.RecommendationType
import kotlin.math.roundToInt

/**
 * Coach Dashboard — assembles a single "Good morning" briefing from the
 * intelligence the app already produces (recommendation + recovery + goals).
 *
 * Pure, deterministic, offline. No new analytics — it only restates existing
 * outputs into one persistent-advisor view.
 */
data class CoachBriefing(
    val greeting: String,
    val recoveryScore: Int?,
    val recoveryDirective: String?,
    val trainLine: String,          // e.g. "Train Push" / "Rest day" / "Repeat last workout"
    val isRestDay: Boolean,
    val focusLine: String?,         // e.g. "Focus: Upper Chest" (weakest muscle / stalled lift)
    val progressionLine: String?,   // e.g. "Bench Press: +2.5kg next session"
    val estimatedMinutes: Int?,     // e.g. 58
    val goalLine: String?,          // e.g. "Bench 100kg → in ~9 weeks"
    val confidence: Int?
)

object CoachBriefingBuilder {

    fun build(
        recommendation: Recommendation?,
        recoveryScore: Int?,
        recoveryDirective: String?,
        weakestMuscle: String?,
        topGoalSummary: String?,
        useKg: Boolean = true,
        hourOfDay: Int = 12,
        userName: String? = null
    ): CoachBriefing {
        val greeting = greeting(hourOfDay, userName)

        if (recommendation == null) {
            return CoachBriefing(
                greeting = greeting,
                recoveryScore = recoveryScore,
                recoveryDirective = recoveryDirective,
                trainLine = "Log a workout to start coaching",
                isRestDay = false,
                focusLine = weakestMuscle?.let { "Focus: $it" },
                progressionLine = null,
                estimatedMinutes = null,
                goalLine = topGoalSummary,
                confidence = null
            )
        }

        val isRest = recommendation.type == RecommendationType.REST
        val trainLine = if (isRest) "Rest day recommended" else recommendation.title

        // Focus = first stalled/undertrained signal from the recommendation, else weakest muscle.
        val focus = focusFrom(recommendation) ?: weakestMuscle
        val focusLine = focus?.let { "Focus: $it" }

        // Progression = the most meaningful planned-exercise progression in the plan.
        val progressionLine = if (isRest) null else progressionFrom(recommendation, useKg)

        return CoachBriefing(
            greeting = greeting,
            recoveryScore = recoveryScore,
            recoveryDirective = recoveryDirective,
            trainLine = trainLine,
            isRestDay = isRest,
            focusLine = focusLine,
            progressionLine = progressionLine,
            estimatedMinutes = recommendation.estimatedDurationMinutes.takeIf { it > 0 && !isRest },
            goalLine = topGoalSummary,
            confidence = recommendation.confidenceScore.roundToInt().takeIf { it > 0 }
        )
    }

    private fun greeting(hour: Int, name: String?): String {
        val tod = when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
        return if (name.isNullOrBlank()) tod else "$tod, $name"
    }

    /** Pull a "focus" muscle/lift from the recommendation's existing dataUsed lines. */
    private fun focusFrom(rec: Recommendation): String? {
        val stalled = rec.dataUsed.firstOrNull { it.startsWith("Stalled lifts") }
            ?.substringAfter(":")?.split(",")?.firstOrNull()?.trim()
        if (!stalled.isNullOrBlank()) return stalled
        val under = rec.dataUsed.firstOrNull { it.startsWith("Undertrained muscles") }
            ?.substringAfter(":")?.split(",")?.firstOrNull()?.trim()
        return under?.takeIf { it.isNotBlank() }
    }

    /** Build a "X: +Ykg next session" line from the most progressable planned exercise. */
    private fun progressionFrom(rec: Recommendation, useKg: Boolean): String? {
        val plan = rec.workoutPlan ?: return null
        val unit = if (useKg) "kg" else "lb"
        // Prefer a load increase with a concrete weight; else first exercise with a target.
        val loadUp = plan.exercises.firstOrNull { it.progression == ProgressionDecision.INCREASE_LOAD && it.targetWeight != null }
        if (loadUp != null) {
            return "${loadUp.exerciseName}: ${fmt(loadUp.targetWeight!!)}$unit next session"
        }
        val firstTargeted: PlannedExercise? = plan.exercises.firstOrNull { it.targetWeight != null }
            ?: plan.exercises.firstOrNull()
        return firstTargeted?.let { ex ->
            val weight = ex.targetWeight?.let { "${fmt(it)}$unit" } ?: "bodyweight"
            "${ex.exerciseName}: $weight × ${ex.targetReps}"
        }
    }

    private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
}
