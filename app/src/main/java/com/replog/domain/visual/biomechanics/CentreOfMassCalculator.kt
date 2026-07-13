package com.replog.domain.visual.biomechanics

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.animation.SolvedSkeleton
import kotlin.math.hypot

/**
 * Centre of Mass calculation based on Dempster cadaver segment mass fractions
 * and de Leva adjustments.
 *
 * Segment mass fractions of total body mass:
 * Head+neck 8.1%, upper trunk 16%, lower trunk 27%, upper arm 2.8% each,
 * forearm 1.6% each, hand 0.6% each, thigh 10% each, shank 4.65% each, foot 1.45% each
 *
 * COM position along segment: e.g., thigh COM 43.3% from hip, shank 43.4% from knee, etc.
 *
 * Used for balancing: COM should remain over mid-foot for standing lifts.
 */
object CentreOfMassCalculator {

    data class SegmentMass(
        val parentJoint: JointId,
        val childJoint: JointId,
        val massFraction: Float,
        val comFractionFromParent: Float // 0..1 where COM lies along segment from parent
    )

    // Dempster segment definitions
    private val SEGMENTS = listOf(
        SegmentMass(JointId.NECK, JointId.HEAD, 0.081f, 0.5f),
        SegmentMass(JointId.CHEST, JointId.UPPER_CHEST, 0.16f, 0.5f), // upper trunk
        SegmentMass(JointId.PELVIS, JointId.CHEST, 0.27f, 0.5f), // lower trunk
        SegmentMass(JointId.LEFT_SHOULDER, JointId.LEFT_ELBOW, 0.028f, 0.436f),
        SegmentMass(JointId.RIGHT_SHOULDER, JointId.RIGHT_ELBOW, 0.028f, 0.436f),
        SegmentMass(JointId.LEFT_ELBOW, JointId.LEFT_WRIST, 0.016f, 0.430f),
        SegmentMass(JointId.RIGHT_ELBOW, JointId.RIGHT_WRIST, 0.016f, 0.430f),
        SegmentMass(JointId.LEFT_WRIST, JointId.LEFT_WRIST, 0.006f, 0.506f), // hand approx at wrist
        SegmentMass(JointId.RIGHT_WRIST, JointId.RIGHT_WRIST, 0.006f, 0.506f),
        SegmentMass(JointId.LEFT_HIP, JointId.LEFT_KNEE, 0.10f, 0.433f),
        SegmentMass(JointId.RIGHT_HIP, JointId.RIGHT_KNEE, 0.10f, 0.433f),
        SegmentMass(JointId.LEFT_KNEE, JointId.LEFT_ANKLE, 0.0465f, 0.433f),
        SegmentMass(JointId.RIGHT_KNEE, JointId.RIGHT_ANKLE, 0.0465f, 0.433f),
        SegmentMass(JointId.LEFT_ANKLE, JointId.LEFT_FOOT, 0.0145f, 0.5f),
        SegmentMass(JointId.RIGHT_ANKLE, JointId.RIGHT_FOOT, 0.0145f, 0.5f)
    )

    data class ComResult(
        val comWorld: Offset,
        val midFootWorld: Offset,
        val comOverMidFootDistance: Float,
        val isBalanced: Boolean,
        val leftFoot: Offset,
        val rightFoot: Offset
    )

    fun calculate(skeleton: SolvedSkeleton): ComResult {
        var totalMass = 0f
        var weightedX = 0f
        var weightedY = 0f

        for (seg in SEGMENTS) {
            val parentPos = skeleton.getWorldPosition(seg.parentJoint)
            val childPos = if (seg.parentJoint == seg.childJoint) {
                // hand case: same joint, use parent only
                parentPos
            } else {
                skeleton.getWorldPosition(seg.childJoint)
            }

            val comX = parentPos.x + (childPos.x - parentPos.x) * seg.comFractionFromParent
            val comY = parentPos.y + (childPos.y - parentPos.y) * seg.comFractionFromParent

            weightedX += comX * seg.massFraction
            weightedY += comY * seg.massFraction
            totalMass += seg.massFraction
        }

        val com = if (totalMass > 0f) Offset(weightedX / totalMass, weightedY / totalMass) else Offset(0.5f, 0.5f)

        val leftFoot = skeleton.getWorldPosition(JointId.LEFT_FOOT)
        val rightFoot = skeleton.getWorldPosition(JointId.RIGHT_FOOT)
        val midFoot = Offset((leftFoot.x + rightFoot.x) * 0.5f, (leftFoot.y + rightFoot.y) * 0.5f)

        val dist = hypot((com.x - midFoot.x).toDouble(), (com.y - midFoot.y).toDouble()).toFloat()

        // Balanced if COM within 0.15 normalized units of mid-foot horizontally for standing
        // For vertical distance, COM should be above feet
        val horizDist = kotlin.math.abs(com.x - midFoot.x)
        val isBalanced = horizDist < 0.15f

        return ComResult(
            comWorld = com,
            midFootWorld = midFoot,
            comOverMidFootDistance = dist,
            isBalanced = isBalanced,
            leftFoot = leftFoot,
            rightFoot = rightFoot
        )
    }

    /**
     * Calculates required torso inclination to balance squat.
     * Example: hips move back, knees forward, torso inclines naturally, COM over mid-foot.
     *
     * Simple rule: for given hip flexion and knee flexion, compute COM, then adjust chest flexion to bring COM over mid-foot.
     *
     * Returns torso inclination correction in degrees.
     */
    fun calculateSquatTorsoCorrection(
        skeleton: SolvedSkeleton,
        targetMidFootX: Float = 0.5f
    ): Float {
        val comResult = calculate(skeleton)
        val errorX = comResult.comWorld.x - targetMidFootX
        // If COM behind mid-foot (error negative), lean forward (positive chest flexion)
        // If COM ahead (positive), lean back (negative)
        // Proportional correction: 100 degrees per 0.15 normalized error
        val correction = -errorX * (100f / 0.15f)
        return correction.coerceIn(-20f, 30f)
    }

    fun calculateDeadliftShoulderOverBarCorrection(
        skeleton: SolvedSkeleton,
        barX: Float
    ): Float {
        // Shoulders should remain over bar in deadlift start
        val leftShoulder = skeleton.getWorldPosition(JointId.LEFT_SHOULDER)
        val rightShoulder = skeleton.getWorldPosition(JointId.RIGHT_SHOULDER)
        val midShoulderX = (leftShoulder.x + rightShoulder.x) * 0.5f
        val error = midShoulderX - barX
        // If shoulders behind bar, lean forward
        return -error * 80f
    }
}
