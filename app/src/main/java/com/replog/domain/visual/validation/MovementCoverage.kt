package com.replog.domain.visual.validation

/**
 * Summary tracking the number and names of catalog exercises mapped to a specific movement family.
 */
data class MovementCoverage(
    val familyId: String,
    val familyDisplayName: String,
    val mappedExerciseCount: Int,
    val mappedExerciseNames: List<String>
)
