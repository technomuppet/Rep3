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
 * Hierarchical joint identifiers representing the 19 primary anatomical articulation points.
 */
enum class JointId(val parentId: JointId?, val defaultConstraint: JointConstraint) {
    PELVIS(null, JointConstraint.FREEDOM_FULL),
    CHEST(PELVIS, JointConstraint.SPINE_LIMITED),
    UPPER_CHEST(CHEST, JointConstraint.SPINE_LIMITED),
    NECK(UPPER_CHEST, JointConstraint.NECK_LIMITED),
    HEAD(NECK, JointConstraint.NECK_LIMITED),

    LEFT_SHOULDER(UPPER_CHEST, JointConstraint.SHOULDER_FLEXION),
    RIGHT_SHOULDER(UPPER_CHEST, JointConstraint.SHOULDER_FLEXION),
    LEFT_ELBOW(LEFT_SHOULDER, JointConstraint.ELBOW_FLEXION),
    RIGHT_ELBOW(RIGHT_SHOULDER, JointConstraint.ELBOW_FLEXION),
    LEFT_WRIST(LEFT_ELBOW, JointConstraint.FREEDOM_FULL),
    RIGHT_WRIST(RIGHT_ELBOW, JointConstraint.FREEDOM_FULL),

    LEFT_HIP(PELVIS, JointConstraint.HIP_FLEXION),
    RIGHT_HIP(PELVIS, JointConstraint.HIP_FLEXION),
    LEFT_KNEE(LEFT_HIP, JointConstraint.KNEE_FLEXION),
    RIGHT_KNEE(RIGHT_HIP, JointConstraint.KNEE_FLEXION),
    LEFT_ANKLE(LEFT_KNEE, JointConstraint.ANKLE_FLEXION),
    RIGHT_ANKLE(RIGHT_KNEE, JointConstraint.ANKLE_FLEXION),
    LEFT_FOOT(LEFT_ANKLE, JointConstraint.ANKLE_FLEXION),
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
