package com.replog.domain.musclegap

import com.replog.data.model.Exercise

/**
 * Priority 2 (#8) — Muscle Gap Analysis.
 *
 * For each weak muscle the Training DNA already identified, suggest exercises
 * from the existing library that primarily train it. Pure library lookup + a
 * little ranking; no new analytics and no schema change. The UI offers a
 * one-tap "add to template" using the suggested exercises.
 */
data class MuscleGapSuggestion(
    val muscle: String,
    val exercises: List<Exercise>
)

object MuscleGapAnalyzer {

    /**
     * @param weakMuscles muscle names from TrainingDnaSnapshot.weakestMuscles
     * @param library the full exercise library
     * @param perMuscle how many suggestions to surface per weak muscle
     */
    fun analyze(
        weakMuscles: List<String>,
        library: List<Exercise>,
        perMuscle: Int = 3
    ): List<MuscleGapSuggestion> {
        return weakMuscles
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { muscle ->
                MuscleGapSuggestion(
                    muscle = muscle,
                    exercises = suggestionsFor(muscle, library, perMuscle)
                )
            }
            .filter { it.exercises.isNotEmpty() }
    }

    /** Suggestions for a single muscle (also usable directly from a tapped muscle). */
    fun suggestionsFor(muscle: String, library: List<Exercise>, limit: Int = 3): List<Exercise> {
        val target = muscle.trim().lowercase()
        if (target.isBlank()) return emptyList()

        // Prefer exercises where the muscle is PRIMARY; fall back to secondary.
        val primaryHits = library.filter { ex ->
            ex.primaryMuscles.split(",").any { it.trim().lowercase() == target }
        }
        val secondaryHits = library.filter { ex ->
            ex.secondaryMuscles.split(",").any { it.trim().lowercase() == target } &&
                ex.primaryMuscles.split(",").none { it.trim().lowercase() == target }
        }

        // Rank: compound (more muscles worked) and beginner-friendly first, then
        // diversify equipment so a user isn't shown four cable variations.
        val ranked = (primaryHits + secondaryHits)
            .sortedWith(
                compareByDescending<Exercise> { it.primaryMuscles.split(",").any { m -> m.trim().lowercase() == target } }
                    .thenByDescending { it.muscles.split(",").count { m -> m.isNotBlank() } }
                    .thenBy { difficultyRank(it.difficulty) }
            )

        return diversifyByEquipment(ranked, limit)
    }

    private fun diversifyByEquipment(ranked: List<Exercise>, limit: Int): List<Exercise> {
        val seenEquipment = mutableSetOf<String>()
        val chosen = mutableListOf<Exercise>()
        // First pass: one per equipment type for variety.
        for (ex in ranked) {
            if (chosen.size >= limit) break
            if (seenEquipment.add(ex.equipment)) chosen += ex
        }
        // Second pass: fill remaining slots with the best remaining.
        if (chosen.size < limit) {
            for (ex in ranked) {
                if (chosen.size >= limit) break
                if (ex !in chosen) chosen += ex
            }
        }
        return chosen
    }

    private fun difficultyRank(difficulty: String): Int = when (difficulty.lowercase()) {
        "beginner" -> 0
        "intermediate" -> 1
        "advanced" -> 2
        else -> 1
    }
}
