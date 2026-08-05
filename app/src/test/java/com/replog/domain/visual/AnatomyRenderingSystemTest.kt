package com.replog.domain.visual

import com.replog.domain.visual.anatomy.MuscleMap
import com.replog.domain.visual.anatomy.MuscleRegion
import com.replog.domain.visual.spec.AnatomySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnatomyRenderingSystemTest {

    @Test
    fun testMuscleRegionTaxonomyStable() {
        // The MuscleRegion taxonomy drives MuscleMap, MuscleActivationEngine and the
        // accessibility description of the premium anatomy artwork. Keep this count
        // aligned with the enum until the taxonomy changes deliberately.
        assertEquals("Current anatomy taxonomy must remain stable", 43, MuscleRegion.entries.size)
    }

    @Test
    fun testMuscleMapResolvesStandardStrings() {
        val chestRegions = MuscleMap.resolveRegions("Upper Chest, Pectorals")
        assertTrue(chestRegions.contains(MuscleRegion.UPPER_CHEST))
        assertTrue(chestRegions.contains(MuscleRegion.CHEST))

        val backRegions = MuscleMap.resolveRegions("Latissimus Dorsi, Traps, Lower Back")
        assertTrue(backRegions.contains(MuscleRegion.LATISSIMUS_DORSI))
        assertTrue(backRegions.contains(MuscleRegion.UPPER_TRAPEZIUS))
        assertTrue(backRegions.contains(MuscleRegion.SPINAL_ERECTORS))
    }

    @Test
    fun testAnatomySpecPrimaryAndSecondaryExclusivity() {
        val spec = AnatomySpec(
            primaryMuscles = setOf("Quadriceps", "Glutes"),
            secondaryMuscles = setOf("Hamstrings", "Glutes", "Calves")
        )
        val primary = MuscleMap.mapPrimary(spec)
        val secondary = MuscleMap.mapSecondary(spec)
        assertTrue(primary.contains(MuscleRegion.QUADRICEPS))
        assertTrue(primary.contains(MuscleRegion.GLUTE_MAXIMUS))
        // Glutes was in primary, so it should be stripped from secondary
        assertTrue(!secondary.contains(MuscleRegion.GLUTE_MAXIMUS))
        assertTrue(secondary.contains(MuscleRegion.HAMSTRINGS))
        assertTrue(secondary.contains(MuscleRegion.CALVES))
    }
}
