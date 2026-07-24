package com.replog.domain.visual.anatomy

import com.replog.domain.visual.anatomy.Muscles

import com.replog.domain.visual.spec.AnatomySpec

/**
 * Deterministic mapper translating raw muscle names from AnatomySpec into
 * standardized MuscleRegion enum sets for anatomical vector rendering.
 */
object MuscleMap {

private val canonicalMappings = mapOf(

    Muscles.CHEST.lowercase() to setOf(MuscleRegion.CHEST),
    Muscles.UPPER_CHEST.lowercase() to setOf(MuscleRegion.UPPER_CHEST),

    Muscles.LATISSIMUS_DORSI.lowercase() to setOf(MuscleRegion.LATISSIMUS_DORSI),
    Muscles.TERES_MAJOR.lowercase() to setOf(MuscleRegion.TERES_MAJOR),
    Muscles.RHOMBOIDS.lowercase() to setOf(MuscleRegion.RHOMBOIDS),

    Muscles.UPPER_TRAPEZIUS.lowercase() to setOf(MuscleRegion.UPPER_TRAPEZIUS),
    Muscles.MIDDLE_TRAPEZIUS.lowercase() to setOf(MuscleRegion.MIDDLE_TRAPEZIUS),

    Muscles.SPINAL_ERECTORS.lowercase() to setOf(MuscleRegion.SPINAL_ERECTORS),

    Muscles.ANTERIOR_DELTOID.lowercase() to setOf(MuscleRegion.ANTERIOR_DELTOID),
    Muscles.LATERAL_DELTOID.lowercase() to setOf(MuscleRegion.LATERAL_DELTOID),
    Muscles.POSTERIOR_DELTOID.lowercase() to setOf(MuscleRegion.POSTERIOR_DELTOID),

    Muscles.BICEPS.lowercase() to setOf(MuscleRegion.BICEPS),
    Muscles.TRICEPS.lowercase() to setOf(MuscleRegion.TRICEPS),
    Muscles.FOREARMS.lowercase() to setOf(
        MuscleRegion.FOREARMS_ANTERIOR,
        MuscleRegion.FOREARMS_POSTERIOR
    ),

    Muscles.RECTUS_ABDOMINIS.lowercase() to setOf(MuscleRegion.RECTUS_ABDOMINIS),
    Muscles.OBLIQUES.lowercase() to setOf(MuscleRegion.OBLIQUES),
    Muscles.SERRATUS_ANTERIOR.lowercase() to setOf(MuscleRegion.SERRATUS_ANTERIOR),
    Muscles.HIP_FLEXORS.lowercase() to setOf(MuscleRegion.HIP_FLEXORS),

    Muscles.GLUTE_MAXIMUS.lowercase() to setOf(MuscleRegion.GLUTE_MAXIMUS),
    Muscles.QUADRICEPS.lowercase() to setOf(MuscleRegion.QUADRICEPS),
    Muscles.HAMSTRINGS.lowercase() to setOf(MuscleRegion.HAMSTRINGS),
    Muscles.ADDUCTORS.lowercase() to setOf(MuscleRegion.ADDUCTORS),

    Muscles.CALVES.lowercase() to setOf(MuscleRegion.CALVES),
    Muscles.TIBIALIS_ANTERIOR.lowercase() to setOf(MuscleRegion.TIBIALIS_ANTERIOR)
)
    private val keywordMappings: List<Pair<String, Set<MuscleRegion>>> = listOf(
        // Upper Chest specific matches first
        "upper chest" to setOf(MuscleRegion.UPPER_CHEST),
        "clavicular" to setOf(MuscleRegion.UPPER_CHEST),
        "chest" to setOf(MuscleRegion.CHEST),
        "middle chest" to setOf(MuscleRegion.MIDDLE_CHEST),
        "lower chest" to setOf(MuscleRegion.LOWER_CHEST),
        "pectoral" to setOf(MuscleRegion.CHEST),
        "pec" to setOf(MuscleRegion.CHEST),

        // Deltoids
        "front delt" to setOf(MuscleRegion.ANTERIOR_DELTOID),
        "anterior delt" to setOf(MuscleRegion.ANTERIOR_DELTOID),
        "side delt" to setOf(MuscleRegion.LATERAL_DELTOID),
        "lateral delt" to setOf(MuscleRegion.LATERAL_DELTOID),
        "rear delt" to setOf(MuscleRegion.POSTERIOR_DELTOID),
        "posterior delt" to setOf(MuscleRegion.POSTERIOR_DELTOID),
        "shoulder" to setOf(MuscleRegion.ANTERIOR_DELTOID, MuscleRegion.LATERAL_DELTOID),
        "deltoid" to setOf(MuscleRegion.ANTERIOR_DELTOID, MuscleRegion.LATERAL_DELTOID),

        // Arms
        "bicep" to setOf(MuscleRegion.BICEPS),
        "brachialis" to setOf(MuscleRegion.BRACHIALIS),
        "tricep" to setOf(MuscleRegion.TRICEPS),
        "forearm" to setOf(MuscleRegion.FOREARMS_ANTERIOR, MuscleRegion.FOREARMS_POSTERIOR),
        "brachioradialis" to setOf(MuscleRegion.FOREARMS_ANTERIOR),
        "wrist" to setOf(MuscleRegion.FOREARMS_ANTERIOR),

        // Back & Traps
        "upper trap" to setOf(MuscleRegion.UPPER_TRAPEZIUS),
        "lower trap" to setOf(MuscleRegion.LOWER_TRAPEZIUS),
        "middle trap" to setOf(MuscleRegion.MIDDLE_TRAPEZIUS),
        "trap" to setOf(MuscleRegion.UPPER_TRAPEZIUS, MuscleRegion.MIDDLE_TRAPEZIUS),
        "lat" to setOf(MuscleRegion.LATISSIMUS_DORSI),
        "rhomboid" to setOf(MuscleRegion.RHOMBOIDS),
        "teres" to setOf(MuscleRegion.TERES_MAJOR),
        "upper back" to setOf(MuscleRegion.RHOMBOIDS, MuscleRegion.MIDDLE_TRAPEZIUS, MuscleRegion.LATISSIMUS_DORSI),
        "lower back" to setOf(MuscleRegion.SPINAL_ERECTORS),
        "erector" to setOf(MuscleRegion.SPINAL_ERECTORS),
        "back" to setOf(MuscleRegion.LATISSIMUS_DORSI, MuscleRegion.RHOMBOIDS),

        // Core
        "oblique" to setOf(MuscleRegion.OBLIQUES),
        "serratus" to setOf(MuscleRegion.SERRATUS_ANTERIOR),
        "abdom" to setOf(MuscleRegion.RECTUS_ABDOMINIS),
        "abs" to setOf(MuscleRegion.RECTUS_ABDOMINIS),
        "core" to setOf(MuscleRegion.RECTUS_ABDOMINIS, MuscleRegion.OBLIQUES, MuscleRegion.TRANSVERSE_ABDOMINIS),
        "transverse abdominis" to setOf(MuscleRegion.TRANSVERSE_ABDOMINIS),

        // Hips & Glutes
        "hip flexor" to setOf(MuscleRegion.HIP_FLEXORS),
        "psoas" to setOf(MuscleRegion.HIP_FLEXORS),
        "glute medius" to setOf(MuscleRegion.GLUTE_MEDIUS),
        "glute" to setOf(MuscleRegion.GLUTE_MAXIMUS),

        // Legs
        "quad" to setOf(MuscleRegion.QUADRICEPS),
        "hamstring" to setOf(MuscleRegion.HAMSTRINGS),
        "adductor" to setOf(MuscleRegion.ADDUCTORS),
        "inner thigh" to setOf(MuscleRegion.ADDUCTORS),
        "abductor" to setOf(MuscleRegion.ABDUCTORS),
        "outer thigh" to setOf(MuscleRegion.ABDUCTORS),
        "outer hip" to setOf(MuscleRegion.GLUTE_MEDIUS, MuscleRegion.ABDUCTORS),
        "calf" to setOf(MuscleRegion.CALVES),
        "calves" to setOf(MuscleRegion.CALVES),
        "soleus" to setOf(MuscleRegion.CALVES),
        "tibialis" to setOf(MuscleRegion.TIBIALIS_ANTERIOR),
        "shin" to setOf(MuscleRegion.TIBIALIS_ANTERIOR)
    )

    fun mapPrimary(spec: AnatomySpec): Set<MuscleRegion> {
        val out = mutableSetOf<MuscleRegion>()
        for (name in spec.primaryMuscles) {
            out.addAll(resolveRegions(name))
        }
        return out
    }

    fun mapSecondary(spec: AnatomySpec): Set<MuscleRegion> {
        val primary = mapPrimary(spec)
        val out = mutableSetOf<MuscleRegion>()
        for (name in spec.secondaryMuscles) {
            out.addAll(resolveRegions(name))
        }
        return out - primary
    }

    fun resolveRegions(muscleName: String): Set<MuscleRegion> {
        val m = muscleName.trim().lowercase()
        canonicalMappings[m]?.let { regions ->

    for (region in regions) {
        AnatomyDiagnostics.auditMatch(
            muscleName,
            region.name,
            region.name,
            true
        )
    }

    return regions
}
        if (m.isEmpty()) {
            AnatomyDiagnostics.auditEnumConversion(muscleName)
            return emptySet()
        }
        val matched = mutableSetOf<MuscleRegion>()
        for ((kw, regions) in keywordMappings) {
            if (m.contains(kw)) {
                matched.addAll(regions)
                // Audit each matched region independently
                for (region in regions) {
                    AnatomyDiagnostics.auditMatch(muscleName, region.name, region.name, true)
                }
            }
        }
        // Audit requested muscle: if no direct keyword match, try valueOf as fallback
        if (matched.isEmpty()) {
            val enumRegion = AnatomyDiagnostics.auditEnumConversion(muscleName)
            if (enumRegion != null) {
                matched.add(enumRegion)
                AnatomyDiagnostics.auditMatch(muscleName, enumRegion.name, enumRegion.name, true)
            } else {
                AnatomyDiagnostics.auditMatch(muscleName, null, null, false)
            }
        }
        return matched
    }
}
