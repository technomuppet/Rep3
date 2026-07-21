package com.replog.domain.visual.anatomy

import android.util.Log

/**
 * RC51 — Anatomy Diagnostics Framework (Audit Only — No Fixes)
 * All logging uses TAG = "Anatomy" with exact exercise names,
 * render pass numbers, region IDs, and verified values only.
 * No speculation. No generalizations.
 */
object AnatomyDiagnostics {
    const val TAG: String = "Anatomy"
    const val ENABLED: Boolean = true // Single feature flag per RC51 requirements
    const val DEBUG_RENDER_MODE: Boolean = false // Phase 11 — temporary diagnostic colour mode; easily disabled

    fun logStage(stageName: String, exerciseName: String, value: Any?) {
        if (!ENABLED) return
        Log.d(TAG, "[STAGE] $stageName | Exercise: $exerciseName | Value: $value")
    }

    fun logPipelineStage(
        exerciseName: String,
        stage: String,
        input: String,
        output: String,
        count: Int,
        status: String // PASS / FAIL / UNVERIFIED
    ) {
        if (!ENABLED) return
        Log.d(TAG, "[PIPELINE] $stage | Exercise: $exerciseName | Input: $input | Output: $output | Count: $count | Status: $status")
    }

    fun logDrawCall(
        regionId: String?,
        muscleRegion: String?,
        activationCategory: String?,
        colourArgb: String?,
        alpha: Float,
        strokeWidth: Float?,
        drawOrder: Int,
        layer: String?
    ) {
        if (!ENABLED) return
        Log.d(TAG, "[DRAW] Region: $regionId | Muscle: $muscleRegion | Category: $activationCategory | ARGB: $colourArgb | Alpha: $alpha | Stroke: $strokeWidth | Order: $drawOrder | Layer: $layer")
    }

    fun logSVGLoad(
        assetName: String,
        expectedIds: Int,
        loadedIds: Int,
        missingIds: List<String>,
        unexpectedIds: List<String>
    ) {
        if (!ENABLED) return
        val status = when {
            missingIds.isNotEmpty() || unexpectedIds.isNotEmpty() -> "FAIL"
            loadedIds == expectedIds -> "PASS"
            else -> "UNVERIFIED"
        }
        Log.d(TAG, "[SVG] Asset: $assetName | Expected: $expectedIds | Loaded: $loadedIds | Missing: $missingIds | Unexpected: $unexpectedIds | Status: $status")
    }

    fun logPathBounds(
        regionName: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        width: Float,
        height: Float,
        area: Float,
        isEmpty: Boolean,
        valid: Boolean
    ) {
        if (!ENABLED) return
        val status = if (!valid) "FAIL" else if (isEmpty || width <= 0f || height <= 0f || area <= 0f) "FAIL" else "PASS"
        Log.d(TAG, "[PATH] Region: $regionName | Bounds(LTRB): $left,$top,$right,$bottom | Size(WxH): ${width}x${height} | Area: $area | Empty: $isEmpty | Valid: $valid | Status: $status")
    }

    fun logColour(
        regionName: String,
        activationType: String?,
        expectedArgb: String?,
        actualArgb: String?,
        alpha: Float,
        isVisible: Boolean
    ) {
        if (!ENABLED) return
        val visibleStatus = if (isVisible) "VISIBLE" else "NOT_VISIBLE"
        Log.d(TAG, "[COLOUR] Region: $regionName | Type: $activationType | Expected: $expectedArgb | Actual: $actualArgb | Alpha: $alpha | Visible: $visibleStatus")
    }

    fun logActivationSet(
        exerciseName: String,
        primaryCount: Int,
        secondaryCount: Int,
        stabiliserCount: Int,
        primaryRegions: List<String>,
        secondaryRegions: List<String>,
        stabiliserRegions: List<String>
    ) {
        if (!ENABLED) return
        Log.d(TAG, "[ACTIVATION] Exercise: $exerciseName | Primary: $primaryCount ($primaryRegions) | Secondary: $secondaryCount ($secondaryRegions) | Stabiliser: $stabiliserCount ($stabiliserRegions)")
    }

    fun logDrawStatistics(
        totalCalls: Int,
        silhouetteCalls: Int,
        primaryCalls: Int,
        secondaryCalls: Int,
        stabiliserCalls: Int,
        inactiveCalls: Int
    ) {
        if (!ENABLED) return
        Log.d(TAG, "[STATS] TotalDrawCalls: $totalCalls | Silhouette: $silhouetteCalls | Primary: $primaryCalls | Secondary: $secondaryCalls | Stabiliser: $stabiliserCalls | Inactive: $inactiveCalls")
    }

    fun logPipelineSummary(
        stageName: String,
        verifiedAtSource: Boolean,
        verifiedAtRuntime: Boolean,
        blockerReason: String? = null
    ) {
        if (!ENABLED) return
        val runtimeStatus = if (verifiedAtRuntime) "VERIFIED_RUNTIME" else if (blockerReason != null) "BLOCKED:$blockerReason" else "UNVERIFIED"
        Log.d(TAG, "[SUMMARY] Stage: $stageName | SourceVerified: $verifiedAtSource | RuntimeStatus: $runtimeStatus | Blocker: $blockerReason")
    }
}
