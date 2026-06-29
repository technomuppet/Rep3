package com.replog.domain.library

import com.replog.data.model.Exercise

/**
 * "Why this exercise?" (Phase 3). Builds a short rationale from the user's goal
 * and, when available, real recovery context. It NEVER invents advice: the
 * recovery sentence is only produced when a concrete recovery status is passed in
 * by the caller (derived upstream from the existing Recovery / Intelligence
 * engines). With no data, it falls back to a goal-based statement only.
 */
object WhyThisExercise {

    /**
     * @param goal the user's primary goal name (e.g. HYPERTROPHY) or null.
     * @param displayName the user's name for personalisation, or null.
     * @param recoveredMuscle a muscle the Recovery Centre reports as fully
     *        recovered that this exercise targets, or null when unknown. Supplied
     *        by the caller from existing engines; this function does not compute it.
     */
    fun rationale(ex: Exercise, goal: String?, displayName: String?, recoveredMuscle: String?): String {
        val goalPhrase = goalPhrase(goal)
        val primary = ex.primaryMuscles.split(",").firstOrNull()?.trim()?.ifBlank { null }
            ?: ex.muscles.split(",").firstOrNull()?.trim()
        val base = if (primary != null)
            "This exercise develops the ${primary.lowercase()}$goalPhrase."
        else
            "This exercise develops the target muscles$goalPhrase."

        if (recoveredMuscle != null) {
            val name = displayName?.takeIf { it.isNotBlank() }
            val lead = if (name != null) "$name, your" else "Your"
            return "$lead Recovery Centre shows your ${recoveredMuscle.lowercase()} is fully recovered today, making this an ideal exercise. $base"
        }
        return base
    }

    private fun goalPhrase(goal: String?): String = when (goal?.uppercase()) {
        "HYPERTROPHY" -> " and matches your goal of muscle growth"
        "STRENGTH" -> " and matches your goal of building strength"
        "FAT_LOSS" -> " and supports your goal of fat loss"
        "GENERAL" -> " and supports your general fitness goal"
        else -> ""
    }
}
