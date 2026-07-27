package com.replog.domain.visual.anatomy

/**
 * Diagnostics for the layered SVG renderer.
 *
 * Kept intentionally lightweight and safe for JVM tests: no Android-only logging
 * APIs are required. Production builds can consume the in-memory counters and
 * Logcat still receives println output in debug runs.
 */
object RendererDiagnostics {
    const val TAG: String = "ANATOMY_RENDERER"
    const val ENABLED: Boolean = false

    data class Snapshot(
        val missingAssets: Map<String, Int>,
        val malformedAssets: Map<String, Int>,
        val renderFailures: Map<String, Int>,
        val missingRegionMappings: Map<String, Int>,
        val duplicateMappingEvents: Int,
        val cacheHits: Int,
        val cacheMisses: Int,
        val cacheEvictions: Int,
        val emptyActivationLists: Int
    )

    private val missingAssets = linkedMapOf<String, Int>()
    private val malformedAssets = linkedMapOf<String, Int>()
    private val renderFailures = linkedMapOf<String, Int>()
    private val missingRegionMappings = linkedMapOf<String, Int>()
    private var duplicateMappingEvents = 0
    private var cacheHits = 0
    private var cacheMisses = 0
    private var cacheEvictions = 0
    private var emptyActivationLists = 0

    @Synchronized fun reset() {
        missingAssets.clear()
        malformedAssets.clear()
        renderFailures.clear()
        missingRegionMappings.clear()
        duplicateMappingEvents = 0
        cacheHits = 0
        cacheMisses = 0
        cacheEvictions = 0
        emptyActivationLists = 0
    }

    @Synchronized fun snapshot(): Snapshot = Snapshot(
        missingAssets = missingAssets.toMap(),
        malformedAssets = malformedAssets.toMap(),
        renderFailures = renderFailures.toMap(),
        missingRegionMappings = missingRegionMappings.toMap(),
        duplicateMappingEvents = duplicateMappingEvents,
        cacheHits = cacheHits,
        cacheMisses = cacheMisses,
        cacheEvictions = cacheEvictions,
        emptyActivationLists = emptyActivationLists
    )

    @Synchronized fun missingSvgAsset(assetPath: String, reason: String? = null) {
        increment(missingAssets, assetPath)
        log("MISSING_ASSET", "$assetPath${reason?.let { " | $it" }.orEmpty()}")
    }

    @Synchronized fun malformedSvgAsset(assetPath: String, reason: String? = null) {
        increment(malformedAssets, assetPath)
        log("MALFORMED_ASSET", "$assetPath${reason?.let { " | $it" }.orEmpty()}")
    }

    @Synchronized fun renderFailure(assetPath: String, reason: String? = null) {
        increment(renderFailures, assetPath)
        log("RENDER_FAILURE", "$assetPath${reason?.let { " | $it" }.orEmpty()}")
    }

    @Synchronized fun missingMuscleRegionMapping(region: MuscleRegion) {
        increment(missingRegionMappings, region.name)
        log("MISSING_REGION_MAPPING", region.name)
    }

    @Synchronized fun cacheHit(assetPath: String) {
        cacheHits++
        log("CACHE_HIT", assetPath)
    }

    @Synchronized fun cacheMiss(assetPath: String) {
        cacheMisses++
        log("CACHE_MISS", assetPath)
    }

    @Synchronized fun cacheEviction(assetPath: String) {
        cacheEvictions++
        log("CACHE_EVICTION", assetPath)
    }

    @Synchronized fun emptyActivationList(side: BodySide) {
        emptyActivationLists++
        log("EMPTY_ACTIVATION_LIST", side.name)
    }

    @Synchronized private fun increment(map: MutableMap<String, Int>, key: String) {
        map[key] = (map[key] ?: 0) + 1
    }

    private fun log(event: String, message: String) {
        if (ENABLED) println("$TAG [$event] $message")
    }
    @Synchronized
    fun duplicatePngMappings(
        duplicates: Map<MuscleRegion, List<PngAnatomyLayerAsset>>
    ) {
        if (duplicates.isEmpty()) return
        duplicateMappingEvents += duplicates.size
        val summary = duplicates.entries.joinToString(separator = ", ") { (region, layers) ->
            "${region.name}[${layers.size}]"
        }
        log("DUPLICATE_PNG_MAPPING", summary)
    }

    @Synchronized
    fun missingPngDrawable(drawableName: String) {
        increment(missingAssets, drawableName)
        log("MISSING_PNG_DRAWABLE", drawableName)
    }

    @Synchronized
    fun pngLayerStack(side: BodySide, layers: List<PngAnatomyLayerAsset>) {
        if (layers.isEmpty()) return
        val byDrawableId = layers.groupingBy { it.drawableId }.eachCount()
        val dupeDrawables = byDrawableId.filter { it.value > 1 }
        if (dupeDrawables.isEmpty()) return
        dupeDrawables.forEach { (id, _) ->
            val drawableName = layers.first { it.drawableId == id }.drawableName
            val key = "${side.name}:stack-duplicate:$drawableName"
            increment(renderFailures, key)
        }
        log("PNG_LAYER_STACK_DUPLICATE", "${side.name}:${dupeDrawables.keys.size}")
    }

}
