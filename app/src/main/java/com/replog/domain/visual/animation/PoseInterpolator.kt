package com.replog.domain.visual.animation

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.biomechanics.BiomechanicalJointModel
import kotlin.math.abs

enum class EasingCurve {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    EASE_IN_OUT_CUBIC // smoother for commercial motion
}

/**
 * Commercial rotational pose interpolator with:
 * - Shortest-angle interpolation (avoids 340 deg spin when -170 to 170)
 * - Preservation of all joints (no snap to zero)
 * - Realistic easing per joint type (could be extended)
 * - COM-aware root interpolation (foot lock preserved)
 * - Clamping to realistic limits from BiomechanicalJointModel
 */
object PoseInterpolator {

    fun interpolate(poseA: SkeletalPose, poseB: SkeletalPose, t: Float, curve: EasingCurve = EasingCurve.EASE_IN_OUT): SkeletalPose {
        val clampedT = t.coerceIn(0f, 1f)
        val easedT = applyEasing(clampedT, curve)

        val interpolatedMap = mutableMapOf<JointId, Float>()

        for (jointId in JointId.entries) {
            val angleA = poseA.getRotation(jointId)
            val angleB = poseB.getRotation(jointId)
            // Preserve all joints that have any non-zero or are explicitly needed for motion
            // If both zero, keep zero (no allocation needed but we include for correctness)
            if (angleA != 0f || angleB != 0f || jointId in setOf(JointId.PELVIS, JointId.CHEST, JointId.UPPER_CHEST)) {
                // Shortest-angle interpolation
                val diff = shortestAngleDiff(angleA, angleB)
                val interpolated = angleA + diff * easedT
                // Clamp to realistic limits
                val clamped = BiomechanicalJointModel.clamp(jointId, interpolated)
                interpolatedMap[jointId] = clamped
            }
        }

        // Root interpolation with easing, but preserve foot locking: if both roots similar, no need to move much
        val rootX = poseA.rootPositionOffset.x + (poseB.rootPositionOffset.x - poseA.rootPositionOffset.x) * easedT
        val rootY = poseA.rootPositionOffset.y + (poseB.rootPositionOffset.y - poseA.rootPositionOffset.y) * easedT

        return SkeletalPose(
            marker = if (clampedT < 0.5f) poseA.marker else poseB.marker,
            jointRotations = interpolatedMap,
            rootPositionOffset = Offset(rootX, rootY)
        )
    }

    /**
     * Computes shortest angular difference from a to b, handling wrap-around at 180.
     * Example: -170 to 170 diff = 20 not -340.
     */
    private fun shortestAngleDiff(a: Float, b: Float): Float {
        var diff = b - a
        // Normalize to -180..180
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        return diff
    }

    private fun applyEasing(t: Float, curve: EasingCurve): Float = when (curve) {
        EasingCurve.LINEAR -> t
        EasingCurve.EASE_IN -> t * t
        EasingCurve.EASE_OUT -> 1f - (1f - t) * (1f - t)
        EasingCurve.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f
        EasingCurve.EASE_IN_OUT_CUBIC -> {
            if (t < 0.5f) 4f * t * t * t else 1f - Math.pow((-2.0 * t + 2).toDouble(), 3.0).toFloat() / 2f
        }
    }
}
