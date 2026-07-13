package com.replog.domain.visual

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.animation.BoneCatalog
import com.replog.domain.visual.animation.EasingCurve
import com.replog.domain.visual.animation.EquipmentAnchoring
import com.replog.domain.visual.animation.ForwardKinematicsSolver
import com.replog.domain.visual.animation.JointConstraint
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.animation.KinematicMovementFamilies
import com.replog.domain.visual.animation.PoseInterpolator
import com.replog.domain.visual.animation.PoseMarker
import com.replog.domain.visual.animation.SkeletalPose
import com.replog.domain.visual.spec.EquipmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class SkeletalAnimationEngineTest {

    @Test
    fun testHierarchicalForwardKinematicsSolvesAllJoints() {
        val rotations = mapOf(
            JointId.LEFT_SHOULDER to -90f,
            JointId.LEFT_ELBOW to 45f
        )
        val solved = ForwardKinematicsSolver.solve(rotations)
        assertEquals(19, solved.joints.size)
        for (jointId in JointId.entries) {
            assertNotNull("Every joint ID must be evaluated by FK solver", solved.getJoint(jointId))
        }
    }

    @Test
    fun testBoneLengthInvarianceUnderRotation() {
        val rotA = mapOf(JointId.LEFT_SHOULDER to -90f, JointId.LEFT_ELBOW to 0f)
        val solvedA = ForwardKinematicsSolver.solve(rotA)
        val shoulderA = solvedA.getWorldPosition(JointId.LEFT_SHOULDER)
        val elbowA = solvedA.getWorldPosition(JointId.LEFT_ELBOW)
        val distA = hypot((elbowA.x - shoulderA.x).toDouble(), (elbowA.y - shoulderA.y).toDouble()).toFloat()

        val rotB = mapOf(JointId.LEFT_SHOULDER to 0f, JointId.LEFT_ELBOW to 90f)
        val solvedB = ForwardKinematicsSolver.solve(rotB)
        val shoulderB = solvedB.getWorldPosition(JointId.LEFT_SHOULDER)
        val elbowB = solvedB.getWorldPosition(JointId.LEFT_ELBOW)
        val distB = hypot((elbowB.x - shoulderB.x).toDouble(), (elbowB.y - shoulderB.y).toDouble()).toFloat()

        assertEquals("Upper arm bone length must remain strictly invariant across rotations", distA, distB, 0.001f)
        assertEquals(BoneCatalog.L_UPPER_ARM.normalizedLength, distA, 0.001f)
    }

    @Test
    fun testJointConstraintClampingPreventingHyperextension() {
        val invalidRotations = mapOf(
            JointId.LEFT_ELBOW to 250f // Exceeds max 150 deg limit
        )
        val solved = ForwardKinematicsSolver.solve(invalidRotations)
        val elbowSolved = solved.getJoint(JointId.LEFT_ELBOW)
        assertNotNull(elbowSolved)
        assertEquals(150f, elbowSolved!!.localRotationDegrees, 0.001f)
    }

    @Test
    fun testPoseInterpolatorRotationalSlerp() {
        val poseA = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_ELBOW to 0f))
        val poseB = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.LEFT_ELBOW to 100f))

        val midPose = PoseInterpolator.interpolate(poseA, poseB, 0.5f, EasingCurve.LINEAR)
        assertEquals(50f, midPose.getRotation(JointId.LEFT_ELBOW), 0.001f)
    }

    @Test
    fun testMovementFamiliesTimelineEvaluation() {
        val families = listOf(
            "HORIZONTAL_PUSH", "INCLINE_PUSH", "DECLINE_PUSH", "VERTICAL_PUSH",
            "HORIZONTAL_PULL", "CABLE_ROW", "PULL_UP", "LAT_PULLDOWN",
            "DEADLIFT", "ROMANIAN_DEADLIFT", "SQUAT", "FRONT_SQUAT",
            "SPLIT_SQUAT", "LUNGE", "HIP_THRUST", "LEG_PRESS",
            "CURL", "OVERHEAD_EXTENSION", "LATERAL_RAISE", "OLYMPIC_LIFT"
        )
        for (fam in families) {
            val timeline = KinematicMovementFamilies.getTimelineForFamily(fam)
            assertTrue("Timeline duration for $fam must be positive", timeline.durationSeconds > 0f)
            val evaluatedPose = timeline.evaluate(1.0f)
            assertNotNull("Evaluating timeline for $fam must produce a valid pose", evaluatedPose)
        }
    }

    @Test
    fun testEquipmentAnchoringFollowsWrists() {
        val solved = ForwardKinematicsSolver.solve(emptyMap())
        val anchor = EquipmentAnchoring.solveAnchor(solved, EquipmentType.BARBELL)
        val leftWrist = solved.getWorldPosition(JointId.LEFT_WRIST)
        val rightWrist = solved.getWorldPosition(JointId.RIGHT_WRIST)
        val expectedMidX = (leftWrist.x + rightWrist.x) / 2f
        val expectedMidY = (leftWrist.y + rightWrist.y) / 2f

        assertEquals(expectedMidX, anchor.axisMidpoint.x, 0.001f)
        assertEquals(expectedMidY, anchor.axisMidpoint.y, 0.001f)
    }
}
