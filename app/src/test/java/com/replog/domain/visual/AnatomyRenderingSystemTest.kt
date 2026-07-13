package com.replog.domain.visual

import com.replog.domain.visual.anatomy.BackBody
import com.replog.domain.visual.anatomy.BodySide
import com.replog.domain.visual.anatomy.FrontBody
import com.replog.domain.visual.anatomy.MuscleMap
import com.replog.domain.visual.anatomy.MuscleRegion
import com.replog.domain.visual.anatomy.VectorBody
import com.replog.domain.visual.spec.AnatomySpec
import com.replog.domain.visual.validation.AnatomyValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnatomyRenderingSystemTest {

    @Test
    fun testAllMuscleRegionsHaveVectorGeometry() {
        val report = AnatomyValidator.validateAnatomySystem()
        assertEquals("Total regions defined must equal sum of front and back geometry maps", 27, report.totalRegionsDefined)
        assertTrue("Zero orphan regions should exist", report.orphanRegions.isEmpty())
        assertTrue("All regions must have pre-compiled geometry paths", report.allRegionsHaveGeometry)
    }

    @Test
    fun testVectorBodiesContainSilhouettesAndRegions() {
        assertNotNull(VectorBody.FRONT.silhouettePath)
        assertNotNull(VectorBody.BACK.silhouettePath)
        assertEquals(BodySide.FRONT, VectorBody.FRONT.side)
        assertEquals(BodySide.BACK, VectorBody.BACK.side)
        assertTrue(VectorBody.FRONT.regionPaths.isNotEmpty())
        assertTrue(VectorBody.BACK.regionPaths.isNotEmpty())
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
        assertTrue(AnatomyValidator.verifySpecConsistency(spec))
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
