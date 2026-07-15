package com.replog.domain.visual.presentation

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.spec.AnatomySpec

/**
 * Clean Architecture Exercise Asset Format — RC40
 * High-quality pose-based structural definition for exercise teaching.
 * Separates data-driven movement metrics, textbook diagrams, and step-by-step poses.
 */
data class ExercisePresentationAsset(
    val exerciseId: Int,
    val name: String,
    val equipment: String,
    val difficulty: String,
    val musclesWorkedText: String,
    val setupChecklist: List<String>,
    val executionSteps: List<String>,
    val commonMistakes: List<String>,
    val coachingTips: List<String>,
    val safetyNotes: List<String>,
    val anatomySpec: AnatomySpec,
    val poses: List<PoseIllustration>,
    val overlayType: OverlayType,
    // Phase 5: Lightweight Sprite & Frame Animation Support
    val spriteSheetUri: String? = null,
    val frameCount: Int = 0,
    val useFrameAnimation: Boolean = false
)

data class PoseIllustration(
    val stageName: String, // e.g. "1. Setup", "2. Start", "3. Midpoint", "4. Peak Contraction", "5. Return"
    val description: String,
    val skeletonOffsets: Map<String, Offset>, // pre-authored pose coordinates (normalized 0..1)
    val activeMuscles: Set<String>, // list of muscles contracting at this stage
    val overlayCues: List<OverlayCue>
)

data class OverlayCue(
    val type: CueType,
    val startJoint: String,
    val endJoint: String,
    val label: String,
    val colorHex: String = "#0A84FF"
)

enum class OverlayType {
    BAR_PATH,
    HIP_HINGE,
    ARM_PATH,
    SQUAT_DEPTH,
    CABLE_PATH,
    CORE_HOLD,
    GENERIC
}

enum class CueType {
    LINE,
    ARC,
    ANGLE,
    MARKER,
    FORCE_VECTOR
}
