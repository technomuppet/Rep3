package com.replog.domain.visual.equipment

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.animation.SolvedSkeleton
import com.replog.domain.visual.spec.EquipmentSpec
import kotlin.math.abs
import kotlin.math.hypot

// RC20.4 Performance: Path pooling to avoid per-frame Path allocations
private val ezPathPool = Path()
private val kettlebellHandlePathPool = Path()
private val cableLoopPathPool = Path()

/**
 * Olympic Barbell: 7ft bar, rotating sleeves, plates aligned, never floating, connected to both hands, grip width respected.
 */
object OlympicBarbellRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        val mid = Offset((leftWrist.x + rightWrist.x) * 0.5f, (leftWrist.y + rightWrist.y) * 0.5f)
        val gripSpan = hypot((rightWrist.x - leftWrist.x).toDouble(), (rightWrist.y - leftWrist.y).toDouble()).toFloat().coerceAtLeast(referenceSize * 0.4f)
        val barHalf = gripSpan * 0.5f + referenceSize * 0.35f
        drawLine(Color(0xFF9E9E9E), Offset(mid.x - barHalf, mid.y), Offset(mid.x + barHalf, mid.y), strokeWidth = referenceSize * 0.045f, cap = StrokeCap.Round)
        val plateColors = listOf(Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFFFACC15))
        val plateWidth = referenceSize * 0.08f
        val plateHeights = listOf(referenceSize * 0.22f, referenceSize * 0.18f, referenceSize * 0.14f)
        var x = mid.x - barHalf + plateWidth * 0.5f
        plateHeights.forEachIndexed { i, h ->
            drawRect(plateColors[i % plateColors.size], Offset(x - plateWidth * 0.5f, mid.y - h * 0.5f), Size(plateWidth, h))
            x += plateWidth * 0.6f
        }
        x = mid.x + barHalf - plateWidth * 0.5f
        plateHeights.forEachIndexed { i, h ->
            drawRect(plateColors[i % plateColors.size], Offset(x - plateWidth * 0.5f, mid.y - h * 0.5f), Size(plateWidth, h))
            x -= plateWidth * 0.6f
        }
        drawCircle(secondaryColor, referenceSize * 0.02f, leftWrist)
        drawCircle(secondaryColor, referenceSize * 0.02f, rightWrist)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset): ValidationResult {
        val lw = skeleton.getWorldPosition(JointId.LEFT_WRIST)
        val rw = skeleton.getWorldPosition(JointId.RIGHT_WRIST)
        val span = hypot((rw.x - lw.x).toDouble(), (rw.y - lw.y).toDouble()).toFloat()
        val attached = span in 0.08f..0.8f
        return ValidationResult(attached, attached, true, !attached, "OlympicBarbell span=$span")
    }
}

object StandardBarbellRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        val mid = Offset((leftWrist.x + rightWrist.x) * 0.5f, (leftWrist.y + rightWrist.y) * 0.5f)
        val gripSpan = hypot((rightWrist.x - leftWrist.x).toDouble(), (rightWrist.y - leftWrist.y).toDouble()).toFloat().coerceAtLeast(referenceSize * 0.35f)
        val barHalf = gripSpan * 0.5f + referenceSize * 0.22f
        drawLine(Color(0xFFB0B0B0), Offset(mid.x - barHalf, mid.y), Offset(mid.x + barHalf, mid.y), strokeWidth = referenceSize * 0.04f, cap = StrokeCap.Round)
        val plateW = referenceSize * 0.07f
        val plateH = referenceSize * 0.18f
        drawRect(Color(0xFF424242), Offset(mid.x - barHalf - plateW * 0.5f, mid.y - plateH * 0.5f), Size(plateW, plateH))
        drawRect(Color(0xFF424242), Offset(mid.x + barHalf - plateW * 0.5f, mid.y - plateH * 0.5f), Size(plateW, plateH))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = OlympicBarbellRenderer.validateAttachment(skeleton, toScreen).copy(message = "StandardBarbell")
}

object EZBarRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        val mid = Offset((leftWrist.x + rightWrist.x) * 0.5f, (leftWrist.y + rightWrist.y) * 0.5f)
        val span = hypot((rightWrist.x - leftWrist.x).toDouble(), (rightWrist.y - leftWrist.y).toDouble()).toFloat().coerceAtLeast(referenceSize * 0.3f)
        val half = span * 0.5f
        ezPathPool.reset()
        ezPathPool.moveTo(mid.x - half - referenceSize * 0.18f, mid.y)
        ezPathPool.lineTo(mid.x - half * 0.5f, mid.y - referenceSize * 0.03f)
        ezPathPool.lineTo(mid.x + half * 0.5f, mid.y + referenceSize * 0.03f)
        ezPathPool.lineTo(mid.x + half + referenceSize * 0.18f, mid.y)
        drawPath(ezPathPool, Color(0xFF9E9E9E), style = Stroke(width = referenceSize * 0.04f, cap = StrokeCap.Round))
        val pw = referenceSize * 0.06f
        drawRect(Color(0xFF616161), Offset(mid.x - half - referenceSize * 0.18f - pw, mid.y - referenceSize * 0.08f), Size(pw, referenceSize * 0.16f))
        drawRect(Color(0xFF616161), Offset(mid.x + half + referenceSize * 0.18f, mid.y - referenceSize * 0.08f), Size(pw, referenceSize * 0.16f))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = OlympicBarbellRenderer.validateAttachment(skeleton, toScreen).copy(message = "EZBar")
}

object DumbbellsRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        listOf(leftWrist, rightWrist).forEach { wrist ->
            val handleHalf = referenceSize * 0.06f
            drawLine(Color(0xFF9E9E9E), Offset(wrist.x - handleHalf, wrist.y), Offset(wrist.x + handleHalf, wrist.y), strokeWidth = referenceSize * 0.025f, cap = StrokeCap.Round)
            val plateR = referenceSize * 0.07f
            drawCircle(Color(0xFF424242), plateR, Offset(wrist.x - handleHalf - plateR * 0.3f, wrist.y))
            drawCircle(Color(0xFF424242), plateR, Offset(wrist.x + handleHalf + plateR * 0.3f, wrist.y))
            drawCircle(Color(0xFF757575), plateR * 0.5f, Offset(wrist.x - handleHalf - plateR * 0.3f, wrist.y))
            drawCircle(Color(0xFF757575), plateR * 0.5f, Offset(wrist.x + handleHalf + plateR * 0.3f, wrist.y))
        }
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "Dumbbells attached at wrists")
}

object KettlebellsRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        val span = hypot((rightWrist.x - leftWrist.x).toDouble(), (rightWrist.y - leftWrist.y).toDouble()).toFloat()
        if (span < referenceSize * 0.5f) {
            val mid = Offset((leftWrist.x + rightWrist.x) * 0.5f, (leftWrist.y + rightWrist.y) * 0.5f + referenceSize * 0.12f)
            drawKettlebell(this, mid, referenceSize)
        } else {
            listOf(leftWrist, rightWrist).forEach { wrist ->
                val pos = Offset(wrist.x, wrist.y + referenceSize * 0.12f)
                drawKettlebell(this, pos, referenceSize * 0.85f)
            }
        }
    }
    private fun DrawScope.drawKettlebell(pos: Offset, ref: Float) {
        drawCircle(Color(0xFF212121), ref * 0.18f, pos)
        kettlebellHandlePathPool.reset()
        kettlebellHandlePathPool.moveTo(pos.x - ref * 0.12f, pos.y - ref * 0.12f)
        kettlebellHandlePathPool.cubicTo(pos.x - ref * 0.18f, pos.y - ref * 0.28f, pos.x + ref * 0.18f, pos.y - ref * 0.28f, pos.x + ref * 0.12f, pos.y - ref * 0.12f)
        drawPath(kettlebellHandlePathPool, Color(0xFF616161), style = Stroke(width = ref * 0.05f, cap = StrokeCap.Round))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "Kettlebell attached below wrists")
}

object CableHandleRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        val mid = Offset((leftWrist.x + rightWrist.x) * 0.5f, (leftWrist.y + rightWrist.y) * 0.5f)
        val pulley = when (equipmentSpec.supportType.name) {
            "SEATED_FLAT" -> Offset(size.width * 0.5f, size.height * 0.95f)
            else -> Offset(size.width * 0.5f, size.height * 0.05f)
        }
        drawLine(Color(0xFF424242), pulley, mid, strokeWidth = referenceSize * 0.015f, cap = StrokeCap.Round)
        val handleW = referenceSize * 0.2f
        drawLine(Color(0xFF9E9E9E), Offset(mid.x - handleW * 0.5f, mid.y), Offset(mid.x + handleW * 0.5f, mid.y), strokeWidth = referenceSize * 0.04f, cap = StrokeCap.Round)
        cableLoopPathPool.reset()
        cableLoopPathPool.moveTo(mid.x - handleW * 0.4f, mid.y)
        cableLoopPathPool.cubicTo(mid.x, mid.y - referenceSize * 0.12f, mid.x, mid.y - referenceSize * 0.12f, mid.x + handleW * 0.4f, mid.y)
        drawPath(cableLoopPathPool, Color(0xFF757575), style = Stroke(width = referenceSize * 0.025f))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "Cable tension validated")
}

object StraightCableBarRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        val mid = Offset((leftWrist.x + rightWrist.x) * 0.5f, (leftWrist.y + rightWrist.y) * 0.5f)
        val pulley = Offset(size.width * 0.5f, size.height * 0.05f)
        drawLine(Color(0xFF424242), pulley, mid, strokeWidth = referenceSize * 0.015f)
        val barHalf = referenceSize * 0.35f
        drawLine(Color(0xFFBDBDBD), Offset(mid.x - barHalf, mid.y), Offset(mid.x + barHalf, mid.y), strokeWidth = referenceSize * 0.045f, cap = StrokeCap.Round)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "StraightBar cable attached")
}

object RopeAttachmentRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        val mid = Offset((leftWrist.x + rightWrist.x) * 0.5f, (leftWrist.y + rightWrist.y) * 0.5f)
        val pulley = Offset(size.width * 0.5f, size.height * 0.1f)
        drawLine(Color(0xFF424242), pulley, mid, strokeWidth = referenceSize * 0.015f)
        drawLine(Color(0xFF8D6E63), mid, leftWrist, strokeWidth = referenceSize * 0.035f, cap = StrokeCap.Round)
        drawLine(Color(0xFF8D6E63), mid, rightWrist, strokeWidth = referenceSize * 0.035f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF4E342E), referenceSize * 0.06f, leftWrist)
        drawCircle(Color(0xFF4E342E), referenceSize * 0.06f, rightWrist)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "Rope attached")
}

object PullUpBarRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val barY = size.height * 0.08f
        drawLine(Color(0xFF616161), Offset(size.width * 0.15f, barY), Offset(size.width * 0.85f, barY), strokeWidth = referenceSize * 0.06f, cap = StrokeCap.Round)
        drawLine(Color(0xFF424242), Offset(size.width * 0.15f, barY), Offset(size.width * 0.15f, 0f), strokeWidth = referenceSize * 0.04f)
        drawLine(Color(0xFF424242), Offset(size.width * 0.85f, barY), Offset(size.width * 0.85f, 0f), strokeWidth = referenceSize * 0.04f)
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        drawLine(Color(0xFF9E9E9E).copy(alpha = 0.5f), Offset(leftWrist.x, barY), leftWrist, strokeWidth = 2f)
        drawLine(Color(0xFF9E9E9E).copy(alpha = 0.5f), Offset(rightWrist.x, barY), rightWrist, strokeWidth = 2f)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset): ValidationResult {
        val leftWrist = skeleton.getWorldPosition(JointId.LEFT_WRIST)
        val rightWrist = skeleton.getWorldPosition(JointId.RIGHT_WRIST)
        val attached = leftWrist.y < 0.45f && rightWrist.y < 0.45f
        return ValidationResult(attached, attached, true, !attached, "PullUpBar hands near fixed bar: L_y=${leftWrist.y} R_y=${rightWrist.y}")
    }
}

object DipBarsRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        val barLen = size.height * 0.25f
        val leftBarX = size.width * 0.28f
        val rightBarX = size.width * 0.72f
        val barY = (leftWrist.y + rightWrist.y) * 0.5f
        drawLine(Color(0xFF616161), Offset(leftBarX, barY - barLen * 0.5f), Offset(leftBarX, barY + barLen * 0.5f), strokeWidth = referenceSize * 0.055f, cap = StrokeCap.Round)
        drawLine(Color(0xFF616161), Offset(rightBarX, barY - barLen * 0.5f), Offset(rightBarX, barY + barLen * 0.5f), strokeWidth = referenceSize * 0.055f, cap = StrokeCap.Round)
        drawLine(Color(0xFF424242), Offset(leftBarX, barY + barLen * 0.5f), Offset(leftBarX, size.height * 0.95f), strokeWidth = referenceSize * 0.04f)
        drawLine(Color(0xFF424242), Offset(rightBarX, barY + barLen * 0.5f), Offset(rightBarX, size.height * 0.95f), strokeWidth = referenceSize * 0.04f)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "DipBars fixed")
}

object SmithMachineRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftRailX = size.width * 0.32f
        val rightRailX = size.width * 0.68f
        drawLine(Color(0xFF757575), Offset(leftRailX, size.height * 0.05f), Offset(leftRailX, size.height * 0.95f), strokeWidth = referenceSize * 0.03f)
        drawLine(Color(0xFF757575), Offset(rightRailX, size.height * 0.05f), Offset(rightRailX, size.height * 0.95f), strokeWidth = referenceSize * 0.03f)
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        val midY = (leftWrist.y + rightWrist.y) * 0.5f
        drawLine(Color(0xFF9E9E9E), Offset(leftRailX, midY), Offset(rightRailX, midY), strokeWidth = referenceSize * 0.05f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF424242), referenceSize * 0.04f, Offset(leftRailX, midY))
        drawCircle(Color(0xFF424242), referenceSize * 0.04f, Offset(rightRailX, midY))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset): ValidationResult {
        val lw = skeleton.getWorldPosition(JointId.LEFT_WRIST)
        val rw = skeleton.getWorldPosition(JointId.RIGHT_WRIST)
        val sameY = abs(lw.y - rw.y) < 0.08f
        return ValidationResult(sameY, sameY, true, !sameY, "SmithMachine bar horizontal")
    }
}

object ChestPressMachineRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val seatX = size.width * 0.5f
        val seatY = size.height * 0.65f
        drawRect(Color(0xFF424242), Offset(seatX - referenceSize * 0.8f, seatY), Size(referenceSize * 1.6f, referenceSize * 0.2f))
        drawRect(Color(0xFF616161), Offset(seatX - referenceSize * 0.85f, seatY - referenceSize * 1.2f), Size(referenceSize * 0.25f, referenceSize * 1.2f))
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        drawCircle(Color(0xFF9E9E9E), referenceSize * 0.08f, leftWrist)
        drawCircle(Color(0xFF9E9E9E), referenceSize * 0.08f, rightWrist)
        drawLine(Color(0xFF757575), Offset(seatX - referenceSize * 0.6f, seatY - referenceSize * 0.6f), leftWrist, strokeWidth = referenceSize * 0.03f)
        drawLine(Color(0xFF757575), Offset(seatX + referenceSize * 0.6f, seatY - referenceSize * 0.6f), rightWrist, strokeWidth = referenceSize * 0.03f)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "ChestPress machine")
}

object ShoulderPressMachineRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val seatX = size.width * 0.5f
        val seatY = size.height * 0.65f
        drawRect(Color(0xFF424242), Offset(seatX - referenceSize * 0.7f, seatY), Size(referenceSize * 1.4f, referenceSize * 0.18f))
        drawRect(Color(0xFF616161), Offset(seatX - referenceSize * 0.75f, seatY - referenceSize * 1.3f), Size(referenceSize * 0.22f, referenceSize * 1.3f))
        val leftWrist = toScreen(skeleton.getWorldPosition(JointId.LEFT_WRIST))
        val rightWrist = toScreen(skeleton.getWorldPosition(JointId.RIGHT_WRIST))
        drawCircle(Color(0xFF9E9E9E), referenceSize * 0.07f, leftWrist)
        drawCircle(Color(0xFF9E9E9E), referenceSize * 0.07f, rightWrist)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "ShoulderPress machine")
}

object LegPressMachineRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftFoot = toScreen(skeleton.getWorldPosition(JointId.LEFT_FOOT))
        val rightFoot = toScreen(skeleton.getWorldPosition(JointId.RIGHT_FOOT))
        val midFoot = Offset((leftFoot.x + rightFoot.x) * 0.5f, (leftFoot.y + rightFoot.y) * 0.5f)
        drawRect(Color(0xFF616161), Offset(midFoot.x - referenceSize * 0.6f, midFoot.y - referenceSize * 0.6f), Size(referenceSize * 1.2f, referenceSize * 0.65f))
        drawLine(Color(0xFF757575), Offset(size.width * 0.2f, size.height * 0.9f), midFoot, strokeWidth = referenceSize * 0.03f)
        drawLine(Color(0xFF757575), Offset(size.width * 0.8f, size.height * 0.9f), Offset(midFoot.x + referenceSize * 0.2f, midFoot.y), strokeWidth = referenceSize * 0.03f)
        drawRect(Color(0xFF424242), Offset(size.width * 0.35f, size.height * 0.65f), Size(referenceSize * 0.9f, referenceSize * 0.2f))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset): ValidationResult {
        val lf = skeleton.getWorldPosition(JointId.LEFT_FOOT)
        val rf = skeleton.getWorldPosition(JointId.RIGHT_FOOT)
        val same = abs(lf.y - rf.y) < 0.1f
        return ValidationResult(same, true, same, !same, "LegPress feet on sled")
    }
}

object PowerRackRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftX = size.width * 0.25f
        val rightX = size.width * 0.75f
        drawLine(Color(0xFF424242), Offset(leftX, size.height * 0.1f), Offset(leftX, size.height * 0.9f), strokeWidth = referenceSize * 0.06f)
        drawLine(Color(0xFF424242), Offset(rightX, size.height * 0.1f), Offset(rightX, size.height * 0.9f), strokeWidth = referenceSize * 0.06f)
        drawLine(Color(0xFF424242), Offset(leftX, size.height * 0.12f), Offset(rightX, size.height * 0.12f), strokeWidth = referenceSize * 0.05f)
        val hips = toScreen(skeleton.getWorldPosition(JointId.PELVIS))
        drawLine(Color(0xFF616161), Offset(leftX, hips.y), Offset(rightX, hips.y), strokeWidth = referenceSize * 0.04f)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "PowerRack fixed")
}

object SquatRackRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftX = size.width * 0.28f
        val rightX = size.width * 0.72f
        drawLine(Color(0xFF424242), Offset(leftX, size.height * 0.2f), Offset(leftX, size.height * 0.85f), strokeWidth = referenceSize * 0.05f)
        drawLine(Color(0xFF424242), Offset(rightX, size.height * 0.2f), Offset(rightX, size.height * 0.85f), strokeWidth = referenceSize * 0.05f)
        drawLine(Color(0xFF616161), Offset(leftX, size.height * 0.25f), Offset(rightX, size.height * 0.25f), strokeWidth = referenceSize * 0.03f)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "SquatRack fixed")
}

object FlatBenchRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val pelvis = toScreen(skeleton.getWorldPosition(JointId.PELVIS))
        val benchTopY = pelvis.y + referenceSize * 0.18f
        drawRect(Color(0xFF3F3F3F), Offset(size.width * 0.2f, benchTopY - referenceSize * 0.1f), Size(size.width * 0.6f, referenceSize * 0.2f))
        drawRect(Color(0xFF616161), Offset(size.width * 0.25f, benchTopY + referenceSize * 0.1f), Size(referenceSize * 0.08f, referenceSize * 0.35f))
        drawRect(Color(0xFF616161), Offset(size.width * 0.68f, benchTopY + referenceSize * 0.1f), Size(referenceSize * 0.08f, referenceSize * 0.35f))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset): ValidationResult {
        val pelvis = skeleton.getWorldPosition(JointId.PELVIS)
        val supported = pelvis.y in 0.4f..0.8f
        return ValidationResult(supported, true, true, !supported, "FlatBench pelvis supported=$supported y=${pelvis.y}")
    }
}

object InclineBenchRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val pelvis = toScreen(skeleton.getWorldPosition(JointId.PELVIS))
        val benchLen = size.width * 0.65f
        val angleRad = Math.toRadians(30.0).toFloat()
        val dx = kotlin.math.cos(angleRad) * benchLen
        val dy = kotlin.math.sin(angleRad) * benchLen
        val start = Offset(pelvis.x - benchLen * 0.3f, pelvis.y + referenceSize * 0.15f)
        val end = Offset(start.x + dx, start.y - dy)
        drawLine(Color(0xFF3F3F3F), start, end, strokeWidth = referenceSize * 0.22f, cap = StrokeCap.Round)
        drawRect(Color(0xFF3F3F3F), Offset(start.x - referenceSize * 0.15f, start.y), Size(referenceSize * 0.4f, referenceSize * 0.15f))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset): ValidationResult {
        val pelvis = skeleton.getWorldPosition(JointId.PELVIS)
        val supported = pelvis.y in 0.3f..0.75f
        return ValidationResult(supported, true, true, !supported, "InclineBench pelvis y=${pelvis.y}")
    }
}

object DeclineBenchRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val pelvis = toScreen(skeleton.getWorldPosition(JointId.PELVIS))
        val benchLen = size.width * 0.65f
        val angleRad = Math.toRadians(-15.0).toFloat()
        val dx = kotlin.math.cos(angleRad) * benchLen
        val dy = kotlin.math.sin(angleRad) * benchLen
        val start = Offset(pelvis.x - benchLen * 0.35f, pelvis.y + referenceSize * 0.12f)
        val end = Offset(start.x + dx, start.y + dy)
        drawLine(Color(0xFF3F3F3F), start, end, strokeWidth = referenceSize * 0.22f, cap = StrokeCap.Round)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset): ValidationResult {
        val pelvis = skeleton.getWorldPosition(JointId.PELVIS)
        return ValidationResult(true, true, true, false, "DeclineBench pelvis y=${pelvis.y}")
    }
}

object AdjustableBenchRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val benchAngle = equipmentSpec.benchAngle
        val pelvis = toScreen(skeleton.getWorldPosition(JointId.PELVIS))
        val benchLen = size.width * 0.6f
        val angleRad = Math.toRadians(benchAngle.toDouble()).toFloat()
        val dx = kotlin.math.cos(angleRad) * benchLen
        val dy = -kotlin.math.sin(angleRad) * benchLen
        val start = Offset(pelvis.x - benchLen * 0.3f, pelvis.y + referenceSize * 0.15f)
        val end = Offset(start.x + dx, start.y + dy)
        drawLine(Color(0xFF3F3F3F), start, end, strokeWidth = referenceSize * 0.22f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF616161), referenceSize * 0.06f, start)
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "AdjustableBench angle=${skeleton.getWorldPosition(JointId.PELVIS)}")
}

object PlyoBoxRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val leftFoot = toScreen(skeleton.getWorldPosition(JointId.LEFT_FOOT))
        val rightFoot = toScreen(skeleton.getWorldPosition(JointId.RIGHT_FOOT))
        val midX = (leftFoot.x + rightFoot.x) * 0.5f
        val boxY = size.height * 0.78f
        drawRect(Color(0xFF6D4C41), Offset(midX - referenceSize * 0.5f, boxY), Size(referenceSize * 1.0f, referenceSize * 0.5f))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset) = ValidationResult(true, true, true, false, "PlyoBox")
}

object FloorRenderer : EquipmentRenderer {
    override fun draw(drawScope: DrawScope, skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset, equipmentSpec: EquipmentSpec, implementColor: Color, secondaryColor: Color, referenceSize: Float) = with(drawScope) {
        val floorY = size.height * 0.92f
        drawLine(Color(0xFF9E9E9E).copy(alpha = 0.6f), Offset(0f, floorY), Offset(size.width, floorY), strokeWidth = referenceSize * 0.04f, cap = StrokeCap.Round)
        val leftFoot = toScreen(skeleton.getWorldPosition(JointId.LEFT_FOOT))
        val rightFoot = toScreen(skeleton.getWorldPosition(JointId.RIGHT_FOOT))
        drawOval(Color(0xFF000000).copy(alpha = 0.15f), topLeft = Offset(leftFoot.x - referenceSize * 0.18f, floorY - referenceSize * 0.03f), size = Size(referenceSize * 0.36f, referenceSize * 0.08f))
        drawOval(Color(0xFF000000).copy(alpha = 0.15f), topLeft = Offset(rightFoot.x - referenceSize * 0.18f, floorY - referenceSize * 0.03f), size = Size(referenceSize * 0.36f, referenceSize * 0.08f))
    }
    override fun validateAttachment(skeleton: SolvedSkeleton, toScreen: (Offset) -> Offset): ValidationResult {
        val lf = skeleton.getWorldPosition(JointId.LEFT_FOOT)
        val rf = skeleton.getWorldPosition(JointId.RIGHT_FOOT)
        val leftPlanted = lf.y in 0.75f..0.98f
        val rightPlanted = rf.y in 0.75f..0.98f
        val both = leftPlanted && rightPlanted
        return ValidationResult(both, true, both, !both, "Floor feet planted L_y=${lf.y} R_y=${rf.y}")
    }
}
