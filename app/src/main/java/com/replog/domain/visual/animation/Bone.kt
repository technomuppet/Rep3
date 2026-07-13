package com.replog.domain.visual.animation

/**
 * Immutable anatomical bone segment connecting a parent joint to a child joint.
 * Stores normalized lengths relative to total human height and default orientation angles.
 */
data class Bone(
    val id: String,
    val parentJoint: JointId,
    val childJoint: JointId,
    val defaultOrientationDegrees: Float,
    val normalizedLength: Float,
    val renderThickness: Float
) {
    /**
     * Returns a scaled copy of this bone segment proportional to the target height scale.
     */
    fun scaleLength(scaleFactor: Float): Bone = copy(
        normalizedLength = normalizedLength * scaleFactor,
        renderThickness = renderThickness * scaleFactor
    )
}

/**
 * Standard anatomical bone catalog defining rigid segment proportions for the human skeleton.
 */
object BoneCatalog {
    val SPINE_LOWER = Bone("SPINE_LOWER", JointId.PELVIS, JointId.CHEST, -90f, 0.12f, 16f)
    val SPINE_UPPER = Bone("SPINE_UPPER", JointId.CHEST, JointId.UPPER_CHEST, -90f, 0.10f, 16f)
    val NECK_SEGMENT = Bone("NECK_SEGMENT", JointId.UPPER_CHEST, JointId.NECK, -90f, 0.05f, 12f)
    val HEAD_SEGMENT = Bone("HEAD_SEGMENT", JointId.NECK, JointId.HEAD, -90f, 0.08f, 20f)

    val L_CLAVICLE = Bone("L_CLAVICLE", JointId.UPPER_CHEST, JointId.LEFT_SHOULDER, 180f, 0.09f, 14f)
    val R_CLAVICLE = Bone("R_CLAVICLE", JointId.UPPER_CHEST, JointId.RIGHT_SHOULDER, 0f, 0.09f, 14f)

    val L_UPPER_ARM = Bone("L_UPPER_ARM", JointId.LEFT_SHOULDER, JointId.LEFT_ELBOW, 90f, 0.14f, 12f)
    val R_UPPER_ARM = Bone("R_UPPER_ARM", JointId.RIGHT_SHOULDER, JointId.RIGHT_ELBOW, 90f, 0.14f, 12f)
    val L_FOREARM = Bone("L_FOREARM", JointId.LEFT_ELBOW, JointId.LEFT_WRIST, 90f, 0.12f, 10f)
    val R_FOREARM = Bone("R_FOREARM", JointId.RIGHT_ELBOW, JointId.RIGHT_WRIST, 90f, 0.12f, 10f)

    val L_PELVIC_LINK = Bone("L_PELVIC_LINK", JointId.PELVIS, JointId.LEFT_HIP, 180f, 0.06f, 16f)
    val R_PELVIC_LINK = Bone("R_PELVIC_LINK", JointId.PELVIS, JointId.RIGHT_HIP, 0f, 0.06f, 16f)

    val L_THIGH = Bone("L_THIGH", JointId.LEFT_HIP, JointId.LEFT_KNEE, 90f, 0.22f, 15f)
    val R_THIGH = Bone("R_THIGH", JointId.RIGHT_HIP, JointId.RIGHT_KNEE, 90f, 0.22f, 15f)
    val L_SHIN = Bone("L_SHIN", JointId.LEFT_KNEE, JointId.LEFT_ANKLE, 90f, 0.20f, 12f)
    val R_SHIN = Bone("R_SHIN", JointId.RIGHT_KNEE, JointId.RIGHT_ANKLE, 90f, 0.20f, 12f)
    val L_FOOT_BONE = Bone("L_FOOT_BONE", JointId.LEFT_ANKLE, JointId.LEFT_FOOT, 0f, 0.06f, 10f)
    val R_FOOT_BONE = Bone("R_FOOT_BONE", JointId.RIGHT_ANKLE, JointId.RIGHT_FOOT, 0f, 0.06f, 10f)

    val ALL_BONES: List<Bone> = listOf(
        SPINE_LOWER, SPINE_UPPER, NECK_SEGMENT, HEAD_SEGMENT,
        L_CLAVICLE, R_CLAVICLE, L_UPPER_ARM, R_UPPER_ARM, L_FOREARM, R_FOREARM,
        L_PELVIC_LINK, R_PELVIC_LINK, L_THIGH, R_THIGH, L_SHIN, R_SHIN, L_FOOT_BONE, R_FOOT_BONE
    )

    fun getBoneToChild(childId: JointId): Bone? = ALL_BONES.find { it.childJoint == childId }
}
