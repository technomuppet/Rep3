package com.replog.domain.progression

import com.replog.data.model.SetLog

data class ProgressionSuggestion(
    val weight: Double,
    val reps: Int,
    val rpe: Double?,
    val source: String
)

object ProgressionSuggester {
    fun suggest(lastSets: List<SetLog>, useKg: Boolean = true): ProgressionSuggestion? {
        val last = lastSets.lastOrNull() ?: return null
        val avgReps = lastSets.takeLast(3).map { it.reps }.average()
        val hitTarget = avgReps >= last.reps - 0.5
        val increment = if (useKg) when {
            last.weight >= 100 -> 2.5
            last.weight >= 60 -> 2.5
            else -> 1.25
        } else 5.0
        val nextWeight = if (hitTarget) last.weight + increment else last.weight
        return ProgressionSuggestion(
            weight = (kotlin.math.round(nextWeight * 4) / 4.0),
            reps = last.reps,
            rpe = last.rpe,
            source = if (hitTarget) "Auto-progression" else "Repeat last"
        )
    }
}
