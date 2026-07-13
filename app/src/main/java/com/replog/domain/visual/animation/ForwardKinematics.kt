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
 * Commercial Forward Kinematics (FK) solver with realistic biomechanical limits.
 * RC20.3 — Now uses BiomechanicalJointModel for clamping with anatomical limits,
 * preserves invariant bone lengths, enforces no impossible positions.
 *
 * Part of hybrid FK/IK system: FK for spine/pelvis, IK for limbs via HybridSolver.
 */
object ForwardKinematicsSolver {

    private const val DEG_TO_RAD = (Math.PI / 180.0).toFloat()

    fun solve(
        jointRotations: Map<JointId, Float>,
        rootPosition: Offset = Offset(0.5f, 0.45f),
        scaleFactor: Float = 1.0f
    ): SolvedSkeleton {
        val solvedMap = mutableMapOf<JointId, SolvedJoint>()

        // 1. Solve Root Pelvis with realistic limits (pelvis tilt -20..20 not -180..180)
        val pelvisRaw = jointRotations[JointId.PELVIS] ?: 0f
        val pelvisRot = BiomechanicalJointModel.clamp(JointId.PELVIS, pelvisRaw)
        solvedMap[JointId.PELVIS] = SolvedJoint(
            id = JointId.PELVIS,
            parentId = null,
            localRotationDegrees = pelvisRot,
            worldRotationDegrees = pelvisRot,
            localPositionOffset = Offset.Zero,
            worldPositionOffset = rootPosition,
            constraint = JointId.PELVIS.defaultConstraint // keep for compatibility, but clamped via biomechanical model
        )

        // 2. Solve hierarchy in topological parent-first order
        val evaluationOrder = listOf(
            JointId.CHEST, JointId.UPPER_CHEST, JointId.NECK, JointId.HEAD,
            JointId.LEFT_SHOULDER, JointId.LEFT_ELBOW, JointId.LEFT_WRIST,
            JointId.RIGHT_SHOULDER, JointId.RIGHT_ELBOW, JointId.RIGHT_WRIST,
            JointId.LEFT_HIP, JointId.LEFT_KNEE, JointId.LEFT_ANKLE, JointId.LEFT_FOOT,
            JointId.RIGHT_HIP, JointId.RIGHT_KNEE, JointId.RIGHT_ANKLE, JointId.RIGHT_FOOT
        )

        for (jointId in evaluationOrder) {
            val parentId = jointId.parentId ?: continue
            val parentSolved = solvedMap[parentId] ?: continue
            val bone = BoneCatalog.getBoneToChild(jointId) ?: continue

            val rawRot = jointRotations[jointId] ?: 0f
            // RC20.3: Clamp using biomechanical model with realistic anatomical limits
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
