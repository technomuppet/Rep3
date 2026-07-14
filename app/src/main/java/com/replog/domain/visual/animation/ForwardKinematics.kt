package com.replog.domain.visual.animation

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.biomechanics.BiomechanicalJointModel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Solved hierarchical skeleton model holding world-space joint coordinates evaluated
 * by the Forward Kinematics solver.
 */
data class SolvedSkeleton(
    val rootPosition: Offset,
    val scaleFactor: Float,
    val joints: Map<JointId, SolvedJoint>
) {
    fun getJoint(id: JointId): SolvedJoint? = joints[id]
    fun getWorldPosition(id: JointId): Offset = joints[id]?.worldPositionOffset ?: rootPosition
}

/**
 * Authoritative Forward Kinematics (FK) solver aligned with the 15-point (24-joint bilateral) body rig.
 * Traverses parent-first, clamps angles to biomechanical limits, and preserves constant bone lengths.
 */
object ForwardKinematicsSolver {

    private const val DEG_TO_RAD = (Math.PI / 180.0).toFloat()

    fun solve(
        jointRotations: Map<JointId, Float>,
        rootPosition: Offset = Offset(0.5f, 0.45f),
        scaleFactor: Float = 1.0f
    ): SolvedSkeleton {
        val solvedMap = mutableMapOf<JointId, SolvedJoint>()

        // 1. Solve Root Pelvis
        val pelvisRaw = jointRotations[JointId.PELVIS] ?: 0f
        val pelvisRot = BiomechanicalJointModel.clamp(JointId.PELVIS, pelvisRaw)
        solvedMap[JointId.PELVIS] = SolvedJoint(
            id = JointId.PELVIS,
            parentId = null,
            localRotationDegrees = pelvisRot,
            worldRotationDegrees = pelvisRot,
            localPositionOffset = Offset.Zero,
            worldPositionOffset = rootPosition,
            constraint = JointId.PELVIS.defaultConstraint
        )

        // 2. Authoritative 24-joint traversal list (includes compatibility legacy fallbacks)
        val evaluationOrder = listOf(
            // Central Spine
            JointId.LOWER_SPINE, JointId.MID_SPINE, JointId.UPPER_SPINE, JointId.NECK, JointId.HEAD,
            // Legacy chest chain
            JointId.CHEST, JointId.UPPER_CHEST,
            // Left Arm
            JointId.LEFT_SHOULDER, JointId.LEFT_ELBOW, JointId.LEFT_WRIST, JointId.LEFT_HAND,
            // Right Arm
            JointId.RIGHT_SHOULDER, JointId.RIGHT_ELBOW, JointId.RIGHT_WRIST, JointId.RIGHT_HAND,
            // Left Leg
            JointId.LEFT_HIP, JointId.LEFT_KNEE, JointId.LEFT_ANKLE, JointId.LEFT_HEEL, JointId.LEFT_TOE, JointId.LEFT_FOOT,
            // Right Leg
            JointId.RIGHT_HIP, JointId.RIGHT_KNEE, JointId.RIGHT_ANKLE, JointId.RIGHT_HEEL, JointId.RIGHT_TOE, JointId.RIGHT_FOOT
        )

        for (jointId in evaluationOrder) {
            val parentId = jointId.parentId ?: continue
            val parentSolved = solvedMap[parentId] ?: continue
            val bone = BoneCatalog.getBoneToChild(jointId) ?: continue

            val rawRot = jointRotations[jointId] ?: 0f
            val clampedLocalRot = BiomechanicalJointModel.clamp(jointId, rawRot)
            val worldAngleDeg = parentSolved.worldRotationDegrees + bone.defaultOrientationDegrees + clampedLocalRot
            val worldAngleRad = worldAngleDeg * DEG_TO_RAD

            val scaledLength = bone.normalizedLength * scaleFactor
            val deltaOffset = Offset(
                x = cos(worldAngleRad) * scaledLength,
                y = sin(worldAngleRad) * scaledLength
            )
            val solvedWorldPos = parentSolved.worldPositionOffset + deltaOffset

            solvedMap[jointId] = SolvedJoint(
                id = jointId,
                parentId = parentId,
                localRotationDegrees = clampedLocalRot,
                worldRotationDegrees = worldAngleDeg,
                localPositionOffset = deltaOffset,
                worldPositionOffset = solvedWorldPos,
                constraint = jointId.defaultConstraint
            )
        }

        return SolvedSkeleton(
            rootPosition = rootPosition,
            scaleFactor = scaleFactor,
            joints = solvedMap
        )
    }
}
