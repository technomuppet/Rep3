package com.replog.domain.visual.animation

import com.replog.domain.visual.body.Anthropometry

/**
 * Immutable anatomical bone segment connecting a parent joint to a child joint.
 * RC20.3 — Updated to match Anthropometry commercial proportions (ANSUR).
 * Stores normalized lengths relative to total human height and default orientation angles.
 * renderThickness retained for backward compatibility but now derived from Anthropometry in renderer.
 */
data class Bone(
    val id: String,
    val parentJoint: JointId,
    val childJoint: JointId,
    val defaultOrientationDegrees: Float,
    val normalizedLength: Float,
    val renderThickness: Float,
    val massFraction: Float = 0.05f // for COM
) {
    fun scaleLength(scaleFactor: Float): Bone = copy(
        normalizedLength = normalizedLength * scaleFactor,
        renderThickness = renderThickness * scaleFactor
    )
}

/**
 * Commercial anatomical bone catalog with anthropometric-correct proportions.
 * Matches Anthropometry.kt for consistent FK world positions and rendering.
 *
 * Improvements from RC20 audit:
 * - Head height 0.13 vs old 0.08 (head too small)
 * - Thigh 0.245 vs 0.22, shank 0.245 vs 0.20, foot 0.15 vs 0.06 (legs/feet short)
 * - Upper arm 0.19 vs 0.14, forearm 0.16 vs 0.12 (arms short)
 * - Pelvic links now infero-lateral 135/45 deg not pure horizontal 180/0, more realistic hip joint inferior+ lateral from pelvis
 * - Spine proportions: lower 0.15 vs 0.12, upper 0.15 vs 0.10 for proper torso height 0.30 vs old 0.22 short torso
 */
object BoneCatalog {
    val SPINE_LOWER = Bone("SPINE_LOWER", JointId.PELVIS, JointId.CHEST, -90f, Anthropometry.TORSO_LOWER_HEIGHT, 16f, 0.27f)
    val SPINE_UPPER = Bone("SPINE_UPPER", JointId.CHEST, JointId.UPPER_CHEST, -90f, Anthropometry.TORSO_UPPER_HEIGHT * 0.85f, 16f, 0.16f)
    val NECK_SEGMENT = Bone("NECK_SEGMENT", JointId.UPPER_CHEST, JointId.NECK, -90f, Anthropometry.NECK_HEIGHT, 12f, 0.02f)
    val HEAD_SEGMENT = Bone("HEAD_SEGMENT", JointId.NECK, JointId.HEAD, -90f, Anthropometry.HEAD_HEIGHT, 20f, 0.081f)

    // Clavicle 0.09 each side good
    val L_CLAVICLE = Bone("L_CLAVICLE", JointId.UPPER_CHEST, JointId.LEFT_SHOULDER, 180f, Anthropometry.CLAVICLE_LENGTH, 14f, 0.02f)
    val R_CLAVICLE = Bone("R_CLAVICLE", JointId.UPPER_CHEST, JointId.RIGHT_SHOULDER, 0f, Anthropometry.CLAVICLE_LENGTH, 14f, 0.02f)

    // Upper arm 0.19, forearm 0.16 per Anthropometry
    val L_UPPER_ARM = Bone("L_UPPER_ARM", JointId.LEFT_SHOULDER, JointId.LEFT_ELBOW, 90f, Anthropometry.UPPER_ARM_LENGTH, 12f, 0.028f)
    val R_UPPER_ARM = Bone("R_UPPER_ARM", JointId.RIGHT_SHOULDER, JointId.RIGHT_ELBOW, 90f, Anthropometry.UPPER_ARM_LENGTH, 12f, 0.028f)
    val L_FOREARM = Bone("L_FOREARM", JointId.LEFT_ELBOW, JointId.LEFT_WRIST, 90f, Anthropometry.FOREARM_LENGTH, 10f, 0.016f)
    val R_FOREARM = Bone("R_FOREARM", JointId.RIGHT_ELBOW, JointId.RIGHT_WRIST, 90f, Anthropometry.FOREARM_LENGTH, 10f, 0.016f)

    // Pelvic links now infero-lateral: left hip down-left 135 deg, right down-right 45 deg, length = half pelvis breadth
    val L_PELVIC_LINK = Bone("L_PELVIC_LINK", JointId.PELVIS, JointId.LEFT_HIP, 135f, Anthropometry.PELVIS_BREADTH * 0.5f, 16f, 0.05f)
    val R_PELVIC_LINK = Bone("R_PELVIC_LINK", JointId.PELVIS, JointId.RIGHT_HIP, 45f, Anthropometry.PELVIS_BREADTH * 0.5f, 16f, 0.05f)

    val L_THIGH = Bone("L_THIGH", JointId.LEFT_HIP, JointId.LEFT_KNEE, 90f, Anthropometry.THIGH_LENGTH, 15f, 0.10f)
    val R_THIGH = Bone("R_THIGH", JointId.RIGHT_HIP, JointId.RIGHT_KNEE, 90f, Anthropometry.THIGH_LENGTH, 15f, 0.10f)
    val L_SHIN = Bone("L_SHIN", JointId.LEFT_KNEE, JointId.LEFT_ANKLE, 90f, Anthropometry.SHANK_LENGTH, 12f, 0.0465f)
    val R_SHIN = Bone("R_SHIN", JointId.RIGHT_KNEE, JointId.RIGHT_ANKLE, 90f, Anthropometry.SHANK_LENGTH, 12f, 0.0465f)
    val L_FOOT_BONE = Bone("L_FOOT_BONE", JointId.LEFT_ANKLE, JointId.LEFT_FOOT, 0f, Anthropometry.FOOT_LENGTH, 10f, 0.0145f)
    val R_FOOT_BONE = Bone("R_FOOT_BONE", JointId.RIGHT_ANKLE, JointId.RIGHT_FOOT, 0f, Anthropometry.FOOT_LENGTH, 10f, 0.0145f)

    val ALL_BONES: List<Bone> = listOf(
        SPINE_LOWER, SPINE_UPPER, NECK_SEGMENT, HEAD_SEGMENT,
        L_CLAVICLE, R_CLAVICLE, L_UPPER_ARM, R_UPPER_ARM, L_FOREARM, R_FOREARM,
        L_PELVIC_LINK, R_PELVIC_LINK, L_THIGH, R_THIGH, L_SHIN, R_SHIN, L_FOOT_BONE, R_FOOT_BONE
    )

    fun getBoneToChild(childId: JointId): Bone? = ALL_BONES.find { it.childJoint == childId }
}
