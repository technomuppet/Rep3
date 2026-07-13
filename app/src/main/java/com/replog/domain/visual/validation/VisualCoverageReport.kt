package com.replog.domain.visual.validation

/**
 * Complete verification summary reporting resolved coverage across an entire exercise catalog.
 */
data class VisualCoverageReport(
    val totalExercises: Int,
    val successfullyResolvedCount: Int,
    val fallbackUsedCount: Int,
    val unknownEquipmentCount: Int,
    val unknownMovementPatternCount: Int,
    val missingMusclesCount: Int,
    val unmappedExerciseNames: List<String>,
    val unknownEquipmentExercises: List<String>,
    val unknownMovementPatternExercises: List<String>,
    val missingMusclesExercises: List<String>,
    val movementFamilyCoverage: List<MovementCoverage>
) {
    val coveragePercentage: Float
        get() = if (totalExercises == 0) 100.0f else (successfullyResolvedCount.toFloat() / totalExercises.toFloat()) * 100.0f
}
