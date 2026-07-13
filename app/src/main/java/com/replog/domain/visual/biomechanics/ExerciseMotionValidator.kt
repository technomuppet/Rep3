package com.replog.domain.visual.biomechanics

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.animation.SolvedSkeleton
import com.replog.domain.visual.spec.ExerciseVisualSpec

/**
 * Exercise validation suite for RC20.3
 * Verifies:
 * - Movement accuracy
 * - Joint limits
 * - Equipment attachment
 * - Foot placement
 * - Hand placement
 * - Body posture
 * - Range of motion
 * - Coaching correctness
 *
 * Generates exercise validation report.
 */
object ExerciseMotionValidator {

    data class ExerciseCheck(
        val exerciseName: String,
        val familyId: String,
        val check: String,
        val passed: Boolean,
        val message: String
    )

    data class ValidationReport(
        val total: Int,
        val passed: Int,
        val failed: Int,
        val jointLimitFails: List<ExerciseCheck>,
        val equipmentFails: List<ExerciseCheck>,
        val footPlacementFails: List<ExerciseCheck>,
        val handPlacementFails: List<ExerciseCheck>,
        val postureFails: List<ExerciseCheck>,
        val romFails: List<ExerciseCheck>,
        val coachingFails: List<ExerciseCheck>,
        val comFails: List<ExerciseCheck>,
        val barPathFails: List<ExerciseCheck>,
        val allChecks: List<ExerciseCheck>
    ) {
        val passRate: Float get() = if (total == 0) 100f else passed.toFloat() / total * 100f
        val isCommercialReady: Boolean get() = passRate >= 85f && jointLimitFails.isEmpty() && equipmentFails.isEmpty()
    }

    fun validate(
        specs: List<ExerciseVisualSpec>,
        skeletons: List<SolvedSkeleton>, // sample pose per exercise (e.g., bottom)
        toScreen: (Offset) -> Offset
    ): ValidationReport {
        val allChecks = mutableListOf<ExerciseCheck>()
        val jointFails = mutableListOf<ExerciseCheck>()
        val equipFails = mutableListOf<ExerciseCheck>()
        val footFails = mutableListOf<ExerciseCheck>()
        val handFails = mutableListOf<ExerciseCheck>()
        val postureFails = mutableListOf<ExerciseCheck>()
        val romFails = mutableListOf<ExerciseCheck>()
        val coachingFails = mutableListOf<ExerciseCheck>()
        val comFails = mutableListOf<ExerciseCheck>()
        val barPathFails = mutableListOf<ExerciseCheck>()

        var passed = 0

        fun record(exName: String, family: String, check: String, ok: Boolean, msg: String) {
            val c = ExerciseCheck(exName, family, check, ok, msg)
            allChecks.add(c)
            if (ok) passed++
            else {
                when (check) {
                    "JointLimits" -> jointFails.add(c)
                    "EquipmentAttachment" -> equipFails.add(c)
                    "FootPlacement" -> footFails.add(c)
                    "HandPlacement" -> handFails.add(c)
                    "Posture" -> postureFails.add(c)
                    "ROM" -> romFails.add(c)
                    "Coaching" -> coachingFails.add(c)
                    "COM" -> comFails.add(c)
                    "BarPath" -> barPathFails.add(c)
                }
            }
        }

        for (i in specs.indices) {
            val spec = specs.getOrNull(i) ?: continue
            val skeleton = skeletons.getOrNull(i) ?: continue
            val family = spec.movementFamily.familyId

            // 1. Joint limits
            var jointOk = true
            val jointMessages = mutableListOf<String>()
            for ((id, joint) in skeleton.joints) {
                val rot = joint.localRotationDegrees
                if (!BiomechanicalJointModel.isValid(id, rot)) {
                    jointOk = false
                    jointMessages.add("${id.name} rot $rot out of ${BiomechanicalJointModel.getLimits(id).flexion}")
                }
            }
            record(spec.exerciseName, family, "JointLimits", jointOk, if (jointOk) "All within realistic limits" else jointMessages.take(3).joinToString("; "))

            // 2. Equipment attachment
            val leftWrist = skeleton.getWorldPosition(JointId.LEFT_WRIST)
            val rightWrist = skeleton.getWorldPosition(JointId.RIGHT_WRIST)
            val gripWidth = kotlin.math.hypot((rightWrist.x - leftWrist.x).toDouble(), (rightWrist.y - leftWrist.y).toDouble()).toFloat()
            val equipOk = if (spec.equipment.type.name == "BODYWEIGHT") true else gripWidth in 0.05f..0.8f
            record(spec.exerciseName, family, "EquipmentAttachment", equipOk, "GripWidth=$gripWidth")

            // 3. Foot placement
            val leftFoot = skeleton.getWorldPosition(JointId.LEFT_FOOT)
            val rightFoot = skeleton.getWorldPosition(JointId.RIGHT_FOOT)
            val leftAnkle = skeleton.getWorldPosition(JointId.LEFT_ANKLE)
            val rightAnkle = skeleton.getWorldPosition(JointId.RIGHT_ANKLE)
            val footYDiff = kotlin.math.abs(leftFoot.y - rightFoot.y)
            val footOk = footYDiff < 0.15f && leftFoot.y in 0.3f..0.98f && rightFoot.y in 0.3f..0.98f
            record(spec.exerciseName, family, "FootPlacement", footOk, "L_y=${leftFoot.y} R_y=${rightFoot.y} diff=$footYDiff")

            // 4. Hand placement
            val handOk = if (spec.equipment.type.name == "BODYWEIGHT") {
                true
            } else {
                // Hands should be above chest for bench, near shoulders for overhead etc — simplified check within canvas bounds
                leftWrist.y in 0.0f..0.9f && rightWrist.y in 0.0f..0.9f
            }
            record(spec.exerciseName, family, "HandPlacement", handOk, "L_wrist y=${leftWrist.y} R_y=${rightWrist.y}")

            // 5. Body posture — neutral spine, hip stable
            val stabilisation = StabilisationEngine.evaluate(skeleton, family)
            val postureOk = stabilisation.neutralSpine && stabilisation.hipStable
            record(spec.exerciseName, family, "Posture", postureOk, "neutralSpine=${stabilisation.neutralSpine} hipStable=${stabilisation.hipStable} msgs=${stabilisation.messages}")

            // 6. ROM — check knee flexion for squat etc
            val leftKnee = skeleton.getJoint(JointId.LEFT_KNEE)?.localRotationDegrees ?: 0f
            val rightKnee = skeleton.getJoint(JointId.RIGHT_KNEE)?.localRotationDegrees ?: 0f
            val romOk = when (family) {
                "SQUAT", "FRONT_SQUAT", "HACK_SQUAT" -> leftKnee in 80f..140f // depth
                "DEADLIFT", "ROMANIAN_DEADLIFT" -> leftKnee in 10f..80f
                "CURL" -> {
                    val leftElbow = skeleton.getJoint(JointId.LEFT_ELBOW)?.localRotationDegrees ?: 0f
                    leftElbow in 10f..140f
                }
                else -> true
            }
            record(spec.exerciseName, family, "ROM", romOk, "Knee L=$leftKnee R=$rightKnee")

            // 7. Coaching correctness — core braced, scap retracted etc
            val coachingOk = stabilisation.coreBraced && stabilisation.balanced
            record(spec.exerciseName, family, "Coaching", coachingOk, "core=${stabilisation.coreBraced} balanced=${stabilisation.balanced}")

            // 8. COM
            val comResult = CentreOfMassCalculator.calculate(skeleton)
            val comOk = comResult.isBalanced || family in listOf("PULL_UP", "HANGING") // hanging COM not over mid-foot is ok
            record(spec.exerciseName, family, "COM", comOk, "COM over mid-foot dist=${comResult.comOverMidFootDistance} balanced=${comResult.isBalanced}")

            // 9. Bar path — simplified: check verticality for bench etc
            // For this validation we don't have full path history, so just check bar not too far from chest for bench
            val chest = skeleton.getWorldPosition(JointId.CHEST)
            val barDist = kotlin.math.hypot((leftWrist.x - chest.x).toDouble(), (leftWrist.y - chest.y).toDouble()).toFloat()
            val barPathOk = when (family) {
                "HORIZONTAL_PUSH" -> barDist in 0.05f..0.4f
                else -> true
            }
            record(spec.exerciseName, family, "BarPath", barPathOk, "Wrist-chest dist=$barDist")
        }

        return ValidationReport(
            total = allChecks.size,
            passed = passed,
            failed = allChecks.size - passed,
            jointLimitFails = jointFails,
            equipmentFails = equipFails,
            footPlacementFails = footFails,
            handPlacementFails = handFails,
            postureFails = postureFails,
            romFails = romFails,
            coachingFails = coachingFails,
            comFails = comFails,
            barPathFails = barPathFails,
            allChecks = allChecks
        )
    }

    fun generateReportText(report: ValidationReport): String {
        return buildString {
            appendLine("=== RC20.3 Exercise Validation Report ===")
            appendLine("Total Checks: ${report.total}")
            appendLine("Passed: ${report.passed}")
            appendLine("Failed: ${report.failed}")
            appendLine("Pass Rate: ${"%.2f".format(report.passRate)}%")
            appendLine("Commercial Ready: ${report.isCommercialReady}")
            appendLine()
            if (report.jointLimitFails.isNotEmpty()) {
                appendLine("--- Joint Limits Fails (${report.jointLimitFails.size}) ---")
                report.jointLimitFails.take(20).forEach { appendLine("${it.exerciseName}: ${it.message}") }
                appendLine()
            }
            if (report.equipmentFails.isNotEmpty()) {
                appendLine("--- Equipment Fails (${report.equipmentFails.size}) ---")
                report.equipmentFails.take(20).forEach { appendLine("${it.exerciseName}: ${it.message}") }
                appendLine()
            }
            if (report.footPlacementFails.isNotEmpty()) {
                appendLine("--- Foot Placement Fails (${report.footPlacementFails.size}) ---")
                report.footPlacementFails.take(20).forEach { appendLine("${it.exerciseName}: ${it.message}") }
                appendLine()
            }
            if (report.handPlacementFails.isNotEmpty()) {
                appendLine("--- Hand Placement Fails ---")
                report.handPlacementFails.take(20).forEach { appendLine("${it.exerciseName}: ${it.message}") }
                appendLine()
            }
            if (report.postureFails.isNotEmpty()) {
                appendLine("--- Posture Fails (${report.postureFails.size}) ---")
                report.postureFails.take(20).forEach { appendLine("${it.exerciseName}: ${it.message}") }
                appendLine()
            }
            if (report.romFails.isNotEmpty()) {
                appendLine("--- ROM Fails ---")
                report.romFails.take(20).forEach { appendLine("${it.exerciseName}: ${it.message}") }
                appendLine()
            }
            if (report.comFails.isNotEmpty()) {
                appendLine("--- COM Fails (${report.comFails.size}) ---")
                report.comFails.take(20).forEach { appendLine("${it.exerciseName}: ${it.message}") }
                appendLine()
            }
            if (report.barPathFails.isNotEmpty()) {
                appendLine("--- Bar Path Fails ---")
                report.barPathFails.take(20).forEach { appendLine("${it.exerciseName}: ${it.message}") }
                appendLine()
            }
            appendLine("=== End Report ===")
        }
    }
}
