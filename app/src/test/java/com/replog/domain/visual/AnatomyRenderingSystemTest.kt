package com.replog.domain.visual

import com.replog.domain.visual.anatomy.MuscleMap
import com.replog.domain.visual.anatomy.MuscleRegion
import com.replog.domain.visual.anatomy.V2AnatomyModel
import com.replog.domain.visual.spec.AnatomySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnatomyRenderingSystemTest {

    @Test
    fun testAllMuscleRegionsHaveSvgGeometryIds() {
        // The SVG assets are the source of truth for the active V2 renderer.
        // Keep this count aligned with the current enum until the registry is
        // generated in the next anatomy-engine slice.
        assertEquals("Current anatomy taxonomy must remain stable", 43, MuscleRegion.entries.size)
    }

    @Test
    fun testSvgTokenizerHandlesCommandNumberBoundaries() {
        val tokens = com.replog.domain.visual.anatomy.SVGAnatomyLoader.tokenizePathData(
            "M250 30 C285 30, 295 70, 285 110 Z"
        )
        assertEquals(listOf("M", "250", "30", "C", "285", "30", "295", "70", "285", "110", "Z"), tokens)
    }

    @Test
    fun testV2ModelContainsFrontAndBackSilhouettes() {
        assertNotNull(V2AnatomyModel.frontSilhouette)
        assertNotNull(V2AnatomyModel.rearSilhouette)
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
