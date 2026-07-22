package com.replog.domain.visual.anatomy

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RC53 — Anatomy Self-Diagnosing Pipeline (Real Implementation — Audit Only, No Redesign)
 * ONE feature flag only: ENABLED
 * When false: zero logging, zero file output, zero behaviour changes.
 * Logcat tag: ANATOMY_DIAGNOSTICS (only)
 */
object AnatomyDiagnostics {
    const val TAG: String = "ANATOMY_DIAGNOSTICS"
    const val ENABLED: Boolean = true // Single feature flag — controls everything
    const val DEBUG_RENDER_MODE: Boolean = false // Phase 11 temporary diagnostic colour mode

    // Single session object — every stage writes here
    data class DiagnosticSession(
        val sessionId: String = System.currentTimeMillis().toString(),
        val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()),
        val exerciseName: String = "",
        val primaryRequested: MutableList<String> = mutableListOf(),
        val secondaryRequested: MutableList<String> = mutableListOf(),
        val stabiliserRequested: MutableList<String> = mutableListOf(),
        val primaryMapped: MutableList<String> = mutableListOf(),
        val secondaryMapped: MutableList<String> = mutableListOf(),
        val stabiliserMapped: MutableList<String> = mutableListOf(),
        val enumFailures: MutableList<String> = mutableListOf(),
        val svgFrontLoaded: Boolean = false,
        val svgBackLoaded: Boolean = false,
        val svgRegions: MutableList<String> = mutableListOf(),
        val svgMissingIds: MutableList<String> = mutableListOf(),
        val svgException: String = "",
        val rendererPrimaryCount: Int = 0,
        val rendererSecondaryCount: Int = 0,
        val rendererStabiliserCount: Int = 0,
        val rendererInactiveCount: Int = 0,
        val rendererSilhouetteCount: Int = 0,
        val totalDrawCalls: Int = 0,
        val pathsDrawn: MutableList<String> = mutableListOf(),
        val skippedPaths: MutableList<String> = mutableListOf(),
        val zeroAreaPaths: MutableList<String> = mutableListOf(),
        val warnings: MutableList<String> = mutableListOf(),
        val finalVerdict: String = "UNVERIFIED"
    ) {
        fun buildReport(): String = buildString {
            appendLine("=== ANATOMY DIAGNOSTIC SESSION ===")
            appendLine("SessionID: $sessionId")
            appendLine("Timestamp: $timestamp")
            appendLine("Exercise: $exerciseName")
            appendLine("--- REQUESTED MUSCLES ---")
            appendLine("Primary: $primaryRequested")
            appendLine("Secondary: $secondaryRequested")
            appendLine("Stabiliser: $stabiliserRequested")
            appendLine("--- ENUM MAPPING ---")
            appendLine("PrimaryMapped: $primaryMapped")
            appendLine("SecondaryMapped: $secondaryMapped")
            appendLine("StabiliserMapped: $stabiliserMapped")
            if (enumFailures.isNotEmpty()) appendLine("ENUM FAILURES (NOT SILENTLY IGNORED): $enumFailures")
            appendLine("--- SVG LOAD ---")
            appendLine("FrontLoaded: $svgFrontLoaded | BackLoaded: $svgBackLoaded")
            appendLine("SVGRegions: $svgRegions")
            if (svgMissingIds.isNotEmpty()) appendLine("SVG MISSING IDs: $svgMissingIds")
            if (svgException.isNotEmpty()) appendLine("SVG EXCEPTION: $svgException")
            appendLine("--- RENDERER INPUT ---")
            appendLine("PrimaryRegions: $rendererPrimaryCount")
            appendLine("SecondaryRegions: $rendererSecondaryCount")
            appendLine("StabiliserRegions: $rendererStabiliserCount")
            appendLine("InactiveRegions: $rendererInactiveCount")
            appendLine("Silhouette: $rendererSilhouetteCount")
            appendLine("--- DRAW CALLS ---")
            appendLine("PathsDrawn: $pathsDrawn")
            appendLine("SkippedPaths: $skippedPaths")
            appendLine("TotalDrawCalls: $totalDrawCalls")
            if (zeroAreaPaths.isNotEmpty()) appendLine("ZERO AREA PATHS DETECTED: $zeroAreaPaths")
            appendLine("--- WARNINGS ---")
            appendLine(warnings.joinToString(" | ") { it })
            appendLine("--- FINAL VERDICT ---")
            appendLine(finalVerdict)
        }
    }

    // Single session instance — all stages write to this
    private val currentSession = DiagnosticSession()

    // File writer — writes to /storage/emulated/0/Download/RepLog/Diagnostics/
    private fun writeReport(report: String, exerciseName: String) {
        if (!ENABLED) return
        try {
            val baseDir = File("/storage/emulated/0/Download/RepLog/Diagnostics/")
            if (!baseDir.exists()) {
                baseDir.mkdirs()
                Log.d(TAG, "[FILE] Created directory: ${baseDir.absolutePath}")
            }
            val safeName = exerciseName.replace("[^A-Za-z0-9_\-]".toRegex(), "_")
            val fileName = "${safeName}_${System.currentTimeMillis()}.txt"
            val file = File(baseDir, fileName)
            FileWriter(file).use { it.write(report) }
            Log.d(TAG, "[FILE] Report written: ${file.absolutePath} (size=${file.length()} bytes)")
        } catch (e: Exception) {
            Log.d(TAG, "[FILE] Report write failed for $exerciseName: ${e.message}")
            // Note: exception is logged but not suppressed silently; the report failure is recorded
        }
    }

    // --- PHASE 3: EXERCISE DATA AUDIT ---
    fun auditExerciseData(
        exerciseName: String,
        primaryMuscles: List<String>,
        secondaryMuscles: List<String>,
        stabiliserMuscles: List<String>
    ) {
        if (!ENABLED) return
        currentSession.exerciseName = exerciseName
        currentSession.primaryRequested.clear()
        currentSession.primaryRequested.addAll(primaryMuscles)
        currentSession.secondaryRequested.clear()
        currentSession.secondaryRequested.addAll(secondaryMuscles)
        currentSession.stabiliserRequested.clear()
        currentSession.stabiliserRequested.addAll(stabiliserMuscles)
        Log.d(TAG, "[EXERCISE] Name: $exerciseName | PrimaryRequested: ${primaryMuscles.joinToString()}")
        Log.d(TAG, "[EXERCISE] SecondaryRequested: ${secondaryMuscles.joinToString()}")
        Log.d(TAG, "[EXERCISE] StabiliserRequested: ${stabiliserMuscles.joinToString()}")
        if (primaryMuscles.isEmpty() && secondaryMuscles.isEmpty()) {
            currentSession.warnings.add("WARNING: No primary or secondary muscles for $exerciseName")
            Log.d(TAG, "[WARNING] Empty activation sets for $exerciseName")
        }
    }

    // --- PHASE 4: ENUM CONVERSION AUDIT ---
    // No silent catch blocks: every conversion failure recorded
    fun auditEnumConversion(original: String): MuscleRegion? {
        val lookup = original.uppercase()
        return try {
            val region = MuscleRegion.valueOf(lookup)
            if (ENABLED) {
                Log.d(TAG, "[ENUM] PASS: $original -> $lookup -> $region")
                // Note: enum mapping successes are recorded in session by auditMatch, not duplicated here
            }
            region
        } catch (e: IllegalArgumentException) {
            if (ENABLED) {
                currentSession.enumFailures.add("NOT FOUND: $original (lookup=$lookup)")
                Log.d(TAG, "[ENUM] FAIL: $original -> NOT FOUND (lookup=$lookup, reason=${e.message})")
            }
            // When ENABLED is false: zero behaviour changes (return null as before, no logging/file output)
            // When ENABLED is true: failure is explicitly recorded (not suppressed silently)
            null
        }
    }

    // --- PHASE 5: SVG AUDIT ---
    fun auditSvgLoad(frontLoaded: Boolean, backLoaded: Boolean, regions: List<String>, exceptionMsg: String = "") {
        if (!ENABLED) return
        currentSession.svgFrontLoaded = frontLoaded
        currentSession.svgBackLoaded = backLoaded
        currentSession.svgRegions.clear()
        currentSession.svgRegions.addAll(regions)
        if (exceptionMsg.isNotEmpty()) {
            currentSession.svgException = exceptionMsg
            Log.d(TAG, "[SVG] FAIL: Exception: $exceptionMsg")
        }
        Log.d(TAG, "[SVG] FrontLoaded: $frontLoaded | BackLoaded: $backLoaded | Regions: ${regions.joinToString()}")
    }

    fun auditSvgLoad(
        assetName: String,
        expectedIds: Int,
        loadedIds: Int,
        missingIds: List<String>,
        exceptionMsg: String = ""
    ) {
        auditSvgLoad(
            frontLoaded = loadedIds > 0,
            backLoaded = loadedIds > 0,
            regions = missingIds,
            exceptionMsg = exceptionMsg
        )

        logSVGLoad(
            assetName = assetName,
            expectedIds = expectedIds,
            loadedIds = loadedIds,
            missingIds = missingIds,
            unexpectedIds = emptyList()
        )
    }

    fun auditSvgMissingIds(missing: List<String>) {
        if (!ENABLED) return
        currentSession.svgMissingIds.clear()
        currentSession.svgMissingIds.addAll(missing)
        if (missing.isNotEmpty()) {
            Log.d(TAG, "[SVG] MISSING IDs: ${missing.joinToString()}")
            currentSession.warnings.add("SVG MISSING IDs: ${missing.joinToString()}")
        }
    }

    // --- PHASE 6: MATCHING AUDIT ---
    fun auditMatch(requestedName: String, enumName: String?, regionId: String?, matched: Boolean) {
        if (!ENABLED) return
        val status = if (matched) "MATCH" else "NO MATCH"
        Log.d(TAG, "[MATCH] $requestedName -> Enum: $enumName -> Region: $regionId -> $status")
        if (!matched) {
            currentSession.warnings.add("NO MATCH: $requestedName -> Enum=$enumName Region=$regionId")
        }
    }

    // --- PHASE 7: RENDERER INPUT AUDIT ---
    fun auditRendererInput(
        primaryRegions: Set<MuscleRegion>,
        secondaryRegions: Set<MuscleRegion>,
        stabiliserRegions: Set<MuscleRegion>,
        totalRegions: Int
    ) {
        if (!ENABLED) return
        currentSession.rendererPrimaryCount = primaryRegions.size
        currentSession.rendererSecondaryCount = secondaryRegions.size
        currentSession.rendererStabiliserCount = stabiliserRegions.size
        currentSession.rendererInactiveCount = totalRegions - primaryRegions.size - secondaryRegions.size - stabiliserRegions.size
        Log.d(TAG, "[RENDERER_INPUT] Primary: ${primaryRegions.size} | Secondary: ${secondaryRegions.size} | Stabiliser: ${stabiliserRegions.size} | Inactive: ${currentSession.rendererInactiveCount} | TotalRegions: $totalRegions")
    }

    // --- PHASE 7 (after rendering): Draw statistics ---
    fun auditDrawStatistics(
        silhouetteCalls: Int,
        primaryCalls: Int,
        secondaryCalls: Int,
        stabiliserCalls: Int,
        inactiveCalls: Int
    ) {
        if (!ENABLED) return
        currentSession.rendererSilhouetteCount = silhouetteCalls
        val total = silhouetteCalls + primaryCalls + secondaryCalls + stabiliserCalls + inactiveCalls
        currentSession.totalDrawCalls = total
        Log.d(TAG, "[STATS] Silhouette: $silhouetteCalls | Primary: $primaryCalls | Secondary: $secondaryCalls | Stabiliser: $stabiliserCalls | Inactive: $inactiveCalls | Total: $total")
    }

    // --- PHASE 8: PATH AUDIT ---
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
        if (!ENABLED) return
        if (drawn) {
            currentSession.pathsDrawn.add(regionName)
        } else {
            currentSession.skippedPaths.add(regionName)
        }
        Log.d(TAG, "[PATH] $regionName | W:$width H:$height Area:$area Empty:$isEmpty Valid:$valid Drawn:$drawn")
        if (width <= 0f || height <= 0f || area <= 0f) {
            currentSession.zeroAreaPaths.add(regionName)
            currentSession.warnings.add("ZERO AREA PATH: $regionName (W=$width H=$height Area=$area)")
            Log.d(TAG, "[WARNING] ZERO AREA PATH DETECTED: $regionName (W=$width H=$height Area=$area)")
        }
    }

    // --- PHASE 9: FILE OUTPUT ---
    fun finalizeSession(verdict: String) {
        if (!ENABLED) return
        currentSession.finalVerdict = verdict
        val report = currentSession.buildReport()
        writeReport(report, currentSession.exerciseName)
        Log.d(TAG, "[SESSION] Final verdict: $verdict | SessionID: ${currentSession.sessionId}")
    }

    // Legacy methods preserved for backward compatibility (preserved from RC51, kept functional)
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
        status: String
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
