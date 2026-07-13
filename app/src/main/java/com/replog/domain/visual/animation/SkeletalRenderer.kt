package com.replog.domain.visual.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.replog.domain.visual.spec.EquipmentType

/**
 * Pre-cached draw styles ensuring 0 object allocation inside animation draw loops.
 */
private object SkeletalRenderStyles {
    val boneStrokeCap = StrokeCap.Round
}

/**
 * High-performance 2D kinematic renderer drawing hierarchical bones, joints,
 * and synchronized equipment implements with zero draw loop allocations.
 */
object SkeletalRenderer {

    fun drawSkeleton(
        drawScope: DrawScope,
        skeleton: SolvedSkeleton,
        boneColor: Color,
        jointColor: Color,
        implementColor: Color,
        equipmentType: EquipmentType
    ) {
        with(drawScope) {
            val width = size.width
            val height = size.height

            fun toScreen(pt: Offset) = Offset(width * pt.x, height * pt.y)

            // 1. Draw Bones
            for (bone in BoneCatalog.ALL_BONES) {
                val parentSolved = skeleton.getJoint(bone.parentJoint) ?: continue
                val childSolved = skeleton.getJoint(bone.childJoint) ?: continue
                val startScreen = toScreen(parentSolved.worldPositionOffset)
                val endScreen = toScreen(childSolved.worldPositionOffset)

                drawLine(
                    color = boneColor,
                    start = startScreen,
                    end = endScreen,
                    strokeWidth = bone.renderThickness * (width / 500f),
                    cap = SkeletalRenderStyles.boneStrokeCap
                )
            }

            // 2. Draw Joints
            for (joint in JointId.entries) {
                val solved = skeleton.getJoint(joint) ?: continue
                val screenPos = toScreen(solved.worldPositionOffset)
                val radius = if (joint == JointId.HEAD) width * 0.045f else width * 0.012f
                drawCircle(
                    color = if (joint == JointId.HEAD) boneColor else jointColor,
                    radius = radius,
                    center = screenPos
                )
            }

            // 3. Draw Synchronized Equipment Implement
            if (equipmentType != EquipmentType.BODYWEIGHT) {
                val anchor = EquipmentAnchoring.solveAnchor(skeleton, equipmentType)
                val midScreen = toScreen(anchor.axisMidpoint)
                val barHalfWidth = width * 0.18f

                drawLine(
                    color = implementColor,
                    start = Offset(midScreen.x - barHalfWidth, midScreen.y),
                    end = Offset(midScreen.x + barHalfWidth, midScreen.y),
                    strokeWidth = width * 0.024f,
                    cap = SkeletalRenderStyles.boneStrokeCap
                )
            }
        }
    }
}
