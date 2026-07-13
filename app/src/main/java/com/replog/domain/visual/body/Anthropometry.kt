package com.replog.domain.visual.body

/**
 * Anthropometric proportions based on ANSUR II 50th percentile male/female averages
 * and NASA-STD-3000 human body measurements.
 *
 * All values normalized relative to total body height (H = 1.0).
 * Used for commercial body rendering with realistic proportions.
 *
 * Reference:
 * - Head height ~13% H
 * - Shoulder breadth ~25% H
 * - Chest depth ~12% H
 * - Upper arm length ~19% H (humerus)
 * - Forearm length ~16% H
 * - Hand length ~11% H
 * - Thigh length ~24.5% H (femur)
 * - Shank length ~24.5% H (tibia)
 * - Foot length ~15% H
 * - Pelvis width ~19% H
 * - Waist width ~15% H
 *
 * Thicknesses are fractions of shoulder breadth or segment length for volume rendering.
 */
object Anthropometry {

    // Vertical proportions normalized to 0..1 standing height
    const val HEAD_HEIGHT = 0.13f
    const val NECK_HEIGHT = 0.05f
    const val TORSO_UPPER_HEIGHT = 0.18f // chest region
    const val TORSO_LOWER_HEIGHT = 0.15f // abdomen
    const val PELVIS_HEIGHT = 0.08f
    const val THIGH_LENGTH = 0.245f
    const val SHANK_LENGTH = 0.245f
    const val FOOT_HEIGHT = 0.04f
    const val FOOT_LENGTH = 0.15f
    const val UPPER_ARM_LENGTH = 0.19f
    const val FOREARM_LENGTH = 0.16f
    const val HAND_LENGTH = 0.11f
    const val CLAVICLE_LENGTH = 0.09f

    // Width proportions
    const val SHOULDER_BREADTH = 0.26f // biacromial
    const val CHEST_BREADTH = 0.20f
    const val WAIST_BREADTH = 0.16f
    const val PELVIS_BREADTH = 0.19f
    const val HIP_BREADTH = 0.19f

    // Thicknesses (depth) for volumetric rendering, relative to segment length
    const val HEAD_RADIUS_FACTOR = 0.065f // half of head height
    const val NECK_RADIUS = 0.035f
    const val CHEST_THICKNESS = 0.11f
    const val ABDOMEN_THICKNESS = 0.09f
    const val UPPER_ARM_THICKNESS_START = 0.045f
    const val UPPER_ARM_THICKNESS_END = 0.038f
    const val FOREARM_THICKNESS_START = 0.038f
    const val FOREARM_THICKNESS_END = 0.030f
    const val HAND_RADIUS = 0.025f
    const val THIGH_THICKNESS_START = 0.065f
    const val THIGH_THICKNESS_END = 0.052f
    const val SHANK_THICKNESS_START = 0.050f
    const val SHANK_THICKNESS_MID = 0.058f // calf bulge
    const val SHANK_THICKNESS_END = 0.035f
    const val FOOT_THICKNESS = 0.030f

    // Taper ratios
    const val ARM_TAPER = 0.84f
    const val LEG_TAPER = 0.80f

    // Validation of proportions: total height check
    fun totalHeight(): Float {
        return HEAD_HEIGHT + NECK_HEIGHT + TORSO_UPPER_HEIGHT + TORSO_LOWER_HEIGHT + PELVIS_HEIGHT + THIGH_LENGTH + SHANK_LENGTH + FOOT_HEIGHT
    }

    // Returns shoulder width relative to canvas reference size
    fun shoulderWidth(referenceSize: Float): Float = referenceSize * SHOULDER_BREADTH

    // Derived thickness functions that scale with reference size (min(width,height))
    fun upperArmThicknessStart(ref: Float): Float = ref * UPPER_ARM_THICKNESS_START
    fun upperArmThicknessEnd(ref: Float): Float = ref * UPPER_ARM_THICKNESS_END
    fun forearmThicknessStart(ref: Float): Float = ref * FOREARM_THICKNESS_START
    fun forearmThicknessEnd(ref: Float): Float = ref * FOREARM_THICKNESS_END
    fun thighThicknessStart(ref: Float): Float = ref * THIGH_THICKNESS_START
    fun thighThicknessEnd(ref: Float): Float = ref * THIGH_THICKNESS_END
    fun shankThicknessStart(ref: Float): Float = ref * SHANK_THICKNESS_START
    fun shankThicknessMid(ref: Float): Float = ref * SHANK_THICKNESS_MID
    fun shankThicknessEnd(ref: Float): Float = ref * SHANK_THICKNESS_END
}
