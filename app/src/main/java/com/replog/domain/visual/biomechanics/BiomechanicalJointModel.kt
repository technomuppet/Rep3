package com.replog.domain.visual.biomechanics

import com.replog.domain.visual.animation.JointId

/**
 * Commercial biomechanical joint model with realistic anatomical limits.
 * Each joint has type and per-axis constraints, not single float.
 */

enum class JointType {
    HINGE,
    BALL,
    UNIVERSAL,
    SCAPULA,
    FIXED
}

data class AxisLimit(
    val minDegrees: Float,
    val maxDegrees: Float,
    val neutralDegrees: Float = 0f
) {
    fun clamp(value: Float): Float = value.coerceIn(minDegrees, maxDegrees)
    fun isValid(value: Float): Boolean = value in minDegrees..maxDegrees
}

data class JointLimits(
    val flexion: AxisLimit, // sagittal plane
    val abduction: AxisLimit? = null, // frontal plane
    val rotation: AxisLimit? = null // transverse plane
)

object BiomechanicalJointModel {

    // PELVIS: anterior/posterior tilt -90..90, lateral tilt -15..15, rotation -45..45
    val PELVIS = JointLimits(
        flexion = AxisLimit(-90f, 90f, 0f),
        abduction = AxisLimit(-15f, 15f, 0f),
        rotation = AxisLimit(-45f, 45f, 0f)
    )

    // LUMBAR (CHEST / LOWER_SPINE): flexion -45..60, lateral -25..25, rotation -30..30
    val LUMBAR = JointLimits(
        flexion = AxisLimit(-45f, 60f, 0f),
        abduction = AxisLimit(-25f, 25f, 0f),
        rotation = AxisLimit(-30f, 30f, 0f)
    )

    // THORACIC (UPPER_CHEST / MID_SPINE / UPPER_SPINE): flexion -20..50, lateral -20..20, rotation -40..40
    val THORACIC = JointLimits(
        flexion = AxisLimit(-20f, 50f, 0f),
        abduction = AxisLimit(-20f, 20f, 0f),
        rotation = AxisLimit(-40f, 40f, 0f)
    )

    // CERVICAL (NECK): flexion -45..45, lateral -45..45, rotation -70..70
    val CERVICAL = JointLimits(
        flexion = AxisLimit(-45f, 45f, 0f),
        abduction = AxisLimit(-45f, 45f, 0f),
        rotation = AxisLimit(-70f, 70f, 0f)
    )

    // HEAD: similar to cervical but more limited
    val HEAD = JointLimits(
        flexion = AxisLimit(-30f, 30f, 0f),
        abduction = AxisLimit(-20f, 20f, 0f),
        rotation = AxisLimit(-50f, 50f, 0f)
    )

    // SCAPULA implicit via shoulder: retraction -20..15, elevation -10..45
    val SCAPULA = JointLimits(
        flexion = AxisLimit(-20f, 15f, 0f), // retraction/protraction
        abduction = AxisLimit(-10f, 45f, 0f), // elevation/depression
        rotation = AxisLimit(-20f, 60f, 0f) // upward rotation
    )

    // CLAVICLE: elevation -10..45, retraction -20..20
    val CLAVICLE = JointLimits(
        flexion = AxisLimit(-10f, 45f, 0f),
        abduction = AxisLimit(-20f, 20f, 0f)
    )

    // SHOULDER: flexion -180..90, abduction -10..150, rotation -90..90
    val SHOULDER = JointLimits(
        flexion = AxisLimit(-180f, 90f, 0f),
        abduction = AxisLimit(-10f, 150f, 0f),
        rotation = AxisLimit(-90f, 90f, 0f)
    )

    // ELBOW: flexion 0..145, no hyperextension beyond -5
    val ELBOW = JointLimits(
        flexion = AxisLimit(-5f, 145f, 0f)
    )

    // FOREARM ROTATION (pronation/supination): -85..85
    val FOREARM = JointLimits(
        flexion = AxisLimit(-85f, 85f, 0f)
    )

    // WRIST: flexion -70..70, radial -20..20 ulnar, but simplified
    val WRIST = JointLimits(
        flexion = AxisLimit(-70f, 70f, 0f),
        abduction = AxisLimit(-20f, 30f, 0f)
    )

    // HIP: flexion -135..45, extension -30, abduction -30..45, rotation -45..45
    val HIP = JointLimits(
        flexion = AxisLimit(-135f, 45f, 0f),
        abduction = AxisLimit(-30f, 45f, 0f),
        rotation = AxisLimit(-45f, 45f, 0f)
    )

    // KNEE: flexion 0..140, no abduction, rotation slight -10..10 when flexed
    val KNEE = JointLimits(
        flexion = AxisLimit(0f, 140f, 0f),
        rotation = AxisLimit(-10f, 10f, 0f)
    )

    // ANKLE: dorsiflexion -20..50 plantarflexion, but our sign: dorsiflexion 20, plantar -50
    val ANKLE = JointLimits(
        flexion = AxisLimit(-50f, 20f, 0f), // negative plantar, positive dorsiflexion
        abduction = AxisLimit(-15f, 15f, 0f) // inversion/eversion
    )

    // FOOT: toe flexion -40..0
    val FOOT = JointLimits(
        flexion = AxisLimit(-40f, 15f, 0f)
    )

    fun getLimits(jointId: JointId): JointLimits {
        return when (jointId) {
            JointId.PELVIS -> PELVIS
            JointId.LOWER_SPINE -> LUMBAR
            JointId.MID_SPINE -> THORACIC
            JointId.UPPER_SPINE -> THORACIC
            JointId.CHEST -> LUMBAR
            JointId.UPPER_CHEST -> THORACIC
            JointId.NECK -> CERVICAL
            JointId.HEAD -> HEAD
            JointId.LEFT_SHOULDER, JointId.RIGHT_SHOULDER -> SHOULDER
            JointId.LEFT_ELBOW, JointId.RIGHT_ELBOW -> ELBOW
            JointId.LEFT_WRIST, JointId.RIGHT_WRIST -> WRIST
            JointId.LEFT_HAND, JointId.RIGHT_HAND -> WRIST
            JointId.LEFT_HIP, JointId.RIGHT_HIP -> HIP
            JointId.LEFT_KNEE, JointId.RIGHT_KNEE -> KNEE
            JointId.LEFT_ANKLE, JointId.RIGHT_ANKLE -> ANKLE
            JointId.LEFT_HEEL, JointId.RIGHT_HEEL -> ANKLE
            JointId.LEFT_TOE, JointId.RIGHT_TOE -> FOOT
            JointId.LEFT_FOOT, JointId.RIGHT_FOOT -> FOOT
        }
    }

    fun getJointType(jointId: JointId): JointType {
        return when (jointId) {
            JointId.PELVIS -> JointType.UNIVERSAL
            JointId.LOWER_SPINE -> JointType.UNIVERSAL
            JointId.MID_SPINE -> JointType.UNIVERSAL
            JointId.UPPER_SPINE -> JointType.UNIVERSAL
            JointId.CHEST -> JointType.UNIVERSAL
            JointId.UPPER_CHEST -> JointType.UNIVERSAL
            JointId.NECK -> JointType.BALL
            JointId.HEAD -> JointType.BALL
            JointId.LEFT_SHOULDER, JointId.RIGHT_SHOULDER -> JointType.BALL
            JointId.LEFT_ELBOW, JointId.RIGHT_ELBOW -> JointType.HINGE
            JointId.LEFT_WRIST, JointId.RIGHT_WRIST -> JointType.UNIVERSAL
            JointId.LEFT_HAND, JointId.RIGHT_HAND -> JointType.UNIVERSAL
            JointId.LEFT_HIP, JointId.RIGHT_HIP -> JointType.BALL
            JointId.LEFT_KNEE, JointId.RIGHT_KNEE -> JointType.HINGE
            JointId.LEFT_ANKLE, JointId.RIGHT_ANKLE -> JointType.UNIVERSAL
            JointId.LEFT_HEEL, JointId.RIGHT_HEEL -> JointType.UNIVERSAL
            JointId.LEFT_TOE, JointId.RIGHT_TOE -> JointType.HINGE
            JointId.LEFT_FOOT, JointId.RIGHT_FOOT -> JointType.HINGE
        }
    }

    /**
     * Validates if rotation is within realistic limits.
     */
    fun isValid(jointId: JointId, rotationDegrees: Float): Boolean {
        val limits = getLimits(jointId)
        return limits.flexion.isValid(rotationDegrees)
    }

    /**
     * Clamps to realistic limits.
     */
    fun clamp(jointId: JointId, rotationDegrees: Float): Float {
        return getLimits(jointId).flexion.clamp(rotationDegrees)
    }
}
