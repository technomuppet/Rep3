package com.replog.domain.visual.animation

import androidx.compose.ui.geometry.Offset

enum class EasingCurve {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT
}

/**
 * Rotational pose interpolator evaluating intermediate joint angles and root positions
 * using non-linear easing curves. Guarantees 0 coordinate rubber-banding.
 */
object PoseInterpolator {

    fun interpolate(poseA: SkeletalPose, poseB: SkeletalPose, t: Float, curve: EasingCurve = EasingCurve.EASE_IN_OUT): SkeletalPose {
        val clampedT = t.coerceIn(0f, 1f)
        val easedT = applyEasing(clampedT, curve)

        val allJointIds = JointId.entries
        val interpolatedMap = mutableMapOf<JointId, Float>()

        for (jointId in allJointIds) {
            val angleA = poseA.getRotation(jointId)
            val angleB = poseB.getRotation(jointId)
            if (angleA != 0f || angleB != 0f) {
                interpolatedMap[jointId] = angleA + (angleB - angleA) * easedT
            }
        }

        val rootX = poseA.rootPositionOffset.x + (poseB.rootPositionOffset.x - poseA.rootPositionOffset.x) * easedT
        val rootY = poseA.rootPositionOffset.y + (poseB.rootPositionOffset.y - poseA.rootPositionOffset.y) * easedT

        return SkeletalPose(
            marker = if (clampedT < 0.5f) poseA.marker else poseB.marker,
            jointRotations = interpolatedMap,
            rootPositionOffset = Offset(rootX, rootY)
        )
    }

    private fun applyEasing(t: Float, curve: EasingCurve): Float = when (curve) {
        EasingCurve.LINEAR -> t
        EasingCurve.EASE_IN -> t * t
        EasingCurve.EASE_OUT -> 1f - (1f - t) * (1f - t)
        EasingCurve.EASE_IN_OUT -> if (t < 0.5f) {
            2f * t * t
        } else {
            1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f
        }
    }
}
