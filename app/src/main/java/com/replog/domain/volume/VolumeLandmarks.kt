package com.replog.domain.volume

import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetType

/**
 * Priority 3 (#12) — Volume Landmarks.
 *
 * Counts weekly working sets per muscle group from the existing session data
 * and compares against evidence-based optimal ranges (roughly MEV–MRV).
 * Pure, deterministic; no schema change and no new tracking — it reads the
 * sets the user already logged.
 */
data class VolumeLandmark(
    val muscleGroup: String,
    val weeklySets: Double,
    val optimalLow: Int,
    val optimalHigh: Int,
    val status: VolumeStatus
)

enum class VolumeStatus(val label: String) {
    UNDER("Below optimal"),
    IN_RANGE("In Range"),
    ABOVE("Above optimal"),
    NONE("Not trained")
}

object VolumeLandmarks {

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    /**
     * Evidence-informed weekly working-set ranges per muscle group.
     * (General hypertrophy guidance; deliberately simple and offline.)
     */
    private val OPTIMAL: Map<String, IntRange> = mapOf(
        "Chest" to 10..20,
        "Back" to 10..20,
        "Shoulders" to 8..20,
        "Biceps" to 8..16,
        "Triceps" to 8..16,
        "Quads" to 8..18,
        "Hamstrings" to 6..16,
        "Glutes" to 6..16,
        "Calves" to 8..16,
        "Core" to 6..16
    )

    /** Order to display groups in. */
    val GROUP_ORDER: List<String> = OPTIMAL.keys.toList()

    /**
     * Map a fine-grained library muscle name to a trainable group.
     * Returns null for non-hypertrophy categories (e.g. Cardiovascular).
     */
    private fun groupFor(muscle: String): String? = when (muscle.trim().lowercase()) {
        "pectorals" -> "Chest"
        "lats", "upper back", "traps", "back" -> "Back"
        "front deltoids", "side deltoids", "rear deltoids" -> "Shoulders"
        "biceps" -> "Biceps"
        "triceps" -> "Triceps"
        "quadriceps" -> "Quads"
        "hamstrings" -> "Hamstrings"
        "glutes" -> "Glutes"
        "calves" -> "Calves"
        "core", "obliques" -> "Core"
        else -> null // forearms, abductors, adductors, cardiovascular, full body
    }

    /**
     * Weekly working sets per muscle group over the trailing [weeks] weeks.
     * A working set counts 1.0 for the exercise's primary muscle group(s) and
     * 0.5 for secondary group(s) — standard fractional-set accounting.
     */
    fun analyze(
        sessions: List<SessionWithExercises>,
        nowMillis: Long,
        weeks: Int = 1
    ): List<VolumeLandmark> {
        val windowStart = nowMillis - weeks * 7L * DAY_MS
        val completedInWindow = sessions.filter {
            it.session.endTime != null && it.session.startTime >= windowStart
        }

        val setsByGroup = mutableMapOf<String, Double>()
        for (session in completedInWindow) {
            for (entry in session.exercises) {
                val workingSets = entry.sets.count { it.setType != SetType.WARMUP }
                if (workingSets == 0) continue
                val primaryGroups = entry.exercise.primaryMuscles.split(",")
                    .mapNotNull { groupFor(it) }.distinct()
                val secondaryGroups = entry.exercise.secondaryMuscles.split(",")
                    .mapNotNull { groupFor(it) }.distinct()
                    .filter { it !in primaryGroups }

                primaryGroups.forEach { g -> setsByGroup[g] = (setsByGroup[g] ?: 0.0) + workingSets }
                secondaryGroups.forEach { g -> setsByGroup[g] = (setsByGroup[g] ?: 0.0) + workingSets * 0.5 }
            }
        }

        // Normalise to per-week if a longer window was requested.
        val divisor = weeks.coerceAtLeast(1).toDouble()

        return GROUP_ORDER.map { group ->
            val range = OPTIMAL.getValue(group)
            val weekly = (setsByGroup[group] ?: 0.0) / divisor
            VolumeLandmark(
                muscleGroup = group,
                weeklySets = weekly,
                optimalLow = range.first,
                optimalHigh = range.last,
                status = statusFor(weekly, range)
            )
        }
    }

    private fun statusFor(weekly: Double, range: IntRange): VolumeStatus = when {
        weekly <= 0.0 -> VolumeStatus.NONE
        weekly < range.first -> VolumeStatus.UNDER
        weekly > range.last -> VolumeStatus.ABOVE
        else -> VolumeStatus.IN_RANGE
    }
}
