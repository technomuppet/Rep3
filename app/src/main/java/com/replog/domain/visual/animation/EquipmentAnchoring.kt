package com.replog.domain.visual.animation

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.spec.EquipmentType

enum class AttachmentPoint {
    LEFT_HAND,
    RIGHT_HAND,
    BOTH_HANDS_MIDPOINT,
    SHOULDERS_BAR,
    HIPS_BELT,
    ANKLES
}

/**
 * Solved equipment attachment anchor synchronized to world-space kinematic coordinates.
 */
data class SolvedEquipmentAnchor(
    val equipmentType: EquipmentType,
    val primaryAnchor: Offset,
    val secondaryAnchor: Offset?,
    val axisMidpoint: Offset,
    val widthSpan: Float
)

/**
 * Computes dynamic equipment anchor coordinates directly from solved skeletal joints,
 * ensuring implements follow hand and limb motion seamlessly without coordinate drift.
 */
object EquipmentAnchoring {

    fun solveAnchor(skeleton: SolvedSkeleton, equipmentType: EquipmentType): SolvedEquipmentAnchor {
        val leftWrist = skeleton.getWorldPosition(JointId.LEFT_WRIST)
        val rightWrist = skeleton.getWorldPosition(JointId.RIGHT_WRIST)
        val midpoint = Offset((leftWrist.x + rightWrist.x) / 2f, (leftWrist.y + rightWrist.y) / 2f)
        val span = kotlin.math.hypot((rightWrist.x - leftWrist.x).toDouble(), (rightWrist.y - leftWrist.y).toDouble()).toFloat()

        return SolvedEquipmentAnchor(
            equipmentType = equipmentType,
            primaryAnchor = leftWrist,
            secondaryAnchor = rightWrist,
            axisMidpoint = midpoint,
            widthSpan = span.coerceAtLeast(0.08f)
        )
    }
}
