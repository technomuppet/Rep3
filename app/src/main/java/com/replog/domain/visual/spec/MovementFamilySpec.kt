package com.replog.domain.visual.spec

/**
 * Immutable specification identifying the kinematic movement family and any
 * parametric adjustments for rendering the exercise demonstration.
 */
data class MovementFamilySpec(
    val familyId: String = "GENERIC_UNMAPPED",
    val parameters: Map<String, Float> = emptyMap()
)
