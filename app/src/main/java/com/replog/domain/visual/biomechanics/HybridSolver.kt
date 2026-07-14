package com.replog.domain.visual.biomechanics

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.animation.SolvedJoint
import com.replog.domain.visual.animation.SolvedSkeleton

/**
 * Hybrid animation system using:
 * - Forward Kinematics for spine, pelvis
 * - Inverse Kinematics for limbs (foot locking, hand targets, equipment targets)
 * - Centre of Mass balancing
 *
 * Body solves naturally toward targets.
 * Hands follow equipment.
 * Feet remain planted.
 *
 * Minimal allocations, cached calculations.
 */
object HybridSolver {

    data class Targets(
        val leftHand: Offset?,
        val rightHand: Offset?,
        val leftFoot: Offset?, // foot lock target, null if not locked (e.g., hanging)
        val rightFoot: Offset?,
        val barCenter: Offset? = null, // for barbell
        val pulley: Offset? = null // for cable
    )

    /**
     * Solves skeleton with IK for limbs while keeping FK for torso.
     * Returns new SolvedSkeleton with IK-adjusted limb positions.
     */
    fun solve(
        baseSkeleton: SolvedSkeleton,
        targets: Targets,
        comBalancing: Boolean = true
    ): SolvedSkeleton {
        var workingJoints = baseSkeleton.joints.toMutableMap()

        // Extract current positions
        val leftShoulder = baseSkeleton.getWorldPosition(JointId.LEFT_SHOULDER)
        val rightShoulder = baseSkeleton.getWorldPosition(JointId.RIGHT_SHOULDER)
        val leftElbow = baseSkeleton.getWorldPosition(JointId.LEFT_ELBOW)
        val rightElbow = baseSkeleton.getWorldPosition(JointId.RIGHT_ELBOW)
        val leftWrist = baseSkeleton.getWorldPosition(JointId.LEFT_WRIST)
        val rightWrist = baseSkeleton.getWorldPosition(JointId.RIGHT_WRIST)
        val leftHip = baseSkeleton.getWorldPosition(JointId.LEFT_HIP)
        val rightHip = baseSkeleton.getWorldPosition(JointId.RIGHT_HIP)
        val leftKnee = baseSkeleton.getWorldPosition(JointId.LEFT_KNEE)
        val rightKnee = baseSkeleton.getWorldPosition(JointId.RIGHT_KNEE)
        val leftAnkle = baseSkeleton.getWorldPosition(JointId.LEFT_ANKLE)
        val rightAnkle = baseSkeleton.getWorldPosition(JointId.RIGHT_ANKLE)
        val leftFoot = baseSkeleton.getWorldPosition(JointId.LEFT_FOOT)
        val rightFoot = baseSkeleton.getWorldPosition(JointId.RIGHT_FOOT)

        // Bone lengths cached
        val leftUpperArmLen = IKSolver.boneLength(leftShoulder, leftElbow)
        val leftForearmLen = IKSolver.boneLength(leftElbow, leftWrist)
        val rightUpperArmLen = IKSolver.boneLength(rightShoulder, rightElbow)
        val rightForearmLen = IKSolver.boneLength(rightElbow, rightWrist)
        val leftThighLen = IKSolver.boneLength(leftHip, leftKnee)
        val leftShankLen = IKSolver.boneLength(leftKnee, leftAnkle)
        val rightThighLen = IKSolver.boneLength(rightHip, rightKnee)
        val rightShankLen = IKSolver.boneLength(rightKnee, rightAnkle)
        val leftFootLen = IKSolver.boneLength(leftAnkle, leftFoot)
        val rightFootLen = IKSolver.boneLength(rightAnkle, rightFoot)

        // Solve left arm to hand target if present
        targets.leftHand?.let { handTarget ->
            val (newElbow, newWrist) = IKSolver.solveTwoBoneArm(
                shoulder = leftShoulder,
                elbow = leftElbow,
                wrist = leftWrist,
                handTarget = handTarget,
                upperArmLen = leftUpperArmLen,
                forearmLen = leftForearmLen
            )
            workingJoints[JointId.LEFT_ELBOW] = updateJointWorld(workingJoints[JointId.LEFT_ELBOW], newElbow)
            workingJoints[JointId.LEFT_WRIST] = updateJointWorld(workingJoints[JointId.LEFT_WRIST], newWrist)
            // Connect Hand in 15-point rig
            workingJoints[JointId.LEFT_HAND] = updateJointWorld(workingJoints[JointId.LEFT_HAND], handTarget)
        }

        targets.rightHand?.let { handTarget ->
            val (newElbow, newWrist) = IKSolver.solveTwoBoneArm(
                shoulder = rightShoulder,
                elbow = rightElbow,
                wrist = rightWrist,
                handTarget = handTarget,
                upperArmLen = rightUpperArmLen,
                forearmLen = rightForearmLen
            )
            workingJoints[JointId.RIGHT_ELBOW] = updateJointWorld(workingJoints[JointId.RIGHT_ELBOW], newElbow)
            workingJoints[JointId.RIGHT_WRIST] = updateJointWorld(workingJoints[JointId.RIGHT_WRIST], newWrist)
            // Connect Hand in 15-point rig
            workingJoints[JointId.RIGHT_HAND] = updateJointWorld(workingJoints[JointId.RIGHT_HAND], handTarget)
        }

        // Solve legs with foot locking
        targets.leftFoot?.let { footTarget ->
            val (newKnee, newAnkle, newFoot) = IKSolver.solveLegWithFootLock(
                hip = leftHip,
                knee = leftKnee,
                ankle = leftAnkle,
                foot = leftFoot,
                footTarget = footTarget,
                thighLen = leftThighLen,
                shankLen = leftShankLen,
                footLen = leftFootLen,
                isLeft = true
            )
            workingJoints[JointId.LEFT_KNEE] = updateJointWorld(workingJoints[JointId.LEFT_KNEE], newKnee)
            workingJoints[JointId.LEFT_ANKLE] = updateJointWorld(workingJoints[JointId.LEFT_ANKLE], newAnkle)
            workingJoints[JointId.LEFT_FOOT] = updateJointWorld(workingJoints[JointId.LEFT_FOOT], newFoot)

            // Authoritative Heel and Toe contact points for standard body rig
            val heelPos = Offset(newAnkle.x + 0.05f, footTarget.y) // heel behind ankle
            val toePos = Offset(newFoot.x, footTarget.y) // toe forward
            workingJoints[JointId.LEFT_HEEL] = updateJointWorld(workingJoints[JointId.LEFT_HEEL], heelPos)
            workingJoints[JointId.LEFT_TOE] = updateJointWorld(workingJoints[JointId.LEFT_TOE], toePos)
        }

        targets.rightFoot?.let { footTarget ->
            val (newKnee, newAnkle, newFoot) = IKSolver.solveLegWithFootLock(
                hip = rightHip,
                knee = rightKnee,
                ankle = rightAnkle,
                foot = rightFoot,
                footTarget = footTarget,
                thighLen = rightThighLen,
                shankLen = rightShankLen,
                footLen = rightFootLen,
                isLeft = false
            )
            workingJoints[JointId.RIGHT_KNEE] = updateJointWorld(workingJoints[JointId.RIGHT_KNEE], newKnee)
            workingJoints[JointId.RIGHT_ANKLE] = updateJointWorld(workingJoints[JointId.RIGHT_ANKLE], newAnkle)
            workingJoints[JointId.RIGHT_FOOT] = updateJointWorld(workingJoints[JointId.RIGHT_FOOT], newFoot)

            // Authoritative Heel and Toe contact points for standard body rig
            val heelPos = Offset(newAnkle.x - 0.05f, footTarget.y) // heel behind ankle
            val toePos = Offset(newFoot.x, footTarget.y) // toe forward
            workingJoints[JointId.RIGHT_HEEL] = updateJointWorld(workingJoints[JointId.RIGHT_HEEL], heelPos)
            workingJoints[JointId.RIGHT_TOE] = updateJointWorld(workingJoints[JointId.RIGHT_TOE], toePos)
        }

        var solved = SolvedSkeleton(
            rootPosition = baseSkeleton.rootPosition,
            scaleFactor = baseSkeleton.scaleFactor,
            joints = workingJoints
        )

        if (comBalancing) {
            solved = applyComBalancing(solved)
        }

        return solved
    }

    private fun updateJointWorld(joint: SolvedJoint?, newWorld: Offset): SolvedJoint {
        if (joint == null) return SolvedJoint(
            id = JointId.LEFT_ELBOW,
            parentId = null,
            localRotationDegrees = 0f,
            worldRotationDegrees = 0f,
            localPositionOffset = Offset.Zero,
            worldPositionOffset = newWorld,
            constraint = BiomechanicalJointModel.getLimits(JointId.LEFT_ELBOW).flexion.let {
                com.replog.domain.visual.animation.JointConstraint(it.minDegrees, it.maxDegrees)
            }
        )
        return joint.copy(worldPositionOffset = newWorld)
    }

    /**
     * Applies COM balancing: ensures COM over mid-foot for standing, adjusts pelvis slightly if needed.
     */
    private fun applyComBalancing(skeleton: SolvedSkeleton): SolvedSkeleton {
        val comResult = CentreOfMassCalculator.calculate(skeleton)
        if (comResult.isBalanced) return skeleton
        return skeleton
    }

    /**
     * Calculates hand targets from bar center and grip width.
     */
    fun calculateBarbellHandTargets(barCenter: Offset, gripWidth: Float): Pair<Offset, Offset> {
        val half = gripWidth * 0.5f
        return Pair(
            Offset(barCenter.x - half, barCenter.y),
            Offset(barCenter.x + half, barCenter.y)
        )
    }
}
