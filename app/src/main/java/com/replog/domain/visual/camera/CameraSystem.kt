package com.replog.domain.visual.camera

import com.replog.domain.visual.spec.ExerciseVisualSpec

/**
 * Camera system for RC20.4 — Proper rendering views:
 * Front, Rear, Left Side, Right Side, Automatic best-view selection based on exercise, manual override, prevent left/right limb overlap.
 *
 * Offline, Kotlin only.
 */
object CameraSystem {

    enum class CameraView {
        FRONT,
        REAR,
        LEFT_SIDE,
        RIGHT_SIDE,
        AUTO
    }

    /**
     * Automatic best-view selection based on exercise family and equipment.
     * Examples from spec:
     * Squat → side, Deadlift → side, Bench → side, Curl → front, Lateral raise → front, Pull-up → front
     */
    fun selectBestView(spec: ExerciseVisualSpec): CameraView {
        val family = spec.movementFamily.familyId
        val name = spec.exerciseName.lowercase()
        return when {
            // Side view best for sagittal plane movements where depth matters and left/right overlap would occur
            family in setOf("SQUAT", "FRONT_SQUAT", "HACK_SQUAT", "SPLIT_SQUAT", "LUNGE", "LEG_PRESS", "DEADLIFT", "ROMANIAN_DEADLIFT", "HIP_HINGE", "HIP_THRUST", "LEG_EXTENSION", "LEG_CURL") -> CameraView.RIGHT_SIDE
            family == "HORIZONTAL_PUSH" && (name.contains("bench press") || name.contains("floor press")) -> CameraView.RIGHT_SIDE
            family == "INCLINE_PUSH" || family == "DECLINE_PUSH" -> CameraView.RIGHT_SIDE
            family == "VERTICAL_PUSH" && name.contains("overhead press") -> CameraView.RIGHT_SIDE
            family == "CURL" || family == "HAMMER_CURL" || family == "PREACHER_CURL" -> CameraView.FRONT
            family == "LATERAL_RAISE" || family == "REAR_DELT_FLY" || family == "SHRUG" -> CameraView.FRONT
            family == "PULL_UP" || family == "LAT_PULLDOWN" || family == "CABLE_ROW" || family == "HORIZONTAL_PULL" -> CameraView.FRONT
            family == "PUSHDOWN" || family == "OVERHEAD_EXTENSION" -> CameraView.RIGHT_SIDE
            family == "CRUNCH" || family == "LEG_RAISE" || family == "PLANK" -> CameraView.RIGHT_SIDE
            family == "OLYMPIC_LIFT" -> CameraView.RIGHT_SIDE
            family == "CALF_RAISE" -> CameraView.RIGHT_SIDE
            family == "CABLE_FLY" -> CameraView.FRONT
            else -> CameraView.FRONT
        }
    }

    /**
     * Resolves final camera view considering manual override.
     */
    fun resolveView(spec: ExerciseVisualSpec, manualOverride: CameraView? = null): CameraView {
        if (manualOverride != null && manualOverride != CameraView.AUTO) return manualOverride
        return selectBestView(spec)
    }

    /**
     * Determines if left side limbs should be culled to prevent overlap in side views.
     */
    fun shouldCullLeftSide(view: CameraView): Boolean {
        return view == CameraView.RIGHT_SIDE
    }

    fun shouldCullRightSide(view: CameraView): Boolean {
        return view == CameraView.LEFT_SIDE
    }

    /**
     * Returns human-readable description for UI.
     */
    fun getViewDescription(view: CameraView): String {
        return when (view) {
            CameraView.FRONT -> "Front View"
            CameraView.REAR -> "Rear View"
            CameraView.LEFT_SIDE -> "Left Side View"
            CameraView.RIGHT_SIDE -> "Right Side View"
            CameraView.AUTO -> "Auto (Best View)"
        }
    }
}
