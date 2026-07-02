package com.replog.domain.library

import com.replog.data.model.Exercise

/**
 * Maps an exercise's muscle strings to a small fixed set of body REGIONS that the
 * SVG body diagram can highlight (Phase 4). Pure and offline; the diagram itself
 * is drawn in code (no image assets).
 */
enum class MuscleRegion(val side: Side) {
    CHEST(Side.FRONT), FRONT_DELTS(Side.FRONT), SIDE_DELTS(Side.FRONT), BICEPS(Side.FRONT), FOREARMS(Side.FRONT),
    ABS(Side.FRONT), OBLIQUES(Side.FRONT), QUADS(Side.FRONT), INNER_THIGHS(Side.FRONT), OUTER_HIPS(Side.FRONT),
    UPPER_BACK(Side.BACK), LATS(Side.BACK), REAR_DELTS(Side.BACK), TRICEPS(Side.BACK),
    TRAPS(Side.BACK), LOWER_BACK(Side.BACK), GLUTES(Side.BACK), HAMSTRINGS(Side.BACK),
    CALVES(Side.BACK);

    enum class Side { FRONT, BACK }
}

object MuscleMap {

    /**
     * Ordered, SPECIFIC-FIRST keyword -> region map. The first matching keyword for
     * a given muscle phrase wins (we match per-muscle, not per-whole-string), so
     * "Side Deltoids" maps to SIDE_DELTS and never also to FRONT_DELTS. This fixed
     * the Sprint 13 bug where lateral raises highlighted the front shoulders.
     */
    private val keywordToRegion: List<Pair<String, MuscleRegion>> = listOf(
        "pectoral" to MuscleRegion.CHEST, "chest" to MuscleRegion.CHEST,
        "front delt" to MuscleRegion.FRONT_DELTS,
        "side delt" to MuscleRegion.SIDE_DELTS, "lateral delt" to MuscleRegion.SIDE_DELTS,
        "rear delt" to MuscleRegion.REAR_DELTS,
        "deltoid" to MuscleRegion.FRONT_DELTS, "shoulder" to MuscleRegion.FRONT_DELTS,
        "bicep" to MuscleRegion.BICEPS,
        "tricep" to MuscleRegion.TRICEPS,
        "forearm" to MuscleRegion.FOREARMS,
        "oblique" to MuscleRegion.OBLIQUES,
        "abdom" to MuscleRegion.ABS, "abs" to MuscleRegion.ABS, "core" to MuscleRegion.ABS,
        "adductor" to MuscleRegion.INNER_THIGHS,
        "abductor" to MuscleRegion.OUTER_HIPS,
        "quad" to MuscleRegion.QUADS,
        "hamstring" to MuscleRegion.HAMSTRINGS,
        "glute" to MuscleRegion.GLUTES,
        "calf" to MuscleRegion.CALVES, "calves" to MuscleRegion.CALVES,
        "lat" to MuscleRegion.LATS,
        "trap" to MuscleRegion.TRAPS,
        "lower back" to MuscleRegion.LOWER_BACK, "erector" to MuscleRegion.LOWER_BACK,
        "upper back" to MuscleRegion.UPPER_BACK, "back" to MuscleRegion.UPPER_BACK
    )

    /** Map ONE muscle name to its single best region (first specific match wins). */
    private fun regionForMuscle(muscle: String): MuscleRegion? {
        val m = muscle.trim().lowercase()
        if (m.isEmpty()) return null
        for ((kw, region) in keywordToRegion) if (m.contains(kw)) return region
        return null
    }

    private fun regionsFor(csv: String): Set<MuscleRegion> {
        val out = LinkedHashSet<MuscleRegion>()
        for (muscle in csv.split(",")) {
            regionForMuscle(muscle)?.let { out.add(it) }
        }
        return out
    }

    /**
     * Regions highlighted as PRIMARY for this exercise. Falls back to a sensible
     * whole-body highlight for cardio / full-body movements that have no single
     * target muscle, so the diagram always shows something meaningful.
     */
    fun primaryRegions(ex: Exercise): Set<MuscleRegion> {
        val mapped = regionsFor(ex.primaryMuscles.ifBlank { ex.muscles.split(",").take(1).joinToString(",") })
        // Loaded carries are a whole-body brace: the named primary (often forearms)
        // undersells them, so also highlight the core and upper back.
        if (ExercisePattern.of(ex) == ExercisePattern.CORE_CARRY) {
            return mapped + setOf(MuscleRegion.ABS, MuscleRegion.UPPER_BACK)
        }
        if (mapped.isNotEmpty()) return mapped
        // Cardio / full-body / Olympic lifts: highlight the major movers.
        return setOf(MuscleRegion.QUADS, MuscleRegion.GLUTES, MuscleRegion.UPPER_BACK)
    }

    /** Regions highlighted as SECONDARY (excludes any already primary). */
    fun secondaryRegions(ex: Exercise): Set<MuscleRegion> {
        val primary = primaryRegions(ex)
        return regionsFor(ex.secondaryMuscles) - primary
    }
}
