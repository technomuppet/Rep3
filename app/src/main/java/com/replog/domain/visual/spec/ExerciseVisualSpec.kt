package com.replog.domain.visual.spec

enum class BodyOrientation {
    STANDING,
    SUPINE,
    PRONE,
    SEATED,
    SIDE_LYING,
    HANGING,
    INVERTED,
    KNEELING
}

enum class GripType {
    STANDARD_PRONATED,
    SUPINATED,
    NEUTRAL,
    WIDE,
    CLOSE,
    MIXED,
    SNATCH_GRIP,
    NONE
}

enum class StanceType {
    SHOULDER_WIDTH,
    WIDE_SUMO,
    NARROW,
    STAGGERED_SPLIT,
    SINGLE_LEG,
    NONE
}

enum class RangeOfMotion {
    FULL,
    PARTIAL,
    TOP_HALF,
    BOTTOM_HALF,
    ISOMETRIC
}

/**
 * Complete immutable specification representing every visual aspect of an exercise.
 * Consumed by downstream renderers (skeletal animation engine, anatomical diagram,
 * equipment layer) without relying on UI or Canvas code.
 */
data class ExerciseVisualSpec(
    val exerciseId: Int = 0,
    val exerciseName: String = "",
    val movementFamily: MovementFamilySpec = MovementFamilySpec(),
    val equipment: EquipmentSpec = EquipmentSpec(),
    val anatomy: AnatomySpec = AnatomySpec(),
    val bodyOrientation: BodyOrientation = BodyOrientation.STANDING,
    val supportType: SupportType = SupportType.STANDING,
    val gripType: GripType = GripType.STANDARD_PRONATED,
    val stance: StanceType = StanceType.SHOULDER_WIDTH,
    val implementType: String = "None",
    val benchAngle: Float = 0.0f,
    val rangeOfMotion: RangeOfMotion = RangeOfMotion.FULL,
    val primaryMuscles: Set<String> = emptySet(),
    val secondaryMuscles: Set<String> = emptySet(),
    val mirrorable: Boolean = false,
    val playbackSpeed: Float = 1.0f
)
