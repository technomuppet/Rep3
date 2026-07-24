package com.replog.domain.visual

import com.replog.domain.visual.anatomy.BodySide
import com.replog.domain.visual.anatomy.LayeredAnatomyRenderer
import com.replog.domain.visual.anatomy.MuscleAssetMappingType
import com.replog.domain.visual.anatomy.MuscleAssetRegistry
import com.replog.domain.visual.anatomy.MuscleMap
import com.replog.domain.visual.anatomy.MuscleRegion
import com.replog.domain.visual.anatomy.RendererDiagnostics
import com.replog.domain.visual.anatomy.SVGCache
import com.replog.domain.visual.anatomy.SvgCacheTestResult
import com.replog.domain.visual.spec.AnatomySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AnatomyRenderingSystemTest {

    @Before
    fun reset() {
        RendererDiagnostics.reset()
        SVGCache.clear()
        SVGCache.resetMaxEntries()
    }

    @Test
    fun everyMuscleRegionHasRegistryMapping() {
        val report = MuscleAssetRegistry.validate(availableLayeredAssetPaths())
        assertEquals(MuscleRegion.entries.size, report.totalRegions)
        assertEquals(emptySet<MuscleRegion>(), report.unmappedRegions)
        assertTrue(report.isValid)
    }

    @Test
    fun everyLayeredSvgAssetIsReferencedAndNoOrphansExist() {
        val available = availableLayeredAssetPaths()
        val report = MuscleAssetRegistry.validate(available)

        assertEquals(emptySet<String>(), report.missingAssetPaths)
        assertEquals(emptySet<String>(), report.orphanAssetPaths)
        assertEquals(emptySet<String>(), report.unreferencedExpectedAssetPaths)
        assertEquals(MuscleAssetRegistry.expectedLibraryAssetPaths, MuscleAssetRegistry.allAssetPaths())
    }

    @Test
    fun aliasMappingsAreExplicitAndStable() {
        val rectusFemoris = MuscleAssetRegistry.assetsFor(MuscleRegion.RECTUS_FEMORIS).single()
        assertEquals(MuscleAssetMappingType.ALIAS, rectusFemoris.mappingType)
        assertEquals(MuscleRegion.QUADRICEPS, rectusFemoris.mappedFrom)
        assertEquals("anatomy/front/quads.svg", rectusFemoris.assetPath)

        val gluteMedius = MuscleAssetRegistry.assetsFor(MuscleRegion.GLUTE_MEDIUS).single()
        assertEquals(MuscleAssetMappingType.ALIAS, gluteMedius.mappingType)
        assertEquals(MuscleRegion.GLUTE_MAXIMUS, gluteMedius.mappedFrom)
        assertEquals("anatomy/back/glutes.svg", gluteMedius.assetPath)
    }

    @Test
    fun duplicateMappingsAreReportedAsEmptyForProductionRegistry() {
        assertTrue(MuscleAssetRegistry.duplicateRegionMappings().isEmpty())
        assertEquals(0, RendererDiagnostics.snapshot().duplicateMappingEvents)
    }

    @Test
    fun everyRegisteredAssetExistsInAssetsDirectory() {
        val root = assetsRoot()
        val missing = MuscleAssetRegistry.allAssetPaths()
            .map { File(root, it) }
            .filterNot { it.exists() }
            .map { it.path }

        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun allLayeredAssetsHaveSvgRootElement() {
        val root = assetsRoot()
        val invalid = MuscleAssetRegistry.allAssetPaths().filter { path ->
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(File(root, path))
            document.documentElement.nodeName != "svg"
        }
        assertEquals(emptyList<String>(), invalid)
    }

    @Test
    fun malformedSvgXmlFailsCleanlyInContractParser() {
        val malformed = "<svg width=\"768\" height=\"1536\"><path></svg>"
        val result = runCatching {
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(malformed.toByteArray()))
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun allLayeredAssetsUseSharedCanvasContract() {
        val root = assetsRoot()
        val invalid = MuscleAssetRegistry.allAssetPaths().filter { path ->
            val text = File(root, path).readText()
            !("width=\"768\"" in text && "height=\"1536\"" in text && "viewBox=\"0 0 768 1536\"" in text)
        }
        assertEquals(emptyList<String>(), invalid)
    }

    @Test
    fun overlayAssetsPreserveTransparencyContract() {
        val root = assetsRoot()
        val suspiciousOpaqueBackgrounds = MuscleAssetRegistry.allAssetPaths()
            .filterNot { it.endsWith("_base.svg") }
            .filter { path ->
                val text = File(root, path).readText().lowercase()
                "background" in text || Regex("<rect[^>]+(width=\"768\"|height=\"1536\")").containsMatchIn(text)
            }
        assertEquals(emptyList<String>(), suspiciousOpaqueBackgrounds)
    }

    @Test
    fun cacheRecordsHitsAndMissesAndCachesFailures() {
        var calls = 0
        val first = SVGCache.getOrLoad("test/missing.svg") {
            calls++
            SvgCacheTestResult.Failure
        }
        val second = SVGCache.getOrLoad("test/missing.svg") {
            calls++
            SvgCacheTestResult.Success
        }

        assertFalse(first)
        assertFalse(second)
        assertEquals("Loader should only run once because failures are cached", 1, calls)
        assertEquals(1, RendererDiagnostics.snapshot().cacheMisses)
        assertEquals(1, RendererDiagnostics.snapshot().cacheHits)
    }

    @Test
    fun cacheRecordsHitsAndMissesAndCachesSuccesses() {
        var calls = 0
        val first = SVGCache.getOrLoad("test/success.svg") {
            calls++
            SvgCacheTestResult.Success
        }
        val second = SVGCache.getOrLoad("test/success.svg") {
            calls++
            SvgCacheTestResult.Failure
        }

        assertTrue(first)
        assertTrue(second)
        assertEquals(1, calls)
        assertEquals(1, RendererDiagnostics.snapshot().cacheMisses)
        assertEquals(1, RendererDiagnostics.snapshot().cacheHits)
    }

    @Test
    fun duplicateConcurrentCacheRequestsShareOneLoad() {
        val calls = AtomicInteger(0)
        val start = CountDownLatch(1)
        val done = CountDownLatch(4)
        val results = mutableListOf<Boolean>()

        repeat(4) {
            Thread {
                start.await()
                val result = SVGCache.getOrLoad("shared.svg") {
                    calls.incrementAndGet()
                    Thread.sleep(25)
                    SvgCacheTestResult.Success
                }
                synchronized(results) { results.add(result) }
                done.countDown()
            }.start()
        }

        start.countDown()
        assertTrue(done.await(2, TimeUnit.SECONDS))

        assertEquals(1, calls.get())
        assertEquals(4, results.size)
        assertTrue(results.all { it })
    }

    @Test
    fun cacheIsBoundedByEvictionLimit() {
        SVGCache.configureMaxEntries(2)
        SVGCache.getOrLoad("a.svg") { SvgCacheTestResult.Success }
        SVGCache.getOrLoad("b.svg") { SvgCacheTestResult.Success }
        SVGCache.getOrLoad("c.svg") { SvgCacheTestResult.Success }

        assertEquals(2, SVGCache.size())
        assertFalse(SVGCache.contains("a.svg"))
        assertTrue(SVGCache.contains("b.svg"))
        assertTrue(SVGCache.contains("c.svg"))
        assertEquals(1, RendererDiagnostics.snapshot().cacheEvictions)
    }

    @Test
    fun missingAndMalformedAssetsAreRecordedGracefully() {
        RendererDiagnostics.missingSvgAsset("anatomy/front/missing.svg", "test")
        RendererDiagnostics.malformedSvgAsset("anatomy/front/malformed.svg", "test")

        val snapshot = RendererDiagnostics.snapshot()
        assertEquals(1, snapshot.missingAssets["anatomy/front/missing.svg"])
        assertEquals(1, snapshot.malformedAssets["anatomy/front/malformed.svg"])
    }

    @Test
    fun gracefulFailureForEmptyActivationLists() {
        val frontLayers = MuscleAssetRegistry.overlayAssetsFor(BodySide.FRONT, emptySet())
        val backLayers = MuscleAssetRegistry.overlayAssetsFor(BodySide.BACK, emptySet())

        assertTrue(frontLayers.isEmpty())
        assertTrue(backLayers.isEmpty())
        assertEquals(2, RendererDiagnostics.snapshot().emptyActivationLists)
    }

    @Test
    fun deterministicLayerOrderingAndInactiveMusclesAreOmitted() {
        val active = setOf(
            MuscleRegion.BICEPS,
            MuscleRegion.CHEST,
            MuscleRegion.QUADRICEPS,
            MuscleRegion.UPPER_CHEST
        )
        val front = MuscleAssetRegistry.overlayAssetsFor(BodySide.FRONT, active)

        assertEquals(
            listOf(
                "anatomy/front/upper_chest.svg",
                "anatomy/front/chest.svg",
                "anatomy/front/biceps.svg",
                "anatomy/front/quads.svg"
            ),
            front.map { it.assetPath }
        )
        assertFalse(front.map { it.assetPath }.contains("anatomy/front/abs.svg"))
    }

    @Test
    fun duplicateOverlayRequestsAreDeduplicated() {
        val active = setOf(
            MuscleRegion.QUADRICEPS,
            MuscleRegion.RECTUS_FEMORIS,
            MuscleRegion.VASTUS_LATERALIS,
            MuscleRegion.VASTUS_MEDIALIS
        )
        val front = MuscleAssetRegistry.overlayAssetsFor(BodySide.FRONT, active)
        assertEquals(listOf("anatomy/front/quads.svg"), front.map { it.assetPath })
        assertEquals(MuscleRegion.QUADRICEPS, front.single().region)
    }

    @Test
    fun layerStacksAreStableAcrossRepeatedRecompositionEquivalentCalls() {
        val active = setOf(MuscleRegion.CHEST, MuscleRegion.TRICEPS, MuscleRegion.BICEPS)
        val first = LayeredAnatomyRenderer.layersFor(BodySide.FRONT, active).map { it.assetPath }
        repeat(50) {
            assertEquals(first, LayeredAnatomyRenderer.layersFor(BodySide.FRONT, active).map { it.assetPath })
        }
    }

    @Test
    fun muscleMapResolvesStandardStringsIntoLayeredRegions() {
        val chestRegions = MuscleMap.resolveRegions("Upper Chest, Pectorals")
        assertTrue(chestRegions.contains(MuscleRegion.UPPER_CHEST))
        assertTrue(chestRegions.contains(MuscleRegion.CHEST))

        assertEquals(setOf(MuscleRegion.BRACHIALIS), MuscleMap.resolveRegions("Brachialis"))

        val spec = AnatomySpec(
            primaryMuscles = setOf("Quadriceps", "Glutes"),
            secondaryMuscles = setOf("Hamstrings", "Glutes", "Calves")
        )
        val primary = MuscleMap.mapPrimary(spec)
        val secondary = MuscleMap.mapSecondary(spec)
        assertTrue(primary.contains(MuscleRegion.QUADRICEPS))
        assertTrue(primary.contains(MuscleRegion.GLUTE_MAXIMUS))
        assertFalse(secondary.contains(MuscleRegion.GLUTE_MAXIMUS))
        assertTrue(secondary.contains(MuscleRegion.HAMSTRINGS))
        assertTrue(secondary.contains(MuscleRegion.CALVES))
    }

    @Test
    fun unknownMuscleNamesResolveGracefully() {
        assertTrue(MuscleMap.resolveRegions("Not A Real Muscle").isEmpty())
    }

    private fun assetsRoot(): File {
        val candidateRoots = listOf(File("src/main/assets"), File("app/src/main/assets"))
        return candidateRoots.firstOrNull { it.exists() } ?: candidateRoots.first()
    }

    private fun availableLayeredAssetPaths(): Set<String> {
        val root = assetsRoot()
        return File(root, "anatomy").walkTopDown()
            .filter { it.isFile && it.extension == "svg" }
            .map { it.relativeTo(root).path.replace(File.separatorChar, '/') }
            .filter { it.startsWith("anatomy/front/") || it.startsWith("anatomy/back/") }
            .toSet()
    }
}
