package com.replog.domain.visual.validation

import com.replog.domain.visual.anatomy.BackBody
import com.replog.domain.visual.anatomy.BodySide
import com.replog.domain.visual.anatomy.FrontBody
import com.replog.domain.visual.anatomy.MuscleMap
import com.replog.domain.visual.anatomy.MuscleRegion
import com.replog.domain.visual.spec.AnatomySpec

data class AnatomyValidationReport(
    val totalRegionsDefined: Int,
    val totalFrontRegions: Int,
    val totalBackRegions: Int,
    val orphanRegions: List<MuscleRegion>,
    val unmappedStrings: List<String>,
    val allRegionsHaveGeometry: Boolean
)

/**
 * Validator auditing the anatomical rendering system for complete vector geometry,
 * non-overlapping primary/secondary regions, and zero orphan definitions.
 */
object AnatomyValidator {

    fun validateAnatomySystem(sampleMuscleStrings: List<String> = emptyList()): AnatomyValidationReport {
        val allEntries = MuscleRegion.entries
        val frontMapKeys = FrontBody.regionPaths.keys
        val backMapKeys = BackBody.regionPaths.keys

        val orphans = mutableListOf<MuscleRegion>()
        for (region in allEntries) {
            if (region.side == BodySide.FRONT && !frontMapKeys.contains(region)) {
                orphans.add(region)
            } else if (region.side == BodySide.BACK && !backMapKeys.contains(region)) {
                orphans.add(region)
            }
        }

        val unmapped = mutableListOf<String>()
        for (str in sampleMuscleStrings) {
            if (MuscleMap.resolveRegions(str).isEmpty()) {
                unmapped.add(str)
            }
        }

        return AnatomyValidationReport(
            totalRegionsDefined = allEntries.size,
            totalFrontRegions = frontMapKeys.size,
            totalBackRegions = backMapKeys.size,
            orphanRegions = orphans,
            unmappedStrings = unmapped,
            allRegionsHaveGeometry = orphans.isEmpty() && frontMapKeys.size + backMapKeys.size == allEntries.size
        )
    }

    fun verifySpecConsistency(spec: AnatomySpec): Boolean {
        val primary = MuscleMap.mapPrimary(spec)
        val secondary = MuscleMap.mapSecondary(spec)
        // Primary and secondary must be mutually exclusive in rendering
        return (primary.intersect(secondary)).isEmpty()
    }
}
