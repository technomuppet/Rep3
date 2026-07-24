package com.replog.domain.visual.anatomy

/**
 * Compatibility facade for older anatomy diagnostics call sites.
 *
 * Path parsing diagnostics have been retired. New renderer events are recorded by
 * [RendererDiagnostics]. These methods remain lightweight so existing mapping and
 * presentation code can continue to call them without Android-specific logging or
 * file I/O side effects.
 */
object AnatomyDiagnostics {
    const val TAG: String = RendererDiagnostics.TAG
    const val ENABLED: Boolean = RendererDiagnostics.ENABLED
    const val DEBUG_RENDER_MODE: Boolean = false

    fun auditExerciseData(
        exerciseName: String,
        primaryMuscles: List<String>,
        secondaryMuscles: List<String>,
        stabiliserMuscles: List<String>
    ) {
        if (!ENABLED) return
        if (primaryMuscles.isEmpty() && secondaryMuscles.isEmpty() && stabiliserMuscles.isEmpty()) {
            RendererDiagnostics.emptyActivationList(BodySide.FRONT)
            RendererDiagnostics.emptyActivationList(BodySide.BACK)
        }
        println("$TAG [EXERCISE] $exerciseName | primary=$primaryMuscles secondary=$secondaryMuscles stabilisers=$stabiliserMuscles")
    }

    fun auditEnumConversion(original: String): MuscleRegion? {
        val lookup = original.trim().uppercase()
        if (lookup.isEmpty()) return null
        return runCatching { MuscleRegion.valueOf(lookup) }.getOrNull()
    }

    fun auditMatch(requestedName: String, enumName: String?, regionId: String?, matched: Boolean) {
        if (!matched && ENABLED) println("$TAG [MAPPING_MISS] $requestedName -> enum=$enumName region=$regionId")
    }

    fun finalizeSession(verdict: String) {
        if (ENABLED) println("$TAG [SESSION] $verdict")
    }

    fun auditSvgLoad(frontLoaded: Boolean, backLoaded: Boolean, regions: List<String>, exceptionMsg: String = "") {
        if (exceptionMsg.isNotEmpty()) RendererDiagnostics.renderFailure("legacy-svg-load", exceptionMsg)
    }

    fun auditSvgLoad(
        assetName: String,
        expectedIds: Int,
        loadedIds: Int,
        missingIds: List<String>,
        exceptionMsg: String = ""
    ) {
        @Suppress("UNUSED_PARAMETER") val ignoredExpected = expectedIds
        @Suppress("UNUSED_PARAMETER") val ignoredLoaded = loadedIds
        missingIds.forEach { RendererDiagnostics.missingSvgAsset("$assetName:$it") }
        if (exceptionMsg.isNotEmpty()) RendererDiagnostics.renderFailure(assetName, exceptionMsg)
    }

    fun auditSvgMissingIds(missing: List<String>) {
        missing.forEach { RendererDiagnostics.missingSvgAsset(it) }
    }

    fun auditRendererInput(
        primaryRegions: Set<MuscleRegion>,
        secondaryRegions: Set<MuscleRegion>,
        stabiliserRegions: Set<MuscleRegion>,
        totalRegions: Int
    ) {
        @Suppress("UNUSED_PARAMETER") val ignored = listOf(primaryRegions, secondaryRegions, stabiliserRegions, totalRegions)
    }

    fun auditDrawStatistics(
        silhouetteCalls: Int,
        primaryCalls: Int,
        secondaryCalls: Int,
        stabiliserCalls: Int,
        inactiveCalls: Int
    ) {
        @Suppress("UNUSED_PARAMETER") val ignored = listOf(silhouetteCalls, primaryCalls, secondaryCalls, stabiliserCalls, inactiveCalls)
    }

    fun auditPath(
        regionName: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        width: Float,
        height: Float,
        area: Float,
        isEmpty: Boolean,
        valid: Boolean,
        drawn: Boolean
    ) {
        @Suppress("UNUSED_PARAMETER") val ignored = listOf(left, top, right, bottom, width, height, area, isEmpty, drawn)
        if (!valid) RendererDiagnostics.renderFailure(regionName, "Invalid legacy path")
    }

    fun logStage(stageName: String, exerciseName: String, value: Any?) {}
    fun logPipelineStage(exerciseName: String, stage: String, input: String, output: String, count: Int, status: String) {}
    fun logDrawCall(regionId: String?, muscleRegion: String?, activationCategory: String?, colourArgb: String?, alpha: Float, strokeWidth: Float?, drawOrder: Int, layer: String?) {}
    fun logSVGLoad(assetName: String, expectedIds: Int, loadedIds: Int, missingIds: List<String>, unexpectedIds: List<String>) {
        missingIds.forEach { RendererDiagnostics.missingSvgAsset("$assetName:$it") }
        unexpectedIds.forEach { RendererDiagnostics.renderFailure(assetName, "Unexpected legacy id $it") }
    }
    fun logPathBounds(regionName: String, left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float, area: Float, isEmpty: Boolean, valid: Boolean) {
        if (!valid || isEmpty) RendererDiagnostics.renderFailure(regionName, "Invalid legacy path bounds")
    }
    fun logColour(regionName: String, activationType: String?, expectedArgb: String?, actualArgb: String?, alpha: Float, isVisible: Boolean) {}
    fun logActivationSet(exerciseName: String, primaryCount: Int, secondaryCount: Int, stabiliserCount: Int, primaryRegions: List<String>, secondaryRegions: List<String>, stabiliserRegions: List<String>) {}
    fun logDrawStatistics(totalCalls: Int, silhouetteCalls: Int, primaryCalls: Int, secondaryCalls: Int, stabiliserCalls: Int, inactiveCalls: Int) {}
    fun logPipelineSummary(stageName: String, verifiedAtSource: Boolean, verifiedAtRuntime: Boolean, blockerReason: String? = null) {}
}
