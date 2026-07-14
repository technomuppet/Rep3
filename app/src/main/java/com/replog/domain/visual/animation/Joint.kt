package com.replog.domain.visual.animation

import androidx.compose.ui.geometry.Offset

/**
 * Anatomical joint constraint defining realistic rotation limits in degrees.
 * Prevents hyperextension, inverted limbs, and impossible biomechanical poses.
 */
data class JointConstraint(
    val minAngleDegrees: Float,
    val maxAngleDegrees: Float
) {
    fun clamp(angle: Float): Float = angle.coerceIn(minAngleDegrees, maxAngleDegrees)

    companion object {
        val FREEDOM_FULL = JointConstraint(-180f, 180f)
        val SPINE_LIMITED = JointConstraint(-30f, 30f)
        val NECK_LIMITED = JointConstraint(-45f, 45f)
        val SHOULDER_FLEXION = JointConstraint(-180f, 90f)
        val ELBOW_FLEXION = JointConstraint(0f, 150f)
        val HIP_FLEXION = JointConstraint(-135f, 45f)
        val KNEE_FLEXION = JointConstraint(0f, 145f)
        val ANKLE_FLEXION = JointConstraint(-45f, 45f)
    }
}

/**
 * Hierarchical joint identifiers representing the authoritative 15-point (24-joint bilateral) body rig.
 * Canonical Source of Truth: REPLOG - BODY JOINT WORKSHEET.
 * Includes transitional compatibility aliases (marked @Deprecated) to ensure continuous compilation.
 */
enum class JointId(val parentId: JointId?, val defaultConstraint: JointConstraint) {
    PELVIS(null, JointConstraint.FREEDOM_FULL),
    LOWER_SPINE(PELVIS, JointConstraint.SPINE_LIMITED),
    MID_SPINE(LOWER_SPINE, JointConstraint.SPINE_LIMITED),
    UPPER_SPINE(MID_SPINE, JointConstraint.SPINE_LIMITED),
    NECK(UPPER_SPINE, JointConstraint.NECK_LIMITED),
    HEAD(NECK, JointConstraint.NECK_LIMITED),

    LEFT_SHOULDER(UPPER_SPINE, JointConstraint.SHOULDER_FLEXION),
    RIGHT_SHOULDER(UPPER_SPINE, JointConstraint.SHOULDER_FLEXION),
    LEFT_ELBOW(LEFT_SHOULDER, JointConstraint.ELBOW_FLEXION),
    RIGHT_ELBOW(RIGHT_SHOULDER, JointConstraint.ELBOW_FLEXION),
    LEFT_WRIST(LEFT_ELBOW, JointConstraint.FREEDOM_FULL),
    RIGHT_WRIST(RIGHT_ELBOW, JointConstraint.FREEDOM_FULL),
    LEFT_HAND(LEFT_WRIST, JointConstraint.FREEDOM_FULL),
    RIGHT_HAND(RIGHT_WRIST, JointConstraint.FREEDOM_FULL),

    LEFT_HIP(PELVIS, JointConstraint.HIP_FLEXION),
    RIGHT_HIP(PELVIS, JointConstraint.HIP_FLEXION),
    LEFT_KNEE(LEFT_HIP, JointConstraint.KNEE_FLEXION),
    RIGHT_KNEE(RIGHT_HIP, JointConstraint.KNEE_FLEXION),
    LEFT_ANKLE(LEFT_KNEE, JointConstraint.ANKLE_FLEXION),
    RIGHT_ANKLE(RIGHT_KNEE, JointConstraint.ANKLE_FLEXION),
    LEFT_HEEL(LEFT_ANKLE, JointConstraint.ANKLE_FLEXION),
    RIGHT_HEEL(RIGHT_ANKLE, JointConstraint.ANKLE_FLEXION),
    LEFT_TOE(LEFT_ANKLE, JointConstraint.ANKLE_FLEXION),
    RIGHT_TOE(RIGHT_ANKLE, JointConstraint.ANKLE_FLEXION),

    // --- TRANSITIONAL COMPATIBILITY ALIASES ---
    @Deprecated("Superseded by LOWER_SPINE / MID_SPINE")
    CHEST(PELVIS, JointConstraint.SPINE_LIMITED),
    @Deprecated("Superseded by UPPER_SPINE")
    UPPER_CHEST(CHEST, JointConstraint.SPINE_LIMITED),
    @Deprecated("Superseded by LEFT_HEEL / LEFT_TOE")
    LEFT_FOOT(LEFT_ANKLE, JointConstraint.ANKLE_FLEXION),
    @Deprecated("Superseded by RIGHT_HEEL / RIGHT_TOE")
    RIGHT_FOOT(RIGHT_ANKLE, JointConstraint.ANKLE_FLEXION);
}

/**
 * Solved state of an anatomical joint storing local kinematic parameters and evaluated
 * world-space coordinates. No screen coordinates are stored inside joints.
 */
data class SolvedJoint(
    val id: JointId,
    val parentId: JointId?,
    val localRotationDegrees: Float,
    val worldRotationDegrees: Float,
    val localPositionOffset: Offset,
    val worldPositionOffset: Offset,
    val constraint: JointConstraint
)
