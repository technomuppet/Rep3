package com.replog.domain.visual.animation

enum class PlaybackMode {
    LOOP,
    PING_PONG,
    ONCE
}

data class Keyframe(
    val timeSeconds: Float,
    val pose: SkeletalPose,
    val easingToNext: EasingCurve = EasingCurve.EASE_IN_OUT
)

/**
 * Frame-rate independent kinematic timeline orchestrating playback transitions
 * across ordered rotational keyframes.
 */
class SkeletalTimeline(
    val durationSeconds: Float,
    val keyframes: List<Keyframe>,
    val mode: PlaybackMode = PlaybackMode.PING_PONG
) {
    init {
        require(keyframes.isNotEmpty()) { "SkeletalTimeline must contain at least one keyframe." }
    }

    /**
     * Evaluates the interpolated kinematic pose at the specified elapsed time.
     */
    fun evaluate(elapsedSeconds: Float, playbackSpeed: Float = 1.0f): SkeletalPose {
        if (keyframes.size == 1) return keyframes[0].pose
        if (durationSeconds <= 0f) return keyframes[0].pose

        val effectiveTime = (elapsedSeconds * playbackSpeed).coerceAtLeast(0f)
        val cycleTime = when (mode) {
            PlaybackMode.ONCE -> effectiveTime.coerceAtMost(durationSeconds)
            PlaybackMode.LOOP -> effectiveTime % durationSeconds
            PlaybackMode.PING_PONG -> {
                val cycle = effectiveTime % (durationSeconds * 2f)
                if (cycle <= durationSeconds) cycle else (durationSeconds * 2f) - cycle
            }
        }

        // Find surrounding keyframe segments
        var i = 0
        while (i < keyframes.size - 1 && keyframes[i + 1].timeSeconds <= cycleTime) {
            i++
        }
        if (i >= keyframes.size - 1) return keyframes.last().pose

        val frameA = keyframes[i]
        val frameB = keyframes[i + 1]
        val segmentDuration = frameB.timeSeconds - frameA.timeSeconds
        val localT = if (segmentDuration <= 0f) 1f else ((cycleTime - frameA.timeSeconds) / segmentDuration).coerceIn(0f, 1f)

        return PoseInterpolator.interpolate(frameA.pose, frameB.pose, localT, frameA.easingToNext)
    }
}
