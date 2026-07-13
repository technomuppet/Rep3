package com.replog.domain.visual.orientation

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.animation.SolvedJoint
import com.replog.domain.visual.animation.SolvedSkeleton
import com.replog.domain.visual.spec.BodyOrientation
import com.replog.domain.visual.spec.SupportType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Body orientation system supporting:
 * - Standing
 * - Seated
 * - Supine
 * - Prone
 * - Incline
 * - Decline
 * - Hanging
 *
 * Automatically orients the body correctly without fake rotations.
 * Uses 2D rotation mathematics around pelvis for upper body vs lower body decoupling
 * to keep feet planted where appropriate.
 */
object BodyOrientationEngine {

    data class OrientedSkeleton(
        val skeleton: SolvedSkeleton,
        val benchAngleDegrees: Float,
        val pelvisScreenShift: Offset,
        val upperBodyRotationDeg: Float
    )

    private const val DEG_TO_RAD = (Math.PI / 180.0).toFloat()

    /**
     * Transforms world positions based on body orientation and support type.
     * Returns new SolvedSkeleton with rotated positions.
     *
     * Key principles:
     * - Feet remain planted for standing/seated (no upper body rotation affects feet)
     * - For supine/prone, upper body rotates -90/+90 around chest or pelvis, while lower body stays
     *   semi-vertical to keep feet on floor (bench press: legs bent feet flat)
     * - Incline/decline rotates upper body +30/-15 around pelvis with bench angle
     * - Hanging: pelvis suspended, arms overhead, no foot planting requirement relaxed
     */
    fun orient(
        skeleton: SolvedSkeleton,
        bodyOrientation: BodyOrientation,
        supportType: SupportType,
        benchAngle: Float
    ): SolvedSkeleton {
        if (bodyOrientation == BodyOrientation.STANDING && supportType == SupportType.STANDING) {
            return skeleton // no transform needed
        }

        val pelvisOrig = skeleton.getWorldPosition(JointId.PELVIS)

        // Determine rotation angles per body part
        val (upperRotationDeg, lowerRotationDeg, rootShift) = when (bodyOrientation) {
            BodyOrientation.STANDING -> Triple(0f, 0f, Offset(0f, 0f))
            BodyOrientation.SEATED -> Triple(0f, 0f, Offset(0f, 0.08f)) // pelvis lower
            BodyOrientation.SUPINE -> {
                // For flat bench, upper body horizontal head left => -90 deg around pelvis/chest
                // Lower body: thighs horizontal? For bench press legs bent feet flat, we keep thighs slightlyangled
                // Upper -90, lower 0- small? Actually keep lower vertical
                Triple(-90f, 0f, Offset(0f, 0.05f))
            }
            BodyOrientation.PRONE -> Triple(90f, 0f, Offset(0f, 0.05f))
            BodyOrientation.SIDE_LYING -> Triple(-90f, -10f, Offset(0f, 0.05f))
            BodyOrientation.HANGING -> Triple(0f, 0f, Offset(0f, -0.15f)) // pelvis higher
            BodyOrientation.INVERTED -> Triple(180f, 180f, Offset(0f, -0.2f))
            BodyOrientation.KNEELING -> Triple(0f, -20f, Offset(0f, 0.1f))
            else -> Triple(0f, 0f, Offset(0f, 0f))
        }

        // Adjust incline/decline: add bench angle to upper rotation
        val adjustedUpperRot = when (supportType) {
            SupportType.SEATED_INCLINE -> upperRotationDeg + 30f + benchAngle // benchAngle 30
            SupportType.SEATED_DECLINE -> upperRotationDeg - 15f + benchAngle // benchAngle -15
            SupportType.SUPINE_LYING -> if (benchAngle != 0f) benchAngle else upperRotationDeg
            else -> upperRotationDeg + benchAngle // benchAngle may be 0
        }

        // If no rotation, just shift root
        if (adjustedUpperRot == 0f && lowerRotationDeg == 0f && rootShift == Offset(0f, 0f)) {
            return skeleton
        }

        // Rotate joints: upper body = head, neck, chest, upperChest, shoulders, elbows, wrists
        // Lower body = hips, knees, ankles, feet
        // Pelvis is pivot
        val upperBodyJoints = setOf(
            JointId.CHEST, JointId.UPPER_CHEST, JointId.NECK, JointId.HEAD,
            JointId.LEFT_SHOULDER, JointId.RIGHT_SHOULDER,
            JointId.LEFT_ELBOW, JointId.RIGHT_ELBOW,
            JointId.LEFT_WRIST, JointId.RIGHT_WRIST
        )
        val lowerBodyJoints = setOf(
            JointId.LEFT_HIP, JointId.RIGHT_HIP,
            JointId.LEFT_KNEE, JointId.RIGHT_KNEE,
            JointId.LEFT_ANKLE, JointId.RIGHT_ANKLE,
            JointId.LEFT_FOOT, JointId.RIGHT_FOOT
        )

        val newJoints = mutableMapOf<JointId, SolvedJoint>()

        // Pelvis stays as pivot plus shift
        val pelvisJoint = skeleton.getJoint(JointId.PELVIS)
        if (pelvisJoint != null) {
            val shiftedWorld = Offset(pelvisOrig.x + rootShift.x, pelvisOrig.y + rootShift.y)
            newJoints[JointId.PELVIS] = pelvisJoint.copy(worldPositionOffset = shiftedWorld)
        }

        // Transform upper body
        for (jointId in upperBodyJoints) {
            val solved = skeleton.getJoint(jointId) ?: continue
            val rotated = rotateAroundPivot(solved.worldPositionOffset, pelvisOrig, adjustedUpperRot)
            val shifted = Offset(rotated.x + rootShift.x, rotated.y + rootShift.y)
            newJoints[jointId] = solved.copy(worldPositionOffset = shifted)
        }

        // Transform lower body
        for (jointId in lowerBodyJoints) {
            val solved = skeleton.getJoint(jointId) ?: continue
            val rotated = rotateAroundPivot(solved.worldPositionOffset, pelvisOrig, lowerRotationDeg)
            val shifted = Offset(rotated.x + rootShift.x, rotated.y + rootShift.y)
            newJoints[jointId] = solved.copy(worldPositionOffset = shifted)
        }

        // Copy any remaining (should be none) but keep pelvis already
        for ((id, joint) in skeleton.joints) {
            if (!newJoints.containsKey(id)) {
                // For not in upper/lower (e.g., if pelvis already handled) keep with shift
                val shifted = Offset(joint.worldPositionOffset.x + rootShift.x, joint.worldPositionOffset.y + rootShift.y)
                newJoints[id] = joint.copy(worldPositionOffset = shifted)
            }
        }

        return SolvedSkeleton(
            rootPosition = Offset(skeleton.rootPosition.x + rootShift.x, skeleton.rootPosition.y + rootShift.y),
            scaleFactor = skeleton.scaleFactor,
            joints = newJoints
        )
    }

    private fun rotateAroundPivot(point: Offset, pivot: Offset, degrees: Float): Offset {
        if (degrees == 0f) return point
        val rad = degrees * DEG_TO_RAD
        val cosA = cos(rad)
        val sinA = sin(rad)
        val dx = point.x - pivot.x
        val dy = point.y - pivot.y
        val rx = dx * cosA - dy * sinA
        val ry = dx * sinA + dy * cosA
        return Offset(pivot.x + rx, pivot.y + ry)
    }

    /**
     * Returns bench angle to use for rendering based on spec.
     */
    fun resolveBenchAngle(supportType: SupportType, benchAngle: Float): Float {
        return when (supportType) {
            SupportType.SEATED_INCLINE -> 30f
            SupportType.SEATED_DECLINE -> -15f
            SupportType.SUPINE_LYING -> 0f
            else -> benchAngle
        }
    }
}
