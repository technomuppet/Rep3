package com.replog.domain.visual.body

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.animation.SolvedSkeleton
import com.replog.domain.visual.body.VolumetricRenderer.drawCapsule
import com.replog.domain.visual.body.VolumetricRenderer.drawFoot
import com.replog.domain.visual.body.VolumetricRenderer.drawHand
import com.replog.domain.visual.body.VolumetricRenderer.drawHead
import com.replog.domain.visual.body.VolumetricRenderer.drawPelvis
import com.replog.domain.visual.body.VolumetricRenderer.drawTorso
import com.replog.domain.visual.camera.CameraSystem
import kotlin.math.hypot

/**
 * Authoritative human body renderer supporting the 15-point (24-joint) body rig.
 * Dynamically resolves both new spine/heel/toe structures and legacy fallbacks.
 */
object HumanBodyRenderer {

    data class BodyPalette(
        val skin: Color,
        val skinShadow: Color,
        val shirt: Color,
        val shorts: Color,
        val shoe: Color,
        val outline: Color,
        val hair: Color = Color(0xFF3A3A3A)
    ) {
        companion object {
            fun fromMaterial(primary: Color, onSurface: Color): BodyPalette {
                return BodyPalette(
                    skin = Color(0xFFD8BFA0),
                    skinShadow = Color(0xFFC4A68A),
                    shirt = primary,
                    shorts = Color(0xFF2E2E3A),
                    shoe = Color(0xFF1A1A1A),
                    outline = onSurface.copy(alpha = 0.35f),
                    hair = Color(0xFF2B2B2B)
                )
            }
        }
    }

    private fun darkenColor(color: Color, factor: Float): Color {
        return Color(
            red = (color.red * factor).coerceIn(0f, 1f),
            green = (color.green * factor).coerceIn(0f, 1f),
            blue = (color.blue * factor).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }

    fun DrawScope.drawHumanBody(
        skeleton: SolvedSkeleton,
        palette: BodyPalette,
        referenceSize: Float,
        toScreen: (Offset) -> Offset,
        cameraView: CameraSystem.CameraView = CameraSystem.CameraView.FRONT
    ) {
        fun screenPos(id: JointId): Offset {
            val world = skeleton.getWorldPosition(id)
            return toScreen(world)
        }

        // Central Spine / Head (Support both 15-point rig and legacy fallback)
        val pelvis = screenPos(JointId.PELVIS)
        val lowerSpine = if (skeleton.getJoint(JointId.LOWER_SPINE) != null) screenPos(JointId.LOWER_SPINE) else screenPos(JointId.CHEST)
        val midSpine = if (skeleton.getJoint(JointId.MID_SPINE) != null) screenPos(JointId.MID_SPINE) else screenPos(JointId.CHEST)
        val upperSpine = if (skeleton.getJoint(JointId.UPPER_SPINE) != null) screenPos(JointId.UPPER_SPINE) else screenPos(JointId.UPPER_CHEST)
        val neck = screenPos(JointId.NECK)
        val head = screenPos(JointId.HEAD)

        // Arms
        val leftShoulder = screenPos(JointId.LEFT_SHOULDER)
        val rightShoulder = screenPos(JointId.RIGHT_SHOULDER)
        val leftElbow = screenPos(JointId.LEFT_ELBOW)
        val rightElbow = screenPos(JointId.RIGHT_ELBOW)
        val leftWrist = screenPos(JointId.LEFT_WRIST)
        val rightWrist = screenPos(JointId.RIGHT_WRIST)
        val leftHand = if (skeleton.getJoint(JointId.LEFT_HAND) != null) screenPos(JointId.LEFT_HAND) else leftWrist
        val rightHand = if (skeleton.getJoint(JointId.RIGHT_HAND) != null) screenPos(JointId.RIGHT_HAND) else rightWrist

        // Legs
        val leftHip = screenPos(JointId.LEFT_HIP)
        val rightHip = screenPos(JointId.RIGHT_HIP)
        val leftKnee = screenPos(JointId.LEFT_KNEE)
        val rightKnee = screenPos(JointId.RIGHT_KNEE)
        val leftAnkle = screenPos(JointId.LEFT_ANKLE)
        val rightAnkle = screenPos(JointId.RIGHT_ANKLE)
        val leftHeel = if (skeleton.getJoint(JointId.LEFT_HEEL) != null) screenPos(JointId.LEFT_HEEL) else screenPos(JointId.LEFT_FOOT)
        val rightHeel = if (skeleton.getJoint(JointId.RIGHT_HEEL) != null) screenPos(JointId.RIGHT_HEEL) else screenPos(JointId.RIGHT_FOOT)
        val leftToe = if (skeleton.getJoint(JointId.LEFT_TOE) != null) screenPos(JointId.LEFT_TOE) else screenPos(JointId.LEFT_FOOT)
        val rightToe = if (skeleton.getJoint(JointId.RIGHT_TOE) != null) screenPos(JointId.RIGHT_TOE) else screenPos(JointId.RIGHT_FOOT)

        val shoulderWidthScreen = hypot((rightShoulder.x - leftShoulder.x).toDouble(), (rightShoulder.y - leftShoulder.y).toDouble()).toFloat()
            .coerceAtLeast(referenceSize * 0.18f)

        val upperArmStart = Anthropometry.upperArmThicknessStart(referenceSize)
        val upperArmEnd = Anthropometry.upperArmThicknessEnd(referenceSize)
        val forearmStart = Anthropometry.forearmThicknessStart(referenceSize)
        val forearmEnd = Anthropometry.forearmThicknessEnd(referenceSize)
        val thighStart = Anthropometry.thighThicknessStart(referenceSize)
        val thighEnd = Anthropometry.thighThicknessEnd(referenceSize)
        val shankStart = Anthropometry.shankThicknessStart(referenceSize)
        val shankMid = Anthropometry.shankThicknessMid(referenceSize)
        val shankEnd = Anthropometry.shankThicknessEnd(referenceSize)
        val handRadius = referenceSize * Anthropometry.HAND_RADIUS
        val footLength = referenceSize * Anthropometry.FOOT_LENGTH * 0.6f
        val footThickness = referenceSize * Anthropometry.FOOT_THICKNESS
        val headRadius = referenceSize * Anthropometry.HEAD_RADIUS_FACTOR
        val neckRadius = referenceSize * Anthropometry.NECK_RADIUS

        val chestBottomWidth = shoulderWidthScreen * 0.74f
        val waistWidth = shoulderWidthScreen * 0.58f
        val pelvisWidth = shoulderWidthScreen * 0.78f

        // 2.5D Depth Sorting: Determine far and near sides based on cameraView
        val farSide: String
        val nearSide: String
        when (cameraView) {
            CameraSystem.CameraView.RIGHT_SIDE -> {
                farSide = "LEFT"
                nearSide = "RIGHT"
            }
            CameraSystem.CameraView.LEFT_SIDE -> {
                farSide = "RIGHT"
                nearSide = "LEFT"
            }
            else -> {
                farSide = ""
                nearSide = ""
            }
        }

        fun DrawScope.drawLeg(side: String, isFar: Boolean) {
            val hip = if (side == "LEFT") leftHip else rightHip
            val knee = if (side == "LEFT") leftKnee else rightKnee
            val ankle = if (side == "LEFT") leftAnkle else rightAnkle
            val heel = if (side == "LEFT") leftHeel else rightHeel
            val toe = if (side == "LEFT") leftToe else rightToe

            val footDir = Offset(toe.x - ankle.x, toe.y - ankle.y)
            val footVec = if (hypot(footDir.x.toDouble(), footDir.y.toDouble()) > 2f) footDir else Offset(20f, 0f)

            val skinCol = if (isFar) palette.skinShadow else palette.skin
            val shortsCol = if (isFar) darkenColor(palette.shorts, 0.75f) else palette.shorts
            val shoeCol = if (isFar) darkenColor(palette.shoe, 0.75f) else palette.shoe

            // Thigh
            drawCapsule(hip, knee, thighStart, thighEnd, shortsCol)
            // Lower Leg (Shank)
            drawCapsule(knee, ankle, shankStart, shankMid, skinCol)
            drawCapsule(Offset((knee.x + ankle.x) * 0.5f, (knee.y + ankle.y) * 0.5f), ankle, shankMid, shankEnd, skinCol)
            // Foot
            drawFoot(ankle, footVec, footLength, footThickness, shoeCol)
        }

        fun DrawScope.drawArm(side: String, isFar: Boolean) {
            val shoulder = if (side == "LEFT") leftShoulder else rightShoulder
            val elbow = if (side == "LEFT") leftElbow else rightElbow
            val wrist = if (side == "LEFT") leftWrist else rightWrist
            val hand = if (side == "LEFT") leftHand else rightHand

            val skinCol = if (isFar) palette.skinShadow else palette.skin
            val shirtCol = if (isFar) darkenColor(palette.shirt, 0.75f) else palette.shirt

            // Upper Arm
            drawCapsule(shoulder, elbow, upperArmStart, upperArmEnd, shirtCol)
            // Forearm
            drawCapsule(elbow, wrist, forearmStart, forearmEnd, skinCol)
            // Hand
            val forearmDir = Offset(hand.x - elbow.x, hand.y - elbow.y)
            drawHand(wrist, forearmDir, handRadius, skinCol)
        }

        if (farSide.isNotEmpty()) {
            // Draw Far-Side Limbs (shadowed background)
            drawLeg(farSide, isFar = true)
            drawArm(farSide, isFar = true)

            // Draw Central Trunk (Pelvis & Torso)
            val pelvisCenterScreen = Offset((leftHip.x + rightHip.x) * 0.5f, (leftHip.y + rightHip.y) * 0.5f)
            drawPelvis(pelvisCenterScreen, pelvisWidth, referenceSize * Anthropometry.PELVIS_HEIGHT * 0.6f, palette.shorts, palette.outline)
            drawTorso(leftShoulder, rightShoulder, midSpine, pelvis, shoulderWidthScreen, chestBottomWidth, waistWidth, pelvisWidth * 0.85f, palette.shirt, palette.outline)

            // Draw Near-Side Limbs (unshadowed foreground)
            drawLeg(nearSide, isFar = false)
            drawArm(nearSide, isFar = false)
        } else {
            // Front/Rear View: Draw symmetrically
            drawLeg("LEFT", isFar = false)
            drawLeg("RIGHT", isFar = false)

            val pelvisCenterScreen = Offset((leftHip.x + rightHip.x) * 0.5f, (leftHip.y + rightHip.y) * 0.5f)
            drawPelvis(pelvisCenterScreen, pelvisWidth, referenceSize * Anthropometry.PELVIS_HEIGHT * 0.6f, palette.shorts, palette.outline)
            drawTorso(leftShoulder, rightShoulder, midSpine, pelvis, shoulderWidthScreen, chestBottomWidth, waistWidth, pelvisWidth * 0.85f, palette.shirt, palette.outline)

            drawArm("LEFT", isFar = false)
            drawArm("RIGHT", isFar = false)
        }

        // Neck and Head are always drawn in front
        drawCapsule(neck, upperSpine, neckRadius * 1.8f, neckRadius * 2.2f, palette.skin)
        drawHead(head, headRadius, neck, neckRadius, palette.skin, palette.hair)
    }

    fun computeReferenceSize(width: Float, height: Float): Float {
        return kotlin.math.min(width, height) * 0.32f
    }
}
