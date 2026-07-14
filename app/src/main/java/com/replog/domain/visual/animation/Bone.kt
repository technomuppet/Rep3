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
 * RC23 Corrections:
 * - Rectified bone defaultOrientationDegrees to be mathematically correct relative values.
 * - This resolves the severe twist/ pretzel distortion in hierarchical forward kinematics solver
 *   and ensures that at joint rotations = 0f, the body stands perfectly upright.
 */
object BoneCatalog {
    val SPINE_LOWER = Bone("SPINE_LOWER", JointId.PELVIS, JointId.CHEST, -90f, Anthropometry.TORSO_LOWER_HEIGHT, 16f, 0.27f)
    val SPINE_UPPER = Bone("SPINE_UPPER", JointId.CHEST, JointId.UPPER_CHEST, 0f, Anthropometry.TORSO_UPPER_HEIGHT * 0.85f, 16f, 0.16f)
    val NECK_SEGMENT = Bone("NECK_SEGMENT", JointId.UPPER_CHEST, JointId.NECK, 0f, Anthropometry.NECK_HEIGHT, 12f, 0.02f)
    val HEAD_SEGMENT = Bone("HEAD_SEGMENT", JointId.NECK, JointId.HEAD, 0f, Anthropometry.HEAD_HEIGHT, 20f, 0.081f)

    // Clavicles go horizontally to left and right from UPPER_CHEST (which has straight UP -90f world angle)
    val L_CLAVICLE = Bone("L_CLAVICLE", JointId.UPPER_CHEST, JointId.LEFT_SHOULDER, -90f, Anthropometry.CLAVICLE_LENGTH, 14f, 0.02f)
    val R_CLAVICLE = Bone("R_CLAVICLE", JointId.UPPER_CHEST, JointId.RIGHT_SHOULDER, 90f, Anthropometry.CLAVICLE_LENGTH, 14f, 0.02f)

    // Arms default downwards (world angle 90f)
    val L_UPPER_ARM = Bone("L_UPPER_ARM", JointId.LEFT_SHOULDER, JointId.LEFT_ELBOW, -90f, Anthropometry.UPPER_ARM_LENGTH, 12f, 0.028f)
    val R_UPPER_ARM = Bone("R_UPPER_ARM", JointId.RIGHT_SHOULDER, JointId.RIGHT_ELBOW, 90f, Anthropometry.UPPER_ARM_LENGTH, 12f, 0.028f)
    val L_FOREARM = Bone("L_FOREARM", JointId.LEFT_ELBOW, JointId.LEFT_WRIST, 0f, Anthropometry.FOREARM_LENGTH, 10f, 0.016f)
    val R_FOREARM = Bone("R_FOREARM", JointId.RIGHT_ELBOW, JointId.RIGHT_WRIST, 0f, Anthropometry.FOREARM_LENGTH, 10f, 0.016f)

    // Pelvic links: left hip down-left 135 deg, right down-right 45 deg from Pelvis
    val L_PELVIC_LINK = Bone("L_PELVIC_LINK", JointId.PELVIS, JointId.LEFT_HIP, 135f, Anthropometry.PELVIS_BREADTH * 0.5f, 16f, 0.05f)
    val R_PELVIC_LINK = Bone("R_PELVIC_LINK", JointId.PELVIS, JointId.RIGHT_HIP, 45f, Anthropometry.PELVIS_BREADTH * 0.5f, 16f, 0.05f)

    // Thighs point straight downwards (world angle 90f)
    val L_THIGH = Bone("L_THIGH", JointId.LEFT_HIP, JointId.LEFT_KNEE, -45f, Anthropometry.THIGH_LENGTH, 15f, 0.10f)
    val R_THIGH = Bone("R_THIGH", JointId.RIGHT_HIP, JointId.RIGHT_KNEE, 45f, Anthropometry.THIGH_LENGTH, 15f, 0.10f)
    val L_SHIN = Bone("L_SHIN", JointId.LEFT_KNEE, JointId.LEFT_ANKLE, 0f, Anthropometry.SHANK_LENGTH, 12f, 0.0465f)
    val R_SHIN = Bone("R_SHIN", JointId.RIGHT_KNEE, JointId.RIGHT_ANKLE, 0f, Anthropometry.SHANK_LENGTH, 12f, 0.0465f)
    
    // Feet point horizontally to the right (world angle 0f)
    val L_FOOT_BONE = Bone("L_FOOT_BONE", JointId.LEFT_ANKLE, JointId.LEFT_FOOT, -90f, Anthropometry.FOOT_LENGTH, 10f, 0.0145f)
    val R_FOOT_BONE = Bone("R_FOOT_BONE", JointId.RIGHT_ANKLE, JointId.RIGHT_FOOT, -90f, Anthropometry.FOOT_LENGTH, 10f, 0.0145f)

    val ALL_BONES: List<Bone> = listOf(
        SPINE_LOWER, SPINE_UPPER, NECK_SEGMENT, HEAD_SEGMENT,
        L_CLAVICLE, R_CLAVICLE, L_UPPER_ARM, R_UPPER_ARM, L_FOREARM, R_FOREARM,
        L_PELVIC_LINK, R_PELVIC_LINK, L_THIGH, R_THIGH, L_SHIN, R_SHIN, L_FOOT_BONE, R_FOOT_BONE
    )

    fun getBoneToChild(childId: JointId): Bone? = ALL_BONES.find { it.childJoint == childId }
}
