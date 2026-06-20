package com.replog.util

import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog

data class AdaptiveExerciseTarget(
    val exerciseId: Int,
    val exerciseName: String,
    val previousBestSet: SetLog?,
    val suggestedWeight: Double,
    val suggestedReps: Int,
    val suggestedSets: Int,
    val adjustment: String,
    val reason: String
)

data class AdaptiveWorkoutPlan(
    val title: String,
    val summary: String,
    val sourceSessionId: Int?,
    val targets: List<AdaptiveExerciseTarget>,
    val fatigueModifier: String
)

object AdaptiveProgramEngine {
    fun buildPlan(sessions: List<SessionWithExercises>): AdaptiveWorkoutPlan? {
        val completed = sessions.filter { it.session.endTime != null }.sortedByDescending { it.session.startTime }
        val last = completed.firstOrNull() ?: return null
        val recent = completed.take(4)
        val fatigue = fatigueModifier(recent)
        val targets = last.exercises.sortedBy { it.sessionExercise.orderIndex }.map { entry ->
            val hardSets = entry.sets.filter { it.setType != "Warmup" }
            val best = hardSets.maxByOrNull { estimateOneRm(it.weight, it.reps) } ?: hardSets.lastOrNull()
            val avgRpe = hardSets.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()
            val adjustment = when {
                fatigue == "Reduce" -> "Remove 1 set"
                avgRpe != null && avgRpe >= 9.2 -> "Maintain load"
                hardSets.size >= 4 && avgRpe != null && avgRpe <= 7.5 -> "Add 1 set"
                best != null && best.reps >= 10 -> "Add load"
                else -> "Add 1 rep"
            }
            val suggestedSets = when (adjustment) {
                "Remove 1 set" -> (hardSets.size - 1).coerceAtLeast(1)
                "Add 1 set" -> hardSets.size + 1
                else -> hardSets.size.coerceAtLeast(3)
            }
            val suggestedWeight = when (adjustment) {
                "Add load" -> (best?.weight ?: 0.0) + smallestUsefulJump(best?.weight ?: 0.0)
                else -> best?.weight ?: hardSets.lastOrNull()?.weight ?: 0.0
            }
            val suggestedReps = when (adjustment) {
                "Add 1 rep" -> ((best?.reps ?: hardSets.lastOrNull()?.reps ?: 8) + 1).coerceAtMost(15)
                "Add load" -> ((best?.reps ?: 8) - 2).coerceAtLeast(3)
                else -> best?.reps ?: hardSets.lastOrNull()?.reps ?: 8
            }
            AdaptiveExerciseTarget(
                exerciseId = entry.exercise.id,
                exerciseName = entry.exercise.name,
                previousBestSet = best,
                suggestedWeight = suggestedWeight,
                suggestedReps = suggestedReps,
                suggestedSets = suggestedSets,
                adjustment = adjustment,
                reason = reasonFor(adjustment, avgRpe, fatigue)
            )
        }
        val baseName = last.session.templateName ?: "Last Workout"
        return AdaptiveWorkoutPlan(
            title = "$baseName vNext",
            summary = "Repeat your last completed session with ${fatigue.lowercase()} fatigue adjustment and exercise-level progression targets.",
            sourceSessionId = last.session.id,
            targets = targets,
            fatigueModifier = fatigue
        )
    }

    private fun fatigueModifier(recent: List<SessionWithExercises>): String {
        if (recent.size < 2) return "Maintain"
        val newest = recent.first().volume()
        val average = recent.map { it.volume() }.average().takeIf { it > 0.0 } ?: return "Maintain"
        val highRpeSets = recent.first().exercises.flatMap { it.sets }.count { (it.rpe ?: 0.0) >= 9.0 }
        return when {
            newest > average * 1.3 || highRpeSets >= 4 -> "Reduce"
            newest < average * 0.75 -> "Build"
            else -> "Maintain"
        }
    }

    private fun SessionWithExercises.volume(): Double = exercises.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } }
    private fun estimateOneRm(weight: Double, reps: Int): Double = if (reps <= 1) weight else weight * (1.0 + reps / 30.0)
    private fun smallestUsefulJump(weight: Double): Double = if (weight >= 100.0) 2.5 else 1.25

    private fun reasonFor(adjustment: String, avgRpe: Double?, fatigue: String): String = when {
        fatigue == "Reduce" -> "Recent workload or high-RPE work suggests a lower-fatigue repeat."
        adjustment == "Add 1 set" -> "RPE looks manageable and this lift can tolerate more volume."
        adjustment == "Add load" -> "Rep target is high enough to justify a small load increase."
        adjustment == "Maintain load" -> "RPE is high; consolidate before pushing load."
        avgRpe == null -> "No RPE trend yet; use conservative progression."
        else -> "Small rep progression keeps overload controlled."
    }
}
