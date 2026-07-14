package com.replog.domain.visual.animation

import com.replog.domain.visual.body.Anthropometry

/**
 * Immutable anatomical bone segment connecting a parent joint to a child joint.
 * Stores normalized lengths relative to total human height and default orientation angles.
 * Canonical Source of Truth: REPLOG - BODY JOINT WORKSHEET.
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
 * Authoritative anatomical bone catalog based on the 15-point (24-joint bilateral) body rig.
 * Includes transitional compatibility definitions to guarantee continuous compilation.
 */
object BoneCatalog {
    // --- AUTHORITATIVE 15-POINT BODY RIG BONES ---
    val NEW_SPINE_LOWER = Bone("SPINE_LOWER_NEW", JointId.PELVIS, JointId.LOWER_SPINE, -90f, Anthropometry.TORSO_LOWER_HEIGHT * 0.4f, 16f, 0.12f)
    val NEW_SPINE_MID = Bone("SPINE_MID_NEW", JointId.LOWER_SPINE, JointId.MID_SPINE, 0f, Anthropometry.TORSO_LOWER_HEIGHT * 0.6f, 16f, 0.15f)
    val NEW_SPINE_UPPER = Bone("SPINE_UPPER_NEW", JointId.MID_SPINE, JointId.UPPER_SPINE, 0f, Anthropometry.TORSO_UPPER_HEIGHT * 0.85f, 16f, 0.16f)
    val NEW_NECK_SEGMENT = Bone("NECK_SEGMENT_NEW", JointId.UPPER_SPINE, JointId.NECK, 0f, Anthropometry.NECK_HEIGHT, 12f, 0.02f)
    val NEW_HEAD_SEGMENT = Bone("HEAD_SEGMENT_NEW", JointId.NECK, JointId.HEAD, 0f, Anthropometry.HEAD_HEIGHT, 20f, 0.081f)

    // Clavicles go horizontally to left and right from UPPER_SPINE
    val NEW_L_CLAVICLE = Bone("L_CLAVICLE_NEW", JointId.UPPER_SPINE, JointId.LEFT_SHOULDER, -90f, Anthropometry.CLAVICLE_LENGTH, 14f, 0.02f)
    val NEW_R_CLAVICLE = Bone("R_CLAVICLE_NEW", JointId.UPPER_SPINE, JointId.RIGHT_SHOULDER, 90f, Anthropometry.CLAVICLE_LENGTH, 14f, 0.02f)

    // Arms
    val NEW_L_UPPER_ARM = Bone("L_UPPER_ARM_NEW", JointId.LEFT_SHOULDER, JointId.LEFT_ELBOW, -90f, Anthropometry.UPPER_ARM_LENGTH, 12f, 0.028f)
    val NEW_R_UPPER_ARM = Bone("R_UPPER_ARM_NEW", JointId.RIGHT_SHOULDER, JointId.RIGHT_ELBOW, 90f, Anthropometry.UPPER_ARM_LENGTH, 12f, 0.028f)
    val NEW_L_FOREARM = Bone("L_FOREARM_NEW", JointId.LEFT_ELBOW, JointId.LEFT_WRIST, 0f, Anthropometry.FOREARM_LENGTH, 10f, 0.016f)
    val NEW_R_FOREARM = Bone("R_FOREARM_NEW", JointId.RIGHT_ELBOW, JointId.RIGHT_WRIST, 0f, Anthropometry.FOREARM_LENGTH, 10f, 0.016f)
    val NEW_L_HAND = Bone("L_HAND_NEW", JointId.LEFT_WRIST, JointId.LEFT_HAND, 0f, Anthropometry.HAND_LENGTH * 0.3f, 8f, 0.006f)
    val NEW_R_HAND = Bone("R_HAND_NEW", JointId.RIGHT_WRIST, JointId.RIGHT_HAND, 0f, Anthropometry.HAND_LENGTH * 0.3f, 8f, 0.006f)

    // Pelvic links
    val NEW_L_PELVIC_LINK = Bone("L_PELVIC_LINK_NEW", JointId.PELVIS, JointId.LEFT_HIP, 135f, Anthropometry.PELVIS_BREADTH * 0.5f, 16f, 0.05f)
    val NEW_R_PELVIC_LINK = Bone("R_PELVIC_LINK_NEW", JointId.PELVIS, JointId.RIGHT_HIP, 45f, Anthropometry.PELVIS_BREADTH * 0.5f, 16f, 0.05f)

    // Thighs
    val NEW_L_THIGH = Bone("L_THIGH_NEW", JointId.LEFT_HIP, JointId.LEFT_KNEE, -45f, Anthropometry.THIGH_LENGTH, 15f, 0.10f)
    val NEW_R_THIGH = Bone("R_THIGH_NEW", JointId.RIGHT_HIP, JointId.RIGHT_KNEE, 45f, Anthropometry.THIGH_LENGTH, 15f, 0.10f)
    val NEW_L_SHIN = Bone("L_SHIN_NEW", JointId.LEFT_KNEE, JointId.LEFT_ANKLE, 0f, Anthropometry.SHANK_LENGTH, 12f, 0.0465f)
    val NEW_R_SHIN = Bone("R_SHIN_NEW", JointId.RIGHT_KNEE, JointId.RIGHT_ANKLE, 0f, Anthropometry.SHANK_LENGTH, 12f, 0.0465f)

    // Feet (Heel and Toe)
    val NEW_L_HEEL = Bone("L_HEEL_NEW", JointId.LEFT_ANKLE, JointId.LEFT_HEEL, -90f, Anthropometry.FOOT_LENGTH * 0.3f, 10f, 0.005f)
    val NEW_R_HEEL = Bone("R_HEEL_NEW", JointId.RIGHT_ANKLE, JointId.RIGHT_HEEL, 90f, Anthropometry.FOOT_LENGTH * 0.3f, 10f, 0.005f)
    val NEW_L_TOE = Bone("L_TOE_NEW", JointId.LEFT_ANKLE, JointId.LEFT_TOE, 90f, Anthropometry.FOOT_LENGTH * 0.7f, 10f, 0.0095f)
    val NEW_R_TOE = Bone("R_TOE_NEW", JointId.RIGHT_ANKLE, JointId.RIGHT_TOE, -90f, Anthropometry.FOOT_LENGTH * 0.7f, 10f, 0.0095f)

    // --- TRANSITIONAL COMPATIBILITY DEPRECATED BONES ---
    @Deprecated("Superseded by NEW_SPINE_LOWER / NEW_SPINE_MID")
    val SPINE_LOWER = Bone("SPINE_LOWER", JointId.PELVIS, JointId.CHEST, -90f, Anthropometry.TORSO_LOWER_HEIGHT, 16f, 0.27f)
    @Deprecated("Superseded by NEW_SPINE_UPPER")
    val SPINE_UPPER = Bone("SPINE_UPPER", JointId.CHEST, JointId.UPPER_CHEST, 0f, Anthropometry.TORSO_UPPER_HEIGHT * 0.85f, 16f, 0.16f)
    @Deprecated("Superseded by NEW_NECK_SEGMENT")
    val NECK_SEGMENT = Bone("NECK_SEGMENT", JointId.UPPER_CHEST, JointId.NECK, 0f, Anthropometry.NECK_HEIGHT, 12f, 0.02f)
    @Deprecated("Superseded by NEW_HEAD_SEGMENT")
    val HEAD_SEGMENT = Bone("HEAD_SEGMENT", JointId.NECK, JointId.HEAD, 0f, Anthropometry.HEAD_HEIGHT, 20f, 0.081f)
    @Deprecated("Superseded by NEW_L_CLAVICLE")
    val L_CLAVICLE = Bone("L_CLAVICLE", JointId.UPPER_CHEST, JointId.LEFT_SHOULDER, -90f, Anthropometry.CLAVICLE_LENGTH, 14f, 0.02f)
    @Deprecated("Superseded by NEW_R_CLAVICLE")
    val R_CLAVICLE = Bone("R_CLAVICLE", JointId.UPPER_CHEST, JointId.RIGHT_SHOULDER, 90f, Anthropometry.CLAVICLE_LENGTH, 14f, 0.02f)
    @Deprecated("Superseded by NEW_L_UPPER_ARM")
    val L_UPPER_ARM = Bone("L_UPPER_ARM", JointId.LEFT_SHOULDER, JointId.LEFT_ELBOW, -90f, Anthropometry.UPPER_ARM_LENGTH, 12f, 0.028f)
    @Deprecated("Superseded by NEW_R_UPPER_ARM")
    val R_UPPER_ARM = Bone("R_UPPER_ARM", JointId.RIGHT_SHOULDER, JointId.RIGHT_ELBOW, 90f, Anthropometry.UPPER_ARM_LENGTH, 12f, 0.028f)
    @Deprecated("Superseded by NEW_L_FOREARM")
    val L_FOREARM = Bone("L_FOREARM", JointId.LEFT_ELBOW, JointId.LEFT_WRIST, 0f, Anthropometry.FOREARM_LENGTH, 10f, 0.016f)
    @Deprecated("Superseded by NEW_R_FOREARM")
    val R_FOREARM = Bone("R_FOREARM", JointId.RIGHT_ELBOW, JointId.RIGHT_WRIST, 0f, Anthropometry.FOREARM_LENGTH, 10f, 0.016f)
    @Deprecated("Superseded by NEW_L_PELVIC_LINK")
    val L_PELVIC_LINK = Bone("L_PELVIC_LINK", JointId.PELVIS, JointId.LEFT_HIP, 135f, Anthropometry.PELVIS_BREADTH * 0.5f, 16f, 0.05f)
    @Deprecated("Superseded by NEW_R_PELVIC_LINK")
    val R_PELVIC_LINK = Bone("R_PELVIC_LINK", JointId.PELVIS, JointId.RIGHT_HIP, 45f, Anthropometry.PELVIS_BREADTH * 0.5f, 16f, 0.05f)
    @Deprecated("Superseded by NEW_L_THIGH")
    val L_THIGH = Bone("L_THIGH", JointId.LEFT_HIP, JointId.LEFT_KNEE, -45f, Anthropometry.THIGH_LENGTH, 15f, 0.10f)
    @Deprecated("Superseded by NEW_R_THIGH")
    val R_THIGH = Bone("R_THIGH", JointId.RIGHT_HIP, JointId.RIGHT_KNEE, 45f, Anthropometry.THIGH_LENGTH, 15f, 0.10f)
    @Deprecated("Superseded by NEW_L_SHIN")
    val L_SHIN = Bone("L_SHIN", JointId.LEFT_KNEE, JointId.LEFT_ANKLE, 0f, Anthropometry.SHANK_LENGTH, 12f, 0.0465f)
    @Deprecated("Superseded by NEW_R_SHIN")
    val R_SHIN = Bone("R_SHIN", JointId.RIGHT_KNEE, JointId.RIGHT_ANKLE, 0f, Anthropometry.SHANK_LENGTH, 12f, 0.0465f)
    @Deprecated("Superseded by NEW_L_HEEL / NEW_L_TOE")
    val L_FOOT_BONE = Bone("L_FOOT_BONE", JointId.LEFT_ANKLE, JointId.LEFT_FOOT, 90f, Anthropometry.FOOT_LENGTH, 10f, 0.0145f)
    @Deprecated("Superseded by NEW_R_HEEL / NEW_R_TOE")
    val R_FOOT_BONE = Bone("R_FOOT_BONE", JointId.RIGHT_ANKLE, JointId.RIGHT_FOOT, -90f, Anthropometry.FOOT_LENGTH, 10f, 0.0145f)

    val ALL_BONES: List<Bone> = listOf(
        NEW_SPINE_LOWER, NEW_SPINE_MID, NEW_SPINE_UPPER, NEW_NECK_SEGMENT, NEW_HEAD_SEGMENT,
        NEW_L_CLAVICLE, NEW_R_CLAVICLE, NEW_L_UPPER_ARM, NEW_R_UPPER_ARM, NEW_L_FOREARM, NEW_R_FOREARM,
        NEW_L_HAND, NEW_R_HAND, NEW_L_PELVIC_LINK, NEW_R_PELVIC_LINK, NEW_L_THIGH, NEW_R_THIGH, NEW_L_SHIN, NEW_R_SHIN,
        NEW_L_HEEL, NEW_R_HEEL, NEW_L_TOE, NEW_R_TOE,

        // Legacy list elements retained for compatibility
        SPINE_LOWER, SPINE_UPPER, NECK_SEGMENT, HEAD_SEGMENT,
        L_CLAVICLE, R_CLAVICLE, L_UPPER_ARM, R_UPPER_ARM, L_FOREARM, R_FOREARM,
        L_PELVIC_LINK, R_PELVIC_LINK, L_THIGH, R_THIGH, L_SHIN, R_SHIN, L_FOOT_BONE, R_FOOT_BONE
    )

    fun getBoneToChild(childId: JointId): Bone? = ALL_BONES.find { it.childJoint == childId }
}
