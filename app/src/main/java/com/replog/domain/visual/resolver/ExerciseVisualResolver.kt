package com.replog.domain.visual.resolver

import com.replog.data.model.Exercise
import com.replog.domain.visual.registry.MovementFamily
import com.replog.domain.visual.registry.MovementFamilyMetadata
import com.replog.domain.visual.spec.AnatomySpec
import com.replog.domain.visual.spec.BodyOrientation
import com.replog.domain.visual.spec.EquipmentSpec
import com.replog.domain.visual.spec.EquipmentType
import com.replog.domain.visual.spec.ExerciseVisualSpec
import com.replog.domain.visual.spec.GripType
import com.replog.domain.visual.spec.MovementFamilySpec
import com.replog.domain.visual.spec.RangeOfMotion
import com.replog.domain.visual.spec.StanceType
import com.replog.domain.visual.spec.SupportType

/**
 * Pure domain translator resolving an Exercise data entity into an immutable
 * ExerciseVisualSpec without relying on UI, Compose, or Canvas code.
 */
object ExerciseVisualResolver {

    fun resolve(exercise: Exercise): ExerciseVisualSpec {
        val name = exercise.name.trim()
        val eqStr = exercise.equipment.trim().lowercase()
        val patStr = exercise.movementPattern.trim().lowercase()
        val catStr = exercise.category.trim().lowercase()
        val nameLower = name.lowercase()

        val equipmentType = resolveEquipmentType(eqStr)
        val familyMeta = resolveMovementFamily(patStr, nameLower, catStr)

        val (benchAngle, supportType) = resolveSupportAndAngle(familyMeta, nameLower, equipmentType)
        val bodyOrientation = resolveBodyOrientation(supportType)
        val gripType = resolveGripType(nameLower)
        val stanceType = resolveStanceType(nameLower)
        val rangeOfMotion = resolveRangeOfMotion(nameLower)
        val anatomySpec = resolveAnatomy(exercise)

        val implementType = if (eqStr.isEmpty()) "Bodyweight" else exercise.equipment
        val parameters = buildParameters(benchAngle, gripType, stanceType)

        return ExerciseVisualSpec(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            movementFamily = MovementFamilySpec(
                familyId = familyMeta.id,
                parameters = parameters
            ),
            equipment = EquipmentSpec(
                type = equipmentType,
                supportType = supportType,
                benchAngle = benchAngle,
                implementType = implementType
            ),
            anatomy = anatomySpec,
            bodyOrientation = bodyOrientation,
            supportType = supportType,
            gripType = gripType,
            stance = stanceType,
            implementType = implementType,
            benchAngle = benchAngle,
            rangeOfMotion = rangeOfMotion,
            primaryMuscles = anatomySpec.primaryMuscles,
            secondaryMuscles = anatomySpec.secondaryMuscles,
            mirrorable = familyMeta.defaultMirrorable,
            playbackSpeed = 1.0f
        )
    }

    private fun resolveEquipmentType(eqStr: String): EquipmentType = when {
        eqStr.contains("barbell") -> EquipmentType.BARBELL
        eqStr.contains("ez bar") -> EquipmentType.EZ_BAR
        eqStr.contains("smith") -> EquipmentType.SMITH_MACHINE
        eqStr.contains("dumbbell") -> EquipmentType.DUMBBELL
        eqStr.contains("kettlebell") -> EquipmentType.KETTLEBELL
        eqStr.contains("cable") -> EquipmentType.CABLE
        eqStr.contains("machine") -> EquipmentType.MACHINE
        eqStr.contains("band") -> EquipmentType.BAND
        eqStr.contains("plate") -> EquipmentType.PLATE
        eqStr in setOf("bodyweight", "none", "") -> EquipmentType.BODYWEIGHT
        else -> EquipmentType.OTHER
    }

    private fun resolveMovementFamily(pat: String, name: String, cat: String): MovementFamilyMetadata {
        // 1. Olympic Lifts
        if (pat.contains("olympic") || name.contains("snatch") || name.contains("clean") || name.contains("jerk")) {
            return MovementFamily.OLYMPIC_LIFT
        }

        // 2. Squat variations
        if (name.contains("hack squat")) return MovementFamily.HACK_SQUAT
        if (name.contains("front squat") || name.contains("goblet squat")) return MovementFamily.FRONT_SQUAT
        if (name.contains("split squat")) return MovementFamily.SPLIT_SQUAT
        if (pat.contains("squat") || name.contains("squat")) return MovementFamily.SQUAT

        // 3. Lunge variations
        if (pat.contains("lunge") || name.contains("lunge")) return MovementFamily.LUNGE

        // 4. Leg Press
        if (name.contains("leg press")) return MovementFamily.LEG_PRESS

        // 5. Hip Thrust / Glute Bridge
        if (name.contains("hip thrust") || name.contains("frog pump") || name.contains("glute bridge")) {
            return MovementFamily.HIP_THRUST
        }

        // 6. Leg Extensions & Curls & Calves
        if (name.contains("leg extension") || name.contains("terminal knee extension")) return MovementFamily.LEG_EXTENSION
        if (name.contains("leg curl")) return MovementFamily.LEG_CURL
        if (pat.contains("calf") || name.contains("calf")) return MovementFamily.CALF_RAISE

        // 7. Hinge variations
        if (name.contains("romanian deadlift") || name.contains("rdl")) return MovementFamily.ROMANIAN_DEADLIFT
        if (name.contains("deadlift") || name.contains("rack pull")) return MovementFamily.DEADLIFT
        if (pat.contains("hinge") || name.contains("good morning") || name.contains("swing") || name.contains("pull through")) {
            return MovementFamily.HIP_HINGE
        }

        // 8. Pressing variations
        if (name.contains("incline machine press") || name.contains("smith machine incline")) return MovementFamily.INCLINE_PUSH
        if (name.contains("decline machine press") || name.contains("smith machine decline")) return MovementFamily.DECLINE_PUSH
        if (name.contains("machine chest press") || name.contains("hammer strength chest press") || name.contains("machine press")) {
            return MovementFamily.MACHINE_PRESS
        }
        if (pat.contains("incline press") || name.contains("incline bench press") || name.contains("incline dumbbell press") || name.contains("incline push")) {
            return MovementFamily.INCLINE_PUSH
        }
        if (pat.contains("decline press") || name.contains("decline bench press") || name.contains("decline dumbbell press") || name.contains("decline push")) {
            return MovementFamily.DECLINE_PUSH
        }
        if (pat.contains("vertical press") || name.contains("overhead press") || name.contains("shoulder press") || name.contains("military press") || name.contains("w press") || name.contains("svend press")) {
            return MovementFamily.VERTICAL_PUSH
        }
        if (pat.contains("horizontal press") || name.contains("bench press") || name.contains("floor press") || name.contains("push up") || name.contains("chest press") || name.contains("squeeze press") || name.contains("dips")) {
            return MovementFamily.HORIZONTAL_PUSH
        }

        // 9. Fly & Crossover
        if (pat.contains("chest fly") || name.contains("pec deck") || name.contains("crossover") || name.contains("fly") || name.contains("around the world")) {
            return MovementFamily.CABLE_FLY
        }

        // 10. Pulling variations
        if (name.contains("pullover")) return MovementFamily.PULLOVER
        if (name.contains("face pull")) return MovementFamily.FACE_PULL
        if (name.contains("lat pulldown") || name.contains("pulldown")) return MovementFamily.LAT_PULLDOWN
        if (name.contains("pull up") || name.contains("chin up")) return MovementFamily.PULL_UP
        if (name.contains("cable row") || name.contains("seated cable row")) return MovementFamily.CABLE_ROW
        if (name.contains("machine row") || name.contains("machine high row") || name.contains("machine low row") || name.contains("iso lateral")) {
            return MovementFamily.MACHINE_PULL
        }
        if (pat.contains("horizontal row") || pat.contains("vertical pull") || name.contains("row")) {
            return MovementFamily.HORIZONTAL_PULL
        }

        // 11. Isolation Upper
        if (name.contains("preacher curl") || name.contains("spider curl")) return MovementFamily.PREACHER_CURL
        if (name.contains("hammer curl")) return MovementFamily.HAMMER_CURL
        if (pat.contains("elbow flexion") || name.contains("curl")) {
            if (name.contains("wrist")) return MovementFamily.WRIST_CURL
            return MovementFamily.CURL
        }
        if (name.contains("pushdown")) return MovementFamily.PUSHDOWN
        if (pat.contains("elbow extension") || name.contains("overhead extension") || name.contains("extension") || name.contains("skull crusher")) {
            return MovementFamily.OVERHEAD_EXTENSION
        }
        if (pat.contains("lateral raise") || name.contains("lateral raise") || name.contains("powell raise") || name.contains("front raise")) {
            return MovementFamily.LATERAL_RAISE
        }
        if (pat.contains("rear delt") || name.contains("rear delt") || name.contains("reverse fly")) {
            return MovementFamily.REAR_DELT_FLY
        }
        if (pat.contains("shrug") || name.contains("shrug")) return MovementFamily.SHRUG

        // 12. Core & Conditioning
        if (name.contains("plank") || name.contains("bear crawl")) return MovementFamily.PLANK
        if (name.contains("crunch") || name.contains("sit up") || name.contains("rollout")) return MovementFamily.CRUNCH
        if (name.contains("leg raise") || name.contains("oblique raise")) return MovementFamily.LEG_RAISE
        if (name.contains("russian twist") || name.contains("pallof") || name.contains("rotation")) return MovementFamily.CORE_ROTATION
        if (pat.contains("carry") || name.contains("carry") || name.contains("walk")) return MovementFamily.CARRY
        if (pat.contains("conditioning") || cat == "cardio" || name.contains("wall ball")) return MovementFamily.CONDITIONING

        // 13. Category Fallback
        return when (cat) {
            "chest" -> MovementFamily.HORIZONTAL_PUSH
            "back" -> MovementFamily.HORIZONTAL_PULL
            "shoulders" -> MovementFamily.VERTICAL_PUSH
            "legs" -> MovementFamily.SQUAT
            "biceps" -> MovementFamily.CURL
            "triceps" -> MovementFamily.PUSHDOWN
            "core" -> MovementFamily.CRUNCH
            "cardio" -> MovementFamily.CONDITIONING
            else -> MovementFamily.GENERIC_UNMAPPED
        }
    }

    private fun resolveSupportAndAngle(
        family: MovementFamilyMetadata,
        name: String,
        eq: EquipmentType
    ): Pair<Float, SupportType> {
        if (family.id == "INCLINE_PUSH" || name.contains("incline")) {
            return 30.0f to SupportType.SEATED_INCLINE
        }
        if (family.id == "DECLINE_PUSH" || name.contains("decline")) {
            return -15.0f to SupportType.SEATED_DECLINE
        }
        if (family.id == "HORIZONTAL_PUSH" || family.id == "CABLE_FLY" || family.id == "PULLOVER") {
            if (name.contains("push up") || name.contains("floor")) {
                return 0.0f to if (name.contains("push up")) SupportType.PRONE_LYING else SupportType.SUPINE_LYING
            }
            if (eq in setOf(EquipmentType.BARBELL, EquipmentType.DUMBBELL, EquipmentType.SMITH_MACHINE)) {
                return 0.0f to SupportType.SUPINE_LYING
            }
        }
        if (family.id in setOf("PULL_UP", "HANGING")) {
            return 0.0f to SupportType.HANGING
        }
        if (family.id in setOf("PLANK", "CRUNCH")) {
            return 0.0f to if (family.id == "PLANK") SupportType.PRONE_LYING else SupportType.SUPINE_LYING
        }
        if (name.contains("chest supported")) {
            return 45.0f to SupportType.CHEST_SUPPORTED
        }
        if (name.contains("seated")) {
            return 85.0f to SupportType.SEATED_FLAT
        }
        if (family.id == "LAT_PULLDOWN" || family.id == "CABLE_ROW" || family.id == "LEG_PRESS" || family.id == "MACHINE_PRESS") {
            return 85.0f to SupportType.SEATED_FLAT
        }
        return 0.0f to SupportType.STANDING
    }

    private fun resolveBodyOrientation(supportType: SupportType): BodyOrientation = when (supportType) {
        SupportType.STANDING -> BodyOrientation.STANDING
        SupportType.SEATED_FLAT, SupportType.SEATED_INCLINE, SupportType.SEATED_DECLINE -> BodyOrientation.SEATED
        SupportType.SUPINE_LYING -> BodyOrientation.SUPINE
        SupportType.PRONE_LYING, SupportType.CHEST_SUPPORTED -> BodyOrientation.PRONE
        SupportType.SIDE_LYING -> BodyOrientation.SIDE_LYING
        SupportType.HANGING -> BodyOrientation.HANGING
        SupportType.KNEELING, SupportType.ALL_FOURS -> BodyOrientation.KNEELING
        SupportType.NONE -> BodyOrientation.STANDING
    }

    private fun resolveGripType(name: String): GripType = when {
        name.contains("wide grip") || name.contains("snatch grip") -> GripType.WIDE
        name.contains("close grip") -> GripType.CLOSE
        name.contains("neutral grip") || name.contains("hammer") -> GripType.NEUTRAL
        name.contains("reverse grip") || name.contains("underhand") || name.contains("chin up") -> GripType.SUPINATED
        else -> GripType.STANDARD_PRONATED
    }

    private fun resolveStanceType(name: String): StanceType = when {
        name.contains("sumo") -> StanceType.WIDE_SUMO
        name.contains("single leg") || name.contains("single arm") -> StanceType.SINGLE_LEG
        name.contains("split") -> StanceType.STAGGERED_SPLIT
        else -> StanceType.SHOULDER_WIDTH
    }

    private fun resolveRangeOfMotion(name: String): RangeOfMotion = when {
        name.contains("partial") || name.contains("pin") || name.contains("rack pull") -> RangeOfMotion.PARTIAL
        name.contains("iso") -> RangeOfMotion.ISOMETRIC
        else -> RangeOfMotion.FULL
    }

    private fun resolveAnatomy(exercise: Exercise): AnatomySpec {
        val primary = parseCsv(exercise.primaryMuscles.ifBlank { exercise.muscles })
        val secondary = parseCsv(exercise.secondaryMuscles) - primary
        return AnatomySpec(primaryMuscles = primary, secondaryMuscles = secondary)
    }

    private fun parseCsv(csv: String): Set<String> =
        csv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    private fun buildParameters(benchAngle: Float, grip: GripType, stance: StanceType): Map<String, Float> {
        val map = mutableMapOf<String, Float>()
        map["benchAngle"] = benchAngle
        map["gripWidthFactor"] = when (grip) {
            GripType.WIDE -> 1.3f
            GripType.CLOSE -> 0.7f
            else -> 1.0f
        }
        map["stanceWidthFactor"] = when (stance) {
            StanceType.WIDE_SUMO -> 1.4f
            StanceType.SINGLE_LEG -> 0.5f
            else -> 1.0f
        }
        return map
    }
}
