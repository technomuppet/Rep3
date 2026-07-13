package com.replog.domain.visual.validation

import com.replog.data.model.Exercise
import com.replog.domain.visual.registry.MovementRegistry
import com.replog.domain.visual.resolver.ExerciseVisualResolver
import com.replog.domain.visual.spec.EquipmentType

/**
 * Validation engine verifying that every exercise cleanly resolves to known movement
 * families, valid equipment specifications, and non-empty anatomical mappings.
 */
object ExerciseVisualValidator {

    fun validateCatalog(exercises: List<Exercise>): VisualCoverageReport {
        val unmappedNames = mutableListOf<String>()
        val unknownEquipment = mutableListOf<String>()
        val unknownPattern = mutableListOf<String>()
        val missingMuscles = mutableListOf<String>()
        val familyBuckets = mutableMapOf<String, MutableList<String>>()

        // Initialize buckets for all registered movement families
        MovementRegistry.getAllFamilies().forEach { family ->
            familyBuckets[family.id] = mutableListOf()
        }

        var successCount = 0
        var fallbackCount = 0

        for (ex in exercises) {
            val spec = ExerciseVisualResolver.resolve(ex)
            val isFallback = spec.movementFamily.familyId == "GENERIC_UNMAPPED"
            val isUnknownEq = spec.equipment.type == EquipmentType.OTHER
            val isMissingMuscles = spec.anatomy.primaryMuscles.isEmpty() && spec.anatomy.secondaryMuscles.isEmpty()

            val bucket = familyBuckets.getOrPut(spec.movementFamily.familyId) { mutableListOf() }
            bucket.add(ex.name)

            if (isFallback) {
                fallbackCount++
                unmappedNames.add(ex.name)
                unknownPattern.add(ex.name)
            } else {
                successCount++
            }

            if (isUnknownEq) {
                unknownEquipment.add(ex.name)
            }

            if (isMissingMuscles) {
                missingMuscles.add(ex.name)
            }
        }

        val coverageList = familyBuckets.map { (famId, names) ->
            val meta = MovementRegistry.getById(famId)
            MovementCoverage(
                familyId = famId,
                familyDisplayName = meta.displayName,
                mappedExerciseCount = names.size,
                mappedExerciseNames = names
            )
        }.sortedByDescending { it.mappedExerciseCount }

        return VisualCoverageReport(
            totalExercises = exercises.size,
            successfullyResolvedCount = successCount,
            fallbackUsedCount = fallbackCount,
            unknownEquipmentCount = unknownEquipment.size,
            unknownMovementPatternCount = unmappedNames.size,
            missingMusclesCount = missingMuscles.size,
            unmappedExerciseNames = unmappedNames,
            unknownEquipmentExercises = unknownEquipment,
            unknownMovementPatternExercises = unknownPattern,
            missingMusclesExercises = missingMuscles,
            movementFamilyCoverage = coverageList
        )
    }
}
