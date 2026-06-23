package com.replog.domain.library

import com.replog.data.model.Exercise

/**
 * Exercise Library 2.0 — multi-select, combinable filtering (offline, pure).
 *
 * Muscle chips map to the library's fine-grained muscle vocabulary so that
 * selecting e.g. "Shoulders" matches Front/Side/Rear Deltoids. Multiple muscle
 * chips combine with OR (Back + Core shows exercises hitting either), while the
 * different filter dimensions (muscle / equipment / difficulty / pattern)
 * combine with AND. Search remains a further AND constraint.
 */
data class ExerciseFilterState(
    val muscles: Set<String> = emptySet(),       // muscle-group chip labels
    val equipment: Set<String> = emptySet(),
    val difficulties: Set<String> = emptySet(),
    val patterns: Set<String> = emptySet(),       // movement-pattern families
    val query: String = ""
) {
    val isEmpty: Boolean
        get() = muscles.isEmpty() && equipment.isEmpty() && difficulties.isEmpty() &&
            patterns.isEmpty() && query.isBlank()
}

object ExerciseFilter {

    /** The muscle-group chips shown in the UI, in display order. */
    val MUSCLE_GROUPS: List<String> = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Forearms",
        "Core", "Quads", "Hamstrings", "Glutes", "Calves"
    )

    /** Movement-pattern family chips. */
    val PATTERNS: List<String> = listOf("Push", "Pull", "Legs", "Hinge", "Core", "Conditioning", "Shoulders")

    /** Map a muscle-group chip to the fine-grained muscle names it should match. */
    private val groupToMuscles: Map<String, Set<String>> = mapOf(
        "Chest" to setOf("pectorals"),
        "Back" to setOf("back", "lats", "upper back", "traps"),
        "Shoulders" to setOf("front deltoids", "side deltoids", "rear deltoids", "shoulders", "deltoids"),
        "Biceps" to setOf("biceps"),
        "Triceps" to setOf("triceps"),
        "Forearms" to setOf("forearms"),
        "Core" to setOf("core", "obliques"),
        "Quads" to setOf("quadriceps", "quads"),
        "Hamstrings" to setOf("hamstrings"),
        "Glutes" to setOf("glutes"),
        "Calves" to setOf("calves")
    )

    fun apply(all: List<Exercise>, f: ExerciseFilterState): List<Exercise> {
        if (f.isEmpty) return all
        val targetMuscles: Set<String> = f.muscles.flatMap { groupToMuscles[it] ?: setOf(it.lowercase()) }.toSet()
        val equip = f.equipment.map { it.lowercase() }.toSet()
        val diffs = f.difficulties.map { it.lowercase() }.toSet()
        val pats = f.patterns.map { it.lowercase() }.toSet()
        val q = f.query.trim()

        return all.filter { ex ->
            matchesMuscles(ex, targetMuscles) &&
                (equip.isEmpty() || ex.equipment.lowercase() in equip) &&
                (diffs.isEmpty() || ex.difficulty.lowercase() in diffs) &&
                (pats.isEmpty() || patternFamily(ex) in pats) &&
                (q.isBlank() || matchesQuery(ex, q))
        }
    }

    private fun matchesMuscles(ex: Exercise, targets: Set<String>): Boolean {
        if (targets.isEmpty()) return true
        val exMuscles = (ex.primaryMuscles + "," + ex.secondaryMuscles + "," + ex.muscles)
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        return targets.any { t -> exMuscles.any { it == t || it.contains(t) } }
    }

    private fun patternFamily(ex: Exercise): String =
        ex.movementPattern.substringBefore("\u2022").trim().lowercase()

    private fun matchesQuery(ex: Exercise, q: String): Boolean =
        ex.name.contains(q, true) || ex.category.contains(q, true) ||
            ex.equipment.contains(q, true) || ex.muscles.contains(q, true) ||
            ex.primaryMuscles.contains(q, true) || ex.secondaryMuscles.contains(q, true) ||
            ex.movementPattern.contains(q, true)
}
