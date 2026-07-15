package com.replog.domain.visual.spec

/**
 * Immutable specification detailing the primary, secondary, and stabiliser muscle groups
 * targeted by an exercise for anatomical highlighting.
 */
data class AnatomySpec(
    val primaryMuscles: Set<String> = emptySet(),
    val secondaryMuscles: Set<String> = emptySet(),
    val stabiliserMuscles: Set<String> = emptySet()
)
