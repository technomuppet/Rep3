package com.replog.domain.visual.attachment

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.animation.SolvedSkeleton
import com.replog.domain.visual.equipment.EquipmentEngine
import com.replog.domain.visual.equipment.ValidationResult
import com.replog.domain.visual.spec.EquipmentSpec
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Equipment attachment system ensuring physical connection.
 * Validates:
 * - Barbell connected to both hands, correct grip width, plates aligned, never floats
 * - Dumbbells one per hand, correct wrist attachment, rotates naturally
 * - Cable fixed pulley, under tension, correct anchor
 * - Machine fixed frame, handles constrained
 * - Bench pelvis/back/head supported, correct angle
 */
object EquipmentAttachmentSolver {

    data class Attachment(
        val leftHand: Offset,
        val rightHand: Offset,
        val midPoint: Offset,
        val gripWidth: Float,
        val equipmentRendererName: String,
        val isValid: Boolean,
        val validation: ValidationResult
    )

    fun solve(
        skeleton: SolvedSkeleton,
        toScreen: (Offset) -> Offset,
        equipmentSpec: EquipmentSpec
    ): Attachment {
        val leftWorld = skeleton.getWorldPosition(JointId.LEFT_WRIST)
        val rightWorld = skeleton.getWorldPosition(JointId.RIGHT_WRIST)
        val leftScreen = toScreen(leftWorld)
        val rightScreen = toScreen(rightWorld)
        val mid = Offset((leftScreen.x + rightScreen.x) * 0.5f, (leftScreen.y + rightScreen.y) * 0.5f)
        val gripWidth = hypot((rightScreen.x - leftScreen.x).toDouble(), (rightScreen.y - leftScreen.y).toDouble()).toFloat()

        val primaryRenderer = EquipmentEngine.resolvePrimary(equipmentSpec)
        val validation = primaryRenderer.validateAttachment(skeleton, toScreen)

        // Additional attachment checks independent of renderer
        val handsAttached = gripWidth in 5f..1000f // hands exist, grip width reasonable on screen
        val feetPlanted = validateFeetPlanted(skeleton, toScreen)
        val notFloating = validation.floating.not()

        val isValid = validation.isAttached && handsAttached && notFloating

        return Attachment(
            leftHand = leftScreen,
            rightHand = rightScreen,
            midPoint = mid,
            gripWidth = gripWidth,
            equipmentRendererName = primaryRenderer::class.simpleName ?: "Unknown",
            isValid = isValid,
            validation = validation
        )
    }

    private fun validateFeetPlanted(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset): Boolean {
        val leftFoot = skeleton.getWorldPosition(JointId.LEFT_FOOT)
        val rightFoot = skeleton.getWorldPosition(JointId.RIGHT_FOOT)
        // World Y should be near floor 0.75-0.98 for standing; for hanging it will be lower but we allow
        // Check both feet Y close to each other (not one floating far above)
        val yDiff = abs(leftFoot.y - rightFoot.y)
        return yDiff < 0.15f
    }

    /**
     * Validates bench contact: pelvis, back, head supported.
     */
    fun validateBenchContact(
        skeleton: SolvedSkeleton,
        equipmentSpec: EquipmentSpec,
        toScreen: (Offset) -> Offset
    ): BenchContactValidation {
        val pelvis = skeleton.getWorldPosition(JointId.PELVIS)
        val chest = skeleton.getWorldPosition(JointId.CHEST)
        val head = skeleton.getWorldPosition(JointId.HEAD)

        val isSupine = equipmentSpec.supportType.name.contains("SUPINE") ||
                equipmentSpec.supportType.name.contains("SEATED") ||
                equipmentSpec.benchAngle != 0f

        if (!isSupine) {
            return BenchContactValidation(true, true, true, true, "Not bench exercise")
        }

        // For bench, check Y alignment: head, chest, pelvis should be roughly colinear and within bench bounds
        val chestPelvisYDiff = abs(chest.y - pelvis.y)
        val headChestYDiff = abs(head.y - chest.y)

        // For supine horizontal after orientation transform, Y should be similar (since rotated)
        // After our orientation engine, upper body rotated -90, so Y diff should be small for supine?
        // We check differently based on orientation: for supine, X diff matters more
        // Simplified: check all three within 0.3 world units in Y for supine after rotation they become X
        // We'll allow both: either Y diff small (standing) or X diff small (supine)
        val pelvisSupported = pelvis.y in 0.3f..0.85f
        val backSupported = chestPelvisYDiff < 0.25f || abs(chest.x - pelvis.x) < 0.25f
        val headSupported = headChestYDiff < 0.25f || abs(head.x - chest.x) < 0.3f

        val overall = pelvisSupported && backSupported && headSupported

        return BenchContactValidation(
            pelvisSupported = pelvisSupported,
            backSupported = backSupported,
            headSupported = headSupported,
            benchAngleCorrect = abs(equipmentSpec.benchAngle) < 45f, // reasonable
            message = "Pelvis=${pelvis.y} chest-pelvis diff y=$chestPelvisYDiff xDiff=${abs(chest.x - pelvis.x)}"
        )
    }

    data class BenchContactValidation(
        val pelvisSupported: Boolean,
        val backSupported: Boolean,
        val headSupported: Boolean,
        val benchAngleCorrect: Boolean,
        val message: String = ""
    ) {
        val isValid: Boolean get() = pelvisSupported && backSupported && headSupported && benchAngleCorrect
    }
}
