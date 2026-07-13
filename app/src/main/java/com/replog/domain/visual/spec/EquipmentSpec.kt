package com.replog.domain.visual.spec

enum class EquipmentType {
    BODYWEIGHT,
    BARBELL,
    EZ_BAR,
    DUMBBELL,
    KETTLEBELL,
    CABLE,
    MACHINE,
    SMITH_MACHINE,
    BAND,
    PLATE,
    OTHER
}

enum class SupportType {
    STANDING,
    SEATED_FLAT,
    SEATED_INCLINE,
    SEATED_DECLINE,
    CHEST_SUPPORTED,
    PRONE_LYING,
    SUPINE_LYING,
    SIDE_LYING,
    HANGING,
    KNEELING,
    ALL_FOURS,
    NONE
}

/**
 * Immutable specification detailing the equipment setup, support structure,
 * and spatial attachments required for an exercise demonstration.
 */
data class EquipmentSpec(
    val type: EquipmentType = EquipmentType.BODYWEIGHT,
    val supportType: SupportType = SupportType.STANDING,
    val benchAngle: Float = 0.0f,
    val implementType: String = "None",
    val attachments: Map<String, String> = emptyMap()
)
