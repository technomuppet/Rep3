package com.replog.domain.library

import com.replog.data.model.Exercise

/**
 * The granular movement pattern of an exercise, parsed from the catalogue's
 * `movementPattern` string (e.g. "Push - Horizontal Press", "Legs - Hip
 * Abduction"). This is the primary key for individual coaching (Sprint 14): it is
 * far more specific than the old 10 coarse families and is combined with
 * equipment to produce exercise-accurate guidance.
 */
enum class ExercisePattern {
    HORIZONTAL_PRESS, INCLINE_PRESS, DECLINE_PRESS, VERTICAL_PRESS, CHEST_FLY,
    ELBOW_EXTENSION,                 // triceps
    VERTICAL_PULL, HORIZONTAL_ROW, ELBOW_FLEXION /*biceps*/, FACE_PULL, REAR_DELT,
    SHRUG, WRIST_FLEXION, OLYMPIC, LATERAL_RAISE, FRONT_RAISE,
    SQUAT, LUNGE, HINGE, HIP_EXTENSION, HIP_ABDUCTION, HIP_ADDUCTION,
    KNEE_EXTENSION, KNEE_FLEXION, CALF_RAISE,
    CORE_ANTI_EXTENSION, CORE_ROTATION, CORE_TRUNK_FLEXION, CORE_HIP_FLEXION, CORE_CARRY,
    CONDITIONING, GENERIC;

    companion object {
        fun of(ex: Exercise): ExercisePattern {
            val p = ex.movementPattern.lowercase()
            val cat = ex.category.lowercase()
            val name = ex.name.lowercase()
            val primary = ex.primaryMuscles.lowercase()
            return when {
                // Front raises share the "Lateral Raise" catalogue pattern but train the
                // FRONT delts and move the weight FORWARD, not out to the sides. Detect
                // them by name or front-delt primary so their coaching is accurate.
                (name.contains("front raise") || (p.contains("lateral raise") && primary.contains("front delt"))) -> FRONT_RAISE
                p.contains("incline press") -> INCLINE_PRESS
                p.contains("decline press") -> DECLINE_PRESS
                p.contains("vertical press") -> VERTICAL_PRESS
                p.contains("horizontal press") -> HORIZONTAL_PRESS
                p.contains("chest fly") -> CHEST_FLY
                p.contains("elbow extension") -> ELBOW_EXTENSION
                p.contains("elbow flexion") -> ELBOW_FLEXION
                p.contains("vertical pull") -> VERTICAL_PULL
                p.contains("horizontal row") -> HORIZONTAL_ROW
                p.contains("face pull") -> FACE_PULL
                p.contains("rear delt") -> REAR_DELT
                p.contains("shrug") -> SHRUG
                p.contains("wrist flexion") -> WRIST_FLEXION
                p.contains("olympic") -> OLYMPIC
                p.contains("lateral raise") -> LATERAL_RAISE
                p.contains("hip abduction") -> HIP_ABDUCTION
                p.contains("hip adduction") -> HIP_ADDUCTION
                p.contains("hip extension") -> HIP_EXTENSION
                p.contains("knee extension") -> KNEE_EXTENSION
                p.contains("knee flexion") -> KNEE_FLEXION
                p.contains("calf") -> CALF_RAISE
                p.contains("squat") -> SQUAT
                p.contains("lunge") -> LUNGE
                p.contains("hinge") -> HINGE
                p.contains("anti-extension") -> CORE_ANTI_EXTENSION
                p.contains("rotation") -> CORE_ROTATION
                p.contains("trunk flexion") -> CORE_TRUNK_FLEXION
                p.contains("hip flexion") -> CORE_HIP_FLEXION
                p.contains("carry") -> CORE_CARRY
                p.contains("conditioning") || cat == "cardio" -> CONDITIONING
                // sensible fallbacks by category when pattern is blank/odd
                cat == "core" -> CORE_TRUNK_FLEXION
                cat == "legs" -> SQUAT
                else -> GENERIC
            }
        }
    }
}

/** A friendly equipment bucket for setup wording. */
enum class EquipmentKind { BARBELL, EZ_BAR, SMITH, DUMBBELL, KETTLEBELL, CABLE, MACHINE, BAND, BODYWEIGHT, PLATE, OTHER;
    companion object {
        fun of(ex: Exercise): EquipmentKind = when (ex.equipment.lowercase()) {
            "barbell" -> BARBELL
            "ez bar" -> EZ_BAR
            "smith machine" -> SMITH
            "dumbbell" -> DUMBBELL
            "kettlebell" -> KETTLEBELL
            "cable" -> CABLE
            "machine" -> MACHINE
            "band" -> BAND
            "bodyweight", "none", "" -> BODYWEIGHT
            "plate" -> PLATE
            else -> OTHER
        }
    }
}
