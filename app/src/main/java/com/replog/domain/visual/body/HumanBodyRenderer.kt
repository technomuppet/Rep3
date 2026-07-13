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
 * Commercial human body renderer replacing pipe-like stick figure.
 * RC20.4: Added camera system support for front/rear/left/right side views,
 * automatic best-view selection, and prevention of left/right limb overlap.
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

        val pelvis = screenPos(JointId.PELVIS)
        val chest = screenPos(JointId.CHEST)
        val upperChest = screenPos(JointId.UPPER_CHEST)
        val neck = screenPos(JointId.NECK)
        val head = screenPos(JointId.HEAD)
        val leftShoulder = screenPos(JointId.LEFT_SHOULDER)
        val rightShoulder = screenPos(JointId.RIGHT_SHOULDER)
        val leftElbow = screenPos(JointId.LEFT_ELBOW)
        val rightElbow = screenPos(JointId.RIGHT_ELBOW)
        val leftWrist = screenPos(JointId.LEFT_WRIST)
        val rightWrist = screenPos(JointId.RIGHT_WRIST)
        val leftHip = screenPos(JointId.LEFT_HIP)
        val rightHip = screenPos(JointId.RIGHT_HIP)
        val leftKnee = screenPos(JointId.LEFT_KNEE)
        val rightKnee = screenPos(JointId.RIGHT_KNEE)
        val leftAnkle = screenPos(JointId.LEFT_ANKLE)
        val rightAnkle = screenPos(JointId.RIGHT_ANKLE)
        val leftFoot = screenPos(JointId.LEFT_FOOT)
        val rightFoot = screenPos(JointId.RIGHT_FOOT)

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

        val cullLeft = CameraSystem.shouldCullLeftSide(cameraView)
        val cullRight = CameraSystem.shouldCullRightSide(cameraView)

        // Feet
        val leftFootDir = Offset(leftFoot.x - leftAnkle.x, leftFoot.y - leftAnkle.y)
        val rightFootDir = Offset(rightFoot.x - rightAnkle.x, rightFoot.y - rightAnkle.y)
        val leftFootVec = if (hypot(leftFootDir.x.toDouble(), leftFootDir.y.toDouble()) > 2f) leftFootDir else Offset(20f, 0f)
        val rightFootVec = if (hypot(rightFootDir.x.toDouble(), rightFootDir.y.toDouble()) > 2f) rightFootDir else Offset(20f, 0f)

        if (!cullLeft) drawFoot(leftAnkle, leftFootVec, footLength, footThickness, palette.shoe)
        if (!cullRight) drawFoot(rightAnkle, rightFootVec, footLength, footThickness, palette.shoe)

        // Lower legs
        if (!cullLeft) {
            drawCapsule(leftKnee, leftAnkle, shankStart, shankMid, palette.skin)
            drawCapsule(Offset((leftKnee.x + leftAnkle.x) * 0.5f, (leftKnee.y + leftAnkle.y) * 0.5f), leftAnkle, shankMid, shankEnd, palette.skin)
        }
        if (!cullRight) {
            drawCapsule(rightKnee, rightAnkle, shankStart, shankMid, palette.skin)
            drawCapsule(Offset((rightKnee.x + rightAnkle.x) * 0.5f, (rightKnee.y + rightAnkle.y) * 0.5f), rightAnkle, shankMid, shankEnd, palette.skin)
        }

        // Thighs
        if (!cullLeft) drawCapsule(leftHip, leftKnee, thighStart, thighEnd, palette.shorts)
        if (!cullRight) drawCapsule(rightHip, rightKnee, thighStart, thighEnd, palette.shorts)

        // Pelvis
        val pelvisCenterScreen = Offset((leftHip.x + rightHip.x) * 0.5f, (leftHip.y + rightHip.y) * 0.5f)
        drawPelvis(pelvisCenterScreen, pelvisWidth, referenceSize * Anthropometry.PELVIS_HEIGHT * 0.6f, palette.shorts, palette.outline)

        // Torso
        drawTorso(leftShoulder, rightShoulder, chest, pelvis, shoulderWidthScreen, chestBottomWidth, waistWidth, pelvisWidth * 0.85f, palette.shirt, palette.outline)

        // Upper arms
        if (!cullLeft) drawCapsule(leftShoulder, leftElbow, upperArmStart, upperArmEnd, palette.shirt)
        if (!cullRight) drawCapsule(rightShoulder, rightElbow, upperArmStart, upperArmEnd, palette.shirt)

        // Forearms
        if (!cullLeft) drawCapsule(leftElbow, leftWrist, forearmStart, forearmEnd, palette.skin)
        if (!cullRight) drawCapsule(rightElbow, rightWrist, forearmStart, forearmEnd, palette.skin)

        // Hands
        if (!cullLeft) {
            val leftForearmDir = Offset(leftWrist.x - leftElbow.x, leftWrist.y - leftElbow.y)
            drawHand(leftWrist, leftForearmDir, handRadius, palette.skin)
        }
        if (!cullRight) {
            val rightForearmDir = Offset(rightWrist.x - rightElbow.x, rightWrist.y - rightElbow.y)
            drawHand(rightWrist, rightForearmDir, handRadius, palette.skin)
        }

        // Neck and head always visible
        drawCapsule(neck, upperChest, neckRadius * 1.8f, neckRadius * 2.2f, palette.skin)
        drawHead(head, headRadius, neck, neckRadius, palette.skin, palette.hair)
    }

    fun computeReferenceSize(width: Float, height: Float): Float {
        return kotlin.math.min(width, height) * 0.32f
    }
}
