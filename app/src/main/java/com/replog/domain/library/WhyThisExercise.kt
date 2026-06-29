package com.replog.domain.library

import com.replog.data.model.Exercise

/**
 * "Why this exercise?" (Phase 6). Builds a short rationale from facts that already
 * exist: the user's goal, their experience level (from the profile), the
 * exercise's difficulty, and - when available - real recovery context from the
 * Recovery Centre. It NEVER invents advice: the recovery sentence is only produced
 * when a concrete recovered muscle is passed in by the caller (derived upstream
 * from the existing Recovery / Intelligence engines), and the experience and
 * recommendation sentences are derived purely from the difficulty/level the
 * profile and catalog already store.
 */
object WhyThisExercise {

    /**
     * @param goal the user's primary goal name (e.g. HYPERTROPHY) or null.
     * @param experienceLevel the user's stored experience level
     *        (BEGINNER/INTERMEDIATE/ADVANCED) or null.
     * @param displayName the user's name for personalisation, or null.
     * @param recoveredMuscle a muscle the Recovery Centre reports as fully
     *        recovered that this exercise targets, or null when unknown. Supplied
     *        by the caller from existing engines; this function does not compute it.
     */
    fun rationale(
        ex: Exercise,
        goal: String?,
        experienceLevel: String?,
        displayName: String?,
        recoveredMuscle: String?
    ): String {
        val parts = mutableListOf<String>()

        // 1. Goal fit (from profile goal + this exercise's primary muscle).
        val primary = ex.primaryMuscles.split(",").firstOrNull()?.trim()?.ifBlank { null }
            ?: ex.muscles.split(",").firstOrNull()?.trim()
        parts += if (primary != null)
            "This exercise develops the ${primary.lowercase()}${goalPhrase(goal)}."
        else
            "This exercise develops the target muscles${goalPhrase(goal)}."

        // 2. Experience-level appropriateness (derived from difficulty vs level).
        experienceSentence(ex.difficulty, experienceLevel)?.let { parts += it }

        // 3. Recovery context (only when the caller supplies a recovered muscle).
        if (recoveredMuscle != null) {
            val name = displayName?.takeIf { it.isNotBlank() }
            val lead = if (name != null) "$name, your" else "Your"
            parts += "$lead Recovery Centre shows your ${recoveredMuscle.lowercase()} is fully recovered today, making this an ideal exercise."
        }

        // 4. Why it appears in recommendations (derived from goal + level match).
        parts += recommendationSentence(ex, goal, experienceLevel)

        return parts.joinToString(" ")
    }

    /** Backwards-compatible overload (no experience level). */
    fun rationale(ex: Exercise, goal: String?, displayName: String?, recoveredMuscle: String?): String =
        rationale(ex, goal, null, displayName, recoveredMuscle)

    private fun goalPhrase(goal: String?): String = when (goal?.uppercase()) {
        "HYPERTROPHY" -> " and matches your goal of muscle growth"
        "STRENGTH" -> " and matches your goal of building strength"
        "FAT_LOSS" -> " and supports your goal of fat loss"
        "GENERAL" -> " and supports your general fitness goal"
        else -> ""
    }

    private fun experienceSentence(difficulty: String, level: String?): String? {
        val d = difficulty.lowercase()
        val l = level?.lowercase() ?: return when (d) {
            "beginner" -> "It is beginner-friendly, so it is a safe place to start."
            "advanced" -> "It is an advanced movement, so build up to it once your technique is solid."
            else -> null
        }
        return when {
            d == "beginner" -> "It is beginner-friendly and well suited to your experience level."
            d == "intermediate" && l == "beginner" -> "It is a step up from beginner moves; approach it once the basics feel comfortable."
            d == "intermediate" -> "Its intermediate difficulty matches your experience level."
            d == "advanced" && l == "advanced" -> "Its advanced difficulty suits your experience level."
            d == "advanced" -> "It is an advanced movement, so build up to it as your experience grows."
            else -> null
        }
    }

    private fun recommendationSentence(ex: Exercise, goal: String?, level: String?): String {
        val goalMatch = ExerciseFilter.matchesGoalPublic(ex, goal)
        val levelMatch = level == null || ex.difficulty.equals(level, true)
        return when {
            goalMatch && levelMatch -> "It appears in your recommendations because it fits both your goal and your experience level."
            goalMatch -> "It appears in your recommendations because it aligns with your training goal."
            levelMatch -> "It appears in your recommendations because it suits your experience level."
            else -> "It appears in the library as a solid general option you can include in your training."
        }
    }
}
