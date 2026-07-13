package com.replog.domain.visual.registry

enum class MovementCategory {
    PUSH,
    PULL,
    LEGS,
    HINGE,
    CORE,
    ISOLATION_UPPER,
    ISOLATION_LOWER,
    CONDITIONING,
    FULL_BODY
}

data class MovementFamilyMetadata(
    val id: String,
    val displayName: String,
    val category: MovementCategory,
    val defaultMirrorable: Boolean = true,
    val description: String = ""
)

/**
 * Standardized movement families representing parametric kinematic archetypes.
 * Exercises reference these families instead of authoring individual animations.
 */
object MovementFamily {
    val HORIZONTAL_PUSH = MovementFamilyMetadata(
        id = "HORIZONTAL_PUSH",
        displayName = "Horizontal Push",
        category = MovementCategory.PUSH,
        description = "Pressing implement perpendicularly away from chest."
    )
    val INCLINE_PUSH = MovementFamilyMetadata(
        id = "INCLINE_PUSH",
        displayName = "Incline Push",
        category = MovementCategory.PUSH,
        description = "Pressing implement at an upward incline from upper chest."
    )
    val DECLINE_PUSH = MovementFamilyMetadata(
        id = "DECLINE_PUSH",
        displayName = "Decline Push",
        category = MovementCategory.PUSH,
        description = "Pressing implement at a downward angle from lower chest."
    )
    val VERTICAL_PUSH = MovementFamilyMetadata(
        id = "VERTICAL_PUSH",
        displayName = "Vertical Push",
        category = MovementCategory.PUSH,
        description = "Pressing implement overhead above shoulders."
    )
    val HORIZONTAL_PULL = MovementFamilyMetadata(
        id = "HORIZONTAL_PULL",
        displayName = "Horizontal Pull",
        category = MovementCategory.PULL,
        description = "Rowing implement horizontally toward torso."
    )
    val CABLE_ROW = MovementFamilyMetadata(
        id = "CABLE_ROW",
        displayName = "Cable Row",
        category = MovementCategory.PULL,
        description = "Seated horizontal pull against constant cable resistance."
    )
    val PULL_UP = MovementFamilyMetadata(
        id = "PULL_UP",
        displayName = "Pull Up",
        category = MovementCategory.PULL,
        description = "Vertical pull lifting torso upward toward overhead bar."
    )
    val LAT_PULLDOWN = MovementFamilyMetadata(
        id = "LAT_PULLDOWN",
        displayName = "Lat Pulldown",
        category = MovementCategory.PULL,
        description = "Seated vertical pull pulling overhead bar down to chest."
    )
    val DEADLIFT = MovementFamilyMetadata(
        id = "DEADLIFT",
        displayName = "Deadlift",
        category = MovementCategory.HINGE,
        description = "Lifting implement from floor via coordinated hip hinge and knee extension."
    )
    val ROMANIAN_DEADLIFT = MovementFamilyMetadata(
        id = "ROMANIAN_DEADLIFT",
        displayName = "Romanian Deadlift",
        category = MovementCategory.HINGE,
        description = "Top-down hip hinge with minimal knee bend stretching posterior chain."
    )
    val HIP_HINGE = MovementFamilyMetadata(
        id = "HIP_HINGE",
        displayName = "Hip Hinge",
        category = MovementCategory.HINGE,
        description = "Pure flexion and extension of the hip joint while maintaining neutral spine."
    )
    val SQUAT = MovementFamilyMetadata(
        id = "SQUAT",
        displayName = "Squat",
        category = MovementCategory.LEGS,
        description = "Simultaneous knee and hip flexion lowering center of gravity."
    )
    val FRONT_SQUAT = MovementFamilyMetadata(
        id = "FRONT_SQUAT",
        displayName = "Front Squat",
        category = MovementCategory.LEGS,
        description = "Anteriorly loaded squat demanding upright torso posture."
    )
    val HACK_SQUAT = MovementFamilyMetadata(
        id = "HACK_SQUAT",
        displayName = "Hack Squat",
        category = MovementCategory.LEGS,
        description = "Guided or behind-the-body squat focusing on quad extension."
    )
    val SPLIT_SQUAT = MovementFamilyMetadata(
        id = "SPLIT_SQUAT",
        displayName = "Split Squat",
        category = MovementCategory.LEGS,
        description = "Staggered stationary leg flexion focusing on front leg."
    )
    val LUNGE = MovementFamilyMetadata(
        id = "LUNGE",
        displayName = "Lunge",
        category = MovementCategory.LEGS,
        description = "Dynamic stepping leg flexion and recovery."
    )
    val HIP_THRUST = MovementFamilyMetadata(
        id = "HIP_THRUST",
        displayName = "Hip Thrust",
        category = MovementCategory.HINGE,
        description = "Supine or shoulder-supported hip extension against gravity."
    )
    val LEG_PRESS = MovementFamilyMetadata(
        id = "LEG_PRESS",
        displayName = "Leg Press",
        category = MovementCategory.LEGS,
        description = "Seated sled push driving weight away via leg extension."
    )
    val CURL = MovementFamilyMetadata(
        id = "CURL",
        displayName = "Curl",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Isolated elbow flexion bringing implement toward shoulders."
    )
    val HAMMER_CURL = MovementFamilyMetadata(
        id = "HAMMER_CURL",
        displayName = "Hammer Curl",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Neutral-grip elbow flexion targeting brachialis and brachioradialis."
    )
    val PREACHER_CURL = MovementFamilyMetadata(
        id = "PREACHER_CURL",
        displayName = "Preacher Curl",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Arm-supported elbow flexion eliminating shoulder movement."
    )
    val OVERHEAD_EXTENSION = MovementFamilyMetadata(
        id = "OVERHEAD_EXTENSION",
        displayName = "Overhead Extension",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Elbow extension performed with arms raised overhead."
    )
    val PUSHDOWN = MovementFamilyMetadata(
        id = "PUSHDOWN",
        displayName = "Pushdown",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Downward elbow extension against cable resistance."
    )
    val LATERAL_RAISE = MovementFamilyMetadata(
        id = "LATERAL_RAISE",
        displayName = "Lateral Raise",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Shoulder abduction raising arms outward to sides."
    )
    val REAR_DELT_FLY = MovementFamilyMetadata(
        id = "REAR_DELT_FLY",
        displayName = "Rear Delt Fly",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Horizontal shoulder abduction targeting posterior deltoids."
    )
    val SHRUG = MovementFamilyMetadata(
        id = "SHRUG",
        displayName = "Shrug",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Scapular elevation lifting shoulders toward ears."
    )
    val CRUNCH = MovementFamilyMetadata(
        id = "CRUNCH",
        displayName = "Crunch",
        category = MovementCategory.CORE,
        description = "Spinal flexion bringing ribs toward pelvis."
    )
    val CARRY = MovementFamilyMetadata(
        id = "CARRY",
        displayName = "Carry",
        category = MovementCategory.CORE,
        description = "Loaded locomotive carry challenging core stability and posture."
    )
    val PLANK = MovementFamilyMetadata(
        id = "PLANK",
        displayName = "Plank",
        category = MovementCategory.CORE,
        description = "Isometric anti-extension core hold."
    )
    val MACHINE_PRESS = MovementFamilyMetadata(
        id = "MACHINE_PRESS",
        displayName = "Machine Press",
        category = MovementCategory.PUSH,
        description = "Guided horizontal or vertical pressing on fixed machine frame."
    )
    val MACHINE_PULL = MovementFamilyMetadata(
        id = "MACHINE_PULL",
        displayName = "Machine Pull",
        category = MovementCategory.PULL,
        description = "Guided rowing or pulldown movement on fixed machine frame."
    )
    val CABLE_FLY = MovementFamilyMetadata(
        id = "CABLE_FLY",
        displayName = "Cable Fly",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Horizontal shoulder adduction using dual cable tension."
    )
    val OLYMPIC_LIFT = MovementFamilyMetadata(
        id = "OLYMPIC_LIFT",
        displayName = "Olympic Lift",
        category = MovementCategory.FULL_BODY,
        description = "Explosive multi-joint triple extension and catch."
    )
    val CALF_RAISE = MovementFamilyMetadata(
        id = "CALF_RAISE",
        displayName = "Calf Raise",
        category = MovementCategory.ISOLATION_LOWER,
        description = "Ankle plantarflexion against resistance."
    )
    val LEG_EXTENSION = MovementFamilyMetadata(
        id = "LEG_EXTENSION",
        displayName = "Leg Extension",
        category = MovementCategory.ISOLATION_LOWER,
        description = "Isolated knee extension against shin pad resistance."
    )
    val LEG_CURL = MovementFamilyMetadata(
        id = "LEG_CURL",
        displayName = "Leg Curl",
        category = MovementCategory.ISOLATION_LOWER,
        description = "Isolated knee flexion curling heels toward glutes."
    )
    val CORE_ROTATION = MovementFamilyMetadata(
        id = "CORE_ROTATION",
        displayName = "Core Rotation",
        category = MovementCategory.CORE,
        description = "Rotational trunk movement or anti-rotation isometric hold."
    )
    val LEG_RAISE = MovementFamilyMetadata(
        id = "LEG_RAISE",
        displayName = "Leg Raise",
        category = MovementCategory.CORE,
        description = "Hip and lower trunk flexion raising legs or knees toward torso."
    )
    val PULLOVER = MovementFamilyMetadata(
        id = "PULLOVER",
        displayName = "Pullover",
        category = MovementCategory.PULL,
        description = "Shoulder extension moving implement in an arc over the head."
    )
    val FACE_PULL = MovementFamilyMetadata(
        id = "FACE_PULL",
        displayName = "Face Pull",
        category = MovementCategory.PULL,
        description = "High horizontal pull with external shoulder rotation toward upper face."
    )
    val WRIST_CURL = MovementFamilyMetadata(
        id = "WRIST_CURL",
        displayName = "Wrist Curl",
        category = MovementCategory.ISOLATION_UPPER,
        description = "Isolated wrist flexion or extension targeting forearms."
    )
    val CONDITIONING = MovementFamilyMetadata(
        id = "CONDITIONING",
        displayName = "Conditioning",
        category = MovementCategory.CONDITIONING,
        description = "Cardiovascular or metabolic locomotive conditioning work."
    )
    val GENERIC_UNMAPPED = MovementFamilyMetadata(
        id = "GENERIC_UNMAPPED",
        displayName = "Generic Unmapped",
        category = MovementCategory.FULL_BODY,
        description = "Fallback generic movement family for unmapped exercise patterns."
    )

    val ALL: List<MovementFamilyMetadata> = listOf(
        HORIZONTAL_PUSH, INCLINE_PUSH, DECLINE_PUSH, VERTICAL_PUSH,
        HORIZONTAL_PULL, CABLE_ROW, PULL_UP, LAT_PULLDOWN,
        DEADLIFT, ROMANIAN_DEADLIFT, HIP_HINGE,
        SQUAT, FRONT_SQUAT, HACK_SQUAT, SPLIT_SQUAT, LUNGE, HIP_THRUST, LEG_PRESS,
        CURL, HAMMER_CURL, PREACHER_CURL, OVERHEAD_EXTENSION, PUSHDOWN,
        LATERAL_RAISE, REAR_DELT_FLY, SHRUG,
        CRUNCH, CARRY, PLANK,
        MACHINE_PRESS, MACHINE_PULL, CABLE_FLY, OLYMPIC_LIFT,
        CALF_RAISE, LEG_EXTENSION, LEG_CURL, CORE_ROTATION, LEG_RAISE,
        PULLOVER, FACE_PULL, WRIST_CURL, CONDITIONING, GENERIC_UNMAPPED
    )
}
