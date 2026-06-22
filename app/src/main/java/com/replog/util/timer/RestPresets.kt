package com.replog.util.timer

import com.replog.data.model.Exercise

data class RestPresets(
    val compoundSeconds: Int = 180,
    val isolationSeconds: Int = 90,
    val bodyweightSeconds: Int = 60,
    val customByExerciseId: Map<Int, Int> = emptyMap()
)

object RestPresetResolver {
    private val compoundCategories = setOf("Chest", "Back", "Legs", "Shoulders")
    private val compoundEquipment = setOf("Barbell")
    private val compoundPatterns = listOf("Squat", "Deadlift", "Press", "Row", "Pull")

    fun resolve(exercise: Exercise, presets: RestPresets, defaultFallback: Int = 90): Int {
        presets.customByExerciseId[exercise.id]?.let { return it.coerceIn(15, 600) }
        if (exercise.equipment.equals("Bodyweight", true) || exercise.type.equals("Bodyweight", true)) {
            return presets.bodyweightSeconds
        }
        val isCompound = exercise.category in compoundCategories ||
                exercise.equipment in compoundEquipment ||
                compoundPatterns.any { exercise.movementPattern.contains(it, true) } ||
                exercise.difficulty.equals("Advanced", true)
        return if (isCompound) presets.compoundSeconds else presets.isolationSeconds
    }
}
