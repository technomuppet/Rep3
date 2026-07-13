package com.replog.domain.visual.body

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sqrt

/**
 * High-performance volumetric capsule drawer with path pooling for zero allocations.
 * RC20.4: Removed per-frame Path creation, pooled reusable Paths, zero avoidable allocations.
 */
object VolumetricRenderer {

    // Path pooling for torso and pelvis to avoid per-frame allocation (RC20.4 optimization)
    private val chestPath = Path()
    private val abdomenPath = Path()
    private val pelvisPath = Path()

    fun DrawScope.drawCapsule(
        start: Offset,
        end: Offset,
        thicknessStart: Float,
        thicknessEnd: Float,
        color: Color
    ) {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = sqrt(dx * dx + dy * dy)
        if (length < 0.5f) {
            drawCircle(color = color, radius = thicknessStart * 0.5f, center = start)
            return
        }
        val avgThickness = (thicknessStart + thicknessEnd) * 0.5f
        drawLine(color = color, start = start, end = end, strokeWidth = avgThickness, cap = StrokeCap.Round)
        drawCircle(color = color, radius = thicknessStart * 0.5f, center = start)
        drawCircle(color = color, radius = thicknessEnd * 0.5f, center = end)
    }

    fun DrawScope.drawLimbWithBulge(
        start: Offset,
        end: Offset,
        thicknessStart: Float,
        thicknessMid: Float,
        thicknessEnd: Float,
        color: Color
    ) {
        val mid = Offset((start.x + end.x) * 0.5f, (start.y + end.y) * 0.5f)
        drawCapsule(start, mid, thicknessStart, thicknessMid, color)
        drawCapsule(mid, end, thicknessMid, thicknessEnd, color)
    }

    fun DrawScope.drawHead(
        center: Offset,
        radius: Float,
        neckCenter: Offset,
        neckRadius: Float,
        skinColor: Color,
        hairColor: Color = Color(0xFF2D2D2D)
    ) {
        drawCircle(color = skinColor, radius = radius, center = center)
        val hairRadius = radius * 0.55f
        val hairCenter = Offset(center.x, center.y - radius * 0.35f)
        drawCircle(color = hairColor, radius = hairRadius, center = hairCenter)
        drawCapsule(neckCenter, center, neckRadius * 2f, radius * 0.9f, skinColor)
    }

    /**
     * Draws torso as volumetric shape with taper — now using pooled Paths to avoid per-frame allocation (RC20.4)
     */
    fun DrawScope.drawTorso(
        leftShoulder: Offset,
        rightShoulder: Offset,
        chestCenter: Offset,
        pelvisCenter: Offset,
        shoulderWidth: Float,
        chestBottomWidth: Float,
        waistWidth: Float,
        pelvisWidth: Float,
        torsoColor: Color,
        outlineColor: Color
    ) {
        val leftChestBottom = Offset(chestCenter.x - chestBottomWidth * 0.5f, chestCenter.y + 10f)
        val rightChestBottom = Offset(chestCenter.x + chestBottomWidth * 0.5f, chestCenter.y + 10f)

        // Reuse pooled chestPath to avoid allocation
        chestPath.reset()
        chestPath.moveTo(leftShoulder.x, leftShoulder.y)
        chestPath.lineTo(rightShoulder.x, rightShoulder.y)
        chestPath.lineTo(rightChestBottom.x, rightChestBottom.y)
        chestPath.lineTo(leftChestBottom.x, leftChestBottom.y)
        chestPath.close()
        drawPath(path = chestPath, color = torsoColor)
        drawPath(path = chestPath, color = outlineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

        val leftWaist = Offset(chestCenter.x - waistWidth * 0.5f, (chestCenter.y + pelvisCenter.y) * 0.5f)
        val rightWaist = Offset(chestCenter.x + waistWidth * 0.5f, (chestCenter.y + pelvisCenter.y) * 0.5f)
        val leftPelvisTop = Offset(pelvisCenter.x - pelvisWidth * 0.5f, pelvisCenter.y - 5f)
        val rightPelvisTop = Offset(pelvisCenter.x + pelvisWidth * 0.5f, pelvisCenter.y - 5f)

        abdomenPath.reset()
        abdomenPath.moveTo(leftChestBottom.x, leftChestBottom.y)
        abdomenPath.lineTo(rightChestBottom.x, rightChestBottom.y)
        abdomenPath.lineTo(rightWaist.x, rightWaist.y)
        abdomenPath.lineTo(rightPelvisTop.x, rightPelvisTop.y)
        abdomenPath.lineTo(leftPelvisTop.x, leftPelvisTop.y)
        abdomenPath.lineTo(leftWaist.x, leftWaist.y)
        abdomenPath.close()

        val abdomenColor = Color(
            red = (torsoColor.red * 0.92f).coerceIn(0f, 1f),
            green = (torsoColor.green * 0.92f).coerceIn(0f, 1f),
            blue = (torsoColor.blue * 0.92f).coerceIn(0f, 1f),
            alpha = torsoColor.alpha
        )
        drawPath(path = abdomenPath, color = abdomenColor)
        drawPath(path = abdomenPath, color = outlineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
    }

    fun DrawScope.drawPelvis(
        center: Offset,
        width: Float,
        height: Float,
        color: Color,
        outlineColor: Color
    ) {
        val left = center.x - width * 0.5f
        val right = center.x + width * 0.5f
        val top = center.y - height * 0.5f
        val bottom = center.y + height * 0.5f

        pelvisPath.reset()
        pelvisPath.moveTo(left, top)
        pelvisPath.lineTo(right, top)
        pelvisPath.cubicTo(right + 8f, top + height * 0.3f, right + 8f, top + height * 0.7f, right, bottom)
        pelvisPath.lineTo(left, bottom)
        pelvisPath.cubicTo(left - 8f, top + height * 0.7f, left - 8f, top + height * 0.3f, left, top)
        pelvisPath.close()
        drawPath(pelvisPath, color)
        drawPath(pelvisPath, outlineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
    }

    fun DrawScope.drawHand(
        wrist: Offset,
        handDirection: Offset,
        radius: Float,
        skinColor: Color
    ) {
        val len = sqrt(handDirection.x * handDirection.x + handDirection.y * handDirection.y)
        val ndx = if (len > 0.001f) handDirection.x / len else 0f
        val ndy = if (len > 0.001f) handDirection.y / len else 1f
        val handCenter = Offset(wrist.x + ndx * radius * 0.8f, wrist.y + ndy * radius * 0.8f)
        drawCircle(color = skinColor, radius = radius, center = handCenter)
        drawCircle(color = skinColor.copy(alpha = 0.9f), radius = radius * 0.6f, center = handCenter)
    }

    fun DrawScope.drawFoot(
        ankle: Offset,
        toeDirection: Offset,
        length: Float,
        thickness: Float,
        shoeColor: Color
    ) {
        val len = sqrt(toeDirection.x * toeDirection.x + toeDirection.y * toeDirection.y)
        val ndx = if (len > 0.001f) toeDirection.x / len else 1f
        val ndy = if (len > 0.001f) toeDirection.y / len else 0f
        val toe = Offset(ankle.x + ndx * length * 0.9f, ankle.y + ndy * thickness * 0.1f)
        drawCapsule(ankle, toe, thickness, thickness * 0.7f, shoeColor)
        drawLine(
            color = shoeColor.copy(alpha = 0.8f),
            start = Offset(ankle.x, ankle.y + thickness * 0.3f),
            end = Offset(toe.x, toe.y + thickness * 0.3f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}
