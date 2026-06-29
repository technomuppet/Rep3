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
    val goals: Set<String> = emptySet(),          // Sprint 13: Hypertrophy / Strength / Fat Loss
    val experiences: Set<String> = emptySet(),    // Sprint 13: First Week / Building Up / Experienced
    val equipmentPresets: Set<String> = emptySet(), // Sprint 13: No Equipment / Home Workout / Machine Only
    val query: String = ""
) {
    val isEmpty: Boolean
        get() = muscles.isEmpty() && equipment.isEmpty() && difficulties.isEmpty() &&
            patterns.isEmpty() && goals.isEmpty() && experiences.isEmpty() &&
            equipmentPresets.isEmpty() && query.isBlank()
}

object ExerciseFilter {

    /** The muscle-group chips shown in the UI, in display order. */
    val MUSCLE_GROUPS: List<String> = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Forearms",
        "Core", "Quads", "Hamstrings", "Glutes", "Calves"
    )

    /** Movement-pattern family chips. */
    val PATTERNS: List<String> = listOf("Push", "Pull", "Legs", "Hinge", "Core", "Conditioning", "Shoulders")

    /** Sprint 13: goal chips. Mapped onto existing attributes, no new data. */
    val GOALS: List<String> = listOf("Hypertrophy", "Strength", "Fat Loss")

    /** Sprint 13: experience chips, mapped onto difficulty. */
    val EXPERIENCES: List<String> = listOf("First Week", "Building Up", "Experienced")

    /** Sprint 13: convenience equipment presets. */
    val EQUIPMENT_PRESETS: List<String> = listOf("No Equipment", "Home Workout", "Machine Only")

    private val experienceToDifficulty: Map<String, String> = mapOf(
        "First Week" to "beginner",
        "Building Up" to "intermediate",
        "Experienced" to "advanced"
    )

    private val homeFriendlyEquipment = setOf("bodyweight", "none", "dumbbell", "band", "kettlebell")
    private val noEquipment = setOf("bodyweight", "none")
    private val machineEquipment = setOf("machine", "smith machine", "cable")

    private fun matchesGoal(ex: Exercise, goals: Set<String>): Boolean {
        if (goals.isEmpty()) return true
        val pattern = ex.movementPattern.lowercase()
        val cat = ex.category.lowercase()
        val type = ex.type.lowercase()
        return goals.any { goal ->
            when (goal.lowercase()) {
                // Hypertrophy: strength-type resistance work (exclude pure cardio).
                "hypertrophy" -> type != "cardio" && cat != "cardio"
                // Strength: the big compound patterns.
                "strength" -> listOf("squat", "hinge", "press", "row", "pull", "olympic").any { pattern.contains(it) }
                // Fat loss: conditioning / cardio / full-body.
                "fat loss" -> type == "cardio" || cat == "cardio" || cat == "full body" || pattern.contains("conditioning")
                else -> true
            }
        }
    }

    private fun matchesExperience(ex: Exercise, experiences: Set<String>): Boolean {
        if (experiences.isEmpty()) return true
        val diff = ex.difficulty.lowercase()
        return experiences.any { experienceToDifficulty[it] == diff }
    }

    private fun matchesEquipmentPreset(ex: Exercise, presets: Set<String>): Boolean {
        if (presets.isEmpty()) return true
        val eq = ex.equipment.lowercase()
        return presets.any { preset ->
            when (preset.lowercase()) {
                "no equipment" -> eq in noEquipment
                "home workout" -> eq in homeFriendlyEquipment
                "machine only" -> eq in machineEquipment
                else -> true
            }
        }
    }

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
                matchesGoal(ex, f.goals) &&
                matchesExperience(ex, f.experiences) &&
                matchesEquipmentPreset(ex, f.equipmentPresets) &&
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
