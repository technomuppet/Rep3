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
        val name = displayName?.takeIf { it.isNotBlank() }
        val rawMuscle = (ex.primaryMuscles.split(",").firstOrNull()?.trim()?.ifBlank { null }
            ?: ex.muscles.split(",").firstOrNull()?.trim())
        val muscle = rawMuscle?.let { friendlyMuscle(it.lowercase()) }

        // 1. Recovery FIRST when we have it - it's the most personal, "today" reason.
        if (recoveredMuscle != null) {
            val lead = if (name != null) "$name, today's pick" else "Today's pick"
            parts += "$lead: your Recovery Centre shows your ${recoveredMuscle.lowercase()} is fresh and fully recovered, so this is a great time to train it."
        } else if (name != null) {
            parts += "$name, here's why this is in your plan:"
        }

        // 2. Goal + muscle woven into one natural sentence.
        parts += goalSentence(goal, muscle)

        // 3. Experience-level fit (only when it adds something).
        experienceSentence(ex.difficulty, experienceLevel)?.let { parts += it }

        // 4. A short, varied closing reason (avoids the same templated tail).
        recommendationClose(ex, goal, experienceLevel, recoveredMuscle != null)?.let { parts += it }

        return parts.joinToString(" ")
    }

    private fun goalSentence(goal: String?, muscle: String?): String {
        val m = muscle ?: "the muscles you're training"
        return when (goal?.uppercase()) {
            "HYPERTROPHY" -> "It's a strong choice for building muscle, working your $m through a full, controlled range."
            "STRENGTH" -> "It builds real strength in your $m, which carries over to your bigger lifts."
            "FAT_LOSS" -> "It works your $m and keeps the effort up, supporting your fat-loss goal."
            "GENERAL" -> "It's a well-rounded movement for your $m that fits a general fitness goal."
            else -> "It's a solid, effective way to train your $m."
        }
    }

    /** Backwards-compatible overload (no experience level). */
    fun rationale(ex: Exercise, goal: String?, displayName: String?, recoveredMuscle: String?): String =
        rationale(ex, goal, null, displayName, recoveredMuscle)

    private fun friendlyMuscle(m: String): String = when {
        m.contains("pectoral") || m == "chest" -> "chest"
        m.contains("quad") -> "quads"
        m.contains("hamstring") -> "hamstrings"
        m.contains("glute") -> "glutes"
        m.contains("calf") || m.contains("calves") -> "calves"
        m.contains("front delt") -> "front shoulders"
        m.contains("side delt") -> "side shoulders"
        m.contains("rear delt") -> "rear shoulders"
        m.contains("delt") || m == "shoulders" -> "shoulders"
        m.contains("bicep") -> "biceps"
        m.contains("tricep") -> "triceps"
        m.contains("forearm") -> "forearms"
        m.contains("oblique") -> "obliques"
        m.contains("abdom") || m == "core" || m == "abs" -> "core"
        m.contains("lat") -> "lats"
        m.contains("trap") -> "traps"
        m.contains("upper back") -> "upper back"
        m.contains("lower back") || m.contains("erector") -> "lower back"
        m == "back" -> "back"
        m.contains("abductor") -> "outer hips"
        m.contains("adductor") -> "inner thighs"
        m.contains("cardio") -> "heart and lungs"
        m.contains("full body") -> "whole body"
        else -> m
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

    /**
     * A short closing reason that varies by how well it fits, so the paragraph
     * doesn't end with the same sentence every time. Returns null when earlier
     * sentences already cover it (e.g. when a recovery lead was used).
     */
    private fun recommendationClose(ex: Exercise, goal: String?, level: String?, hadRecoveryLead: Boolean): String? {
        if (hadRecoveryLead) return null // the recovery lead already explained "why today"
        val goalMatch = ExerciseFilter.matchesGoalPublic(ex, goal)
        val levelMatch = level == null || ex.difficulty.equals(level, true)
        return when {
            goalMatch && levelMatch -> "That mix of fit and difficulty is why it's a good match for you right now."
            goalMatch -> "That's why it's a good fit for what you're working toward."
            levelMatch -> "And it's pitched at the right level for where you are now."
            else -> "It's a dependable option you can rotate into your training."
        }
    }
}
