package com.replog.domain.visual.animation

import androidx.compose.ui.geometry.Offset

enum class PoseMarker {
    IDLE,
    START,
    BOTTOM,
    MID,
    TOP,
    LOCKOUT,
    STRETCH,
    CONTRACTED
}

/**
 * Immutable kinematic pose defining joint rotations for a specific exercise state.
 * Stores relative rotations only; zero absolute screen coordinates are contained.
 */
data class SkeletalPose(
    val marker: PoseMarker,
    val jointRotations: Map<JointId, Float>,
    val rootPositionOffset: Offset = Offset(0.5f, 0.45f)
) {
    fun getRotation(jointId: JointId): Float = jointRotations[jointId] ?: 0f

    companion object {
        val IDLE = SkeletalPose(PoseMarker.IDLE, emptyMap())
    }
}
