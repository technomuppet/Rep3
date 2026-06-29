package com.replog.domain.library

import com.replog.data.model.Exercise

/** The beginner confidence card (Phase 2). Pure, derived, zero storage. */
data class ConfidenceCard(
    val difficulty: String,            // Beginner / Intermediate / Advanced / Custom
    val equipment: String,
    val estimatedLearningMinutes: Int,
    val idealExperience: String        // e.g. "First Week Friendly"
)

/** A suggested easier alternative to learn first (Phase 2). */
data class EasierAlternative(
    val exercise: Exercise,
    val reason: String
)

object BeginnerGuidance {

    fun confidence(ex: Exercise): ConfidenceCard {
        val diff = ex.difficulty.ifBlank { "Intermediate" }
        val isBodyweightOrDb = ex.equipment.lowercase() in setOf("bodyweight", "none", "dumbbell", "band", "machine")
        val minutes = when (diff.lowercase()) {
            "beginner" -> if (isBodyweightOrDb) 5 else 10
            "intermediate" -> 15
            "advanced" -> 25
            else -> 10
        }
        val ideal = when (diff.lowercase()) {
            "beginner" -> "First Week Friendly"
            "intermediate" -> "A Few Weeks In"
            "advanced" -> "Experienced Lifters"
            else -> "All Levels"
        }
        return ConfidenceCard(
            difficulty = diff,
            equipment = ex.equipment.ifBlank { "Bodyweight" },
            estimatedLearningMinutes = minutes,
            idealExperience = ideal
        )
    }

    /**
     * For an advanced (or, secondarily, intermediate) movement, find an easier
     * variation to recommend first. Reuses the existing similarity logic by
     * scoring same-family library exercises with a lower difficulty. Returns null
     * when the exercise is already beginner-friendly or no easier match exists.
     */
    fun easierAlternative(ex: Exercise, library: List<Exercise>): EasierAlternative? {
        val rank = difficultyRank(ex.difficulty)
        if (rank <= 1) return null // already Beginner (or Custom): nothing easier to suggest
        val fromFamily = ExerciseCoach.familyOf(ex)
        val fromPrimary = primarySet(ex)
        val candidate = library.asSequence()
            .filter { it.id != ex.id }
            .filter { difficultyRank(it.difficulty) in 1 until rank }       // strictly easier, known difficulty
            .filter { ExerciseCoach.familyOf(it) == fromFamily }            // same movement family
            .filter { fromPrimary.isEmpty() || primarySet(it).any { m -> m in fromPrimary } }
            .sortedWith(
                compareByDescending<Exercise> { primarySet(it).count { m -> m in fromPrimary } }
                    .thenBy { difficultyRank(it.difficulty) }               // easiest first
                    .thenBy { preferSimpleEquipment(it) }
                    .thenBy { it.name }
            )
            .firstOrNull() ?: return null
        return EasierAlternative(
            exercise = candidate,
            reason = "Recommended first - an easier ${candidate.difficulty.lowercase()} variation to build confidence."
        )
    }

    private fun difficultyRank(d: String): Int = when (d.lowercase()) {
        "beginner" -> 1
        "intermediate" -> 2
        "advanced" -> 3
        else -> 0 // Custom / unknown: not comparable
    }

    /** Lower is simpler equipment, so it sorts first. */
    private fun preferSimpleEquipment(ex: Exercise): Int = when (ex.equipment.lowercase()) {
        "bodyweight", "none" -> 0
        "dumbbell", "band", "kettlebell" -> 1
        "machine", "cable", "smith machine" -> 2
        else -> 3
    }

    private fun primarySet(ex: Exercise): Set<String> =
        (ex.primaryMuscles.ifBlank { ex.muscles }).split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
}
