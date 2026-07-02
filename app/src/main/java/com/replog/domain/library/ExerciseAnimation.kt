package com.replog.domain.library

import com.replog.data.model.Exercise

/**
 * Lightweight, reusable keyframe animation engine (Phase 5). It produces a tiny
 * stick-figure animation for an exercise, generated entirely in code from the
 * movement-pattern family - no videos, GIFs or photos, and no per-exercise stored
 * frames. Each clip is at most 8 keyframes of normalised joint positions plus
 * frame timing, so the in-memory footprint is well under 5 KB per exercise and
 * the engine is reused for every exercise in the library.
 *
 * Coordinates are normalised 0..1 (x right, y down) so the renderer can scale to
 * any canvas. The renderer interpolates linearly between keyframes.
 */

/** A single skeletal pose. All points normalised 0..1. */
data class Pose(
    val head: Point,
    val shoulder: Point,
    val elbow: Point,
    val hand: Point,
    val hip: Point,
    val knee: Point,
    val foot: Point,
    /** Optional implement (bar/dumbbell) centre; null when bodyweight. */
    val implement: Point?
)

data class Point(val x: Float, val y: Float)

/** A full clip: ordered keyframes (<= 8) and the time for one full cycle. */
data class AnimationClip(
    val keyframes: List<Pose>,
    val cycleMillis: Int,
    val mirrored: Boolean = false
)

object ExerciseAnimation {

    /** Build a clip for an exercise from its movement family. Always non-empty. */
    fun clip(ex: Exercise): AnimationClip {
        val hasImplement = ex.equipment.lowercase() !in setOf("bodyweight", "none", "")
        return when (ExercisePattern.of(ex)) {
            ExercisePattern.SQUAT, ExercisePattern.LUNGE, ExercisePattern.KNEE_EXTENSION -> squatClip(hasImplement)
            ExercisePattern.HINGE, ExercisePattern.HIP_EXTENSION, ExercisePattern.HIP_ABDUCTION,
            ExercisePattern.HIP_ADDUCTION, ExercisePattern.KNEE_FLEXION, ExercisePattern.OLYMPIC -> hingeClip(hasImplement)
            ExercisePattern.HORIZONTAL_PRESS, ExercisePattern.INCLINE_PRESS, ExercisePattern.DECLINE_PRESS,
            ExercisePattern.VERTICAL_PRESS, ExercisePattern.CHEST_FLY, ExercisePattern.ELBOW_EXTENSION -> pressClip(hasImplement)
            ExercisePattern.VERTICAL_PULL, ExercisePattern.HORIZONTAL_ROW, ExercisePattern.ELBOW_FLEXION,
            ExercisePattern.FACE_PULL, ExercisePattern.SHRUG, ExercisePattern.WRIST_FLEXION -> pullClip(hasImplement)
            ExercisePattern.LATERAL_RAISE, ExercisePattern.FRONT_RAISE, ExercisePattern.REAR_DELT -> raiseClip(hasImplement)
            ExercisePattern.CALF_RAISE -> calfClip(hasImplement)
            ExercisePattern.CORE_ANTI_EXTENSION, ExercisePattern.CORE_ROTATION, ExercisePattern.CORE_TRUNK_FLEXION,
            ExercisePattern.CORE_HIP_FLEXION -> coreClip()
            ExercisePattern.CORE_CARRY -> carryClip(hasImplement)
            ExercisePattern.CONDITIONING -> conditioningClip()
            ExercisePattern.GENERIC -> pressClip(hasImplement)
        }
    }

    // Helper to build a standing-ish pose quickly.
    private fun pose(
        headY: Float, shoulderY: Float, elbow: Point, hand: Point,
        hipY: Float, knee: Point, foot: Point, implement: Point?
    ) = Pose(
        head = Point(0.5f, headY),
        shoulder = Point(0.5f, shoulderY),
        elbow = elbow,
        hand = hand,
        hip = Point(0.5f, hipY),
        knee = knee,
        foot = foot,
        implement = implement
    )

    private fun squatClip(impl: Boolean): AnimationClip {
        val imp = { y: Float -> if (impl) Point(0.5f, y) else null }
        val stand = pose(0.10f, 0.22f, Point(0.40f, 0.34f), Point(0.40f, 0.46f), 0.50f, Point(0.5f, 0.72f), Point(0.5f, 0.95f), imp(0.20f))
        val bottom = pose(0.20f, 0.32f, Point(0.40f, 0.44f), Point(0.40f, 0.56f), 0.60f, Point(0.62f, 0.66f), Point(0.5f, 0.95f), imp(0.30f))
        return AnimationClip(listOf(stand, bottom, stand), 2400)
    }

    private fun hingeClip(impl: Boolean): AnimationClip {
        val imp = { y: Float -> if (impl) Point(0.5f, y) else null }
        val stand = pose(0.10f, 0.22f, Point(0.46f, 0.34f), Point(0.5f, 0.50f), 0.50f, Point(0.5f, 0.72f), Point(0.5f, 0.95f), imp(0.50f))
        val hinged = Pose(Point(0.66f,0.30f), Point(0.60f,0.36f), Point(0.58f,0.48f), Point(0.55f,0.62f), Point(0.42f,0.52f), Point(0.45f,0.74f), Point(0.5f,0.95f), if (impl) Point(0.55f,0.62f) else null)
        return AnimationClip(listOf(stand, hinged, stand), 2600)
    }

    private fun pressClip(impl: Boolean): AnimationClip {
        val imp = { y: Float -> if (impl) Point(0.5f, y) else null }
        val racked = pose(0.10f, 0.24f, Point(0.36f, 0.30f), Point(0.42f, 0.24f), 0.52f, Point(0.5f, 0.74f), Point(0.5f, 0.95f), imp(0.24f))
        val pressed = pose(0.10f, 0.24f, Point(0.40f, 0.16f), Point(0.5f, 0.08f), 0.52f, Point(0.5f, 0.74f), Point(0.5f, 0.95f), imp(0.08f))
        return AnimationClip(listOf(racked, pressed, racked), 2200)
    }

    private fun pullClip(impl: Boolean): AnimationClip {
        val imp = { y: Float -> if (impl) Point(0.5f, y) else null }
        val stretched = pose(0.10f, 0.24f, Point(0.42f, 0.40f), Point(0.5f, 0.54f), 0.52f, Point(0.5f, 0.74f), Point(0.5f, 0.95f), imp(0.54f))
        val contracted = pose(0.10f, 0.24f, Point(0.34f, 0.30f), Point(0.44f, 0.30f), 0.52f, Point(0.5f, 0.74f), Point(0.5f, 0.95f), imp(0.32f))
        return AnimationClip(listOf(stretched, contracted, stretched), 2200)
    }

    private fun raiseClip(impl: Boolean): AnimationClip {
        val imp = { y: Float -> if (impl) Point(0.34f, y) else null }
        val down = pose(0.10f, 0.24f, Point(0.40f, 0.34f), Point(0.36f, 0.46f), 0.52f, Point(0.5f, 0.74f), Point(0.5f, 0.95f), imp(0.46f))
        val up = pose(0.10f, 0.24f, Point(0.34f, 0.24f), Point(0.22f, 0.24f), 0.52f, Point(0.5f, 0.74f), Point(0.5f, 0.95f), imp(0.24f))
        return AnimationClip(listOf(down, up, down), 2000)
    }

    private fun calfClip(impl: Boolean): AnimationClip {
        val imp = { y: Float -> if (impl) Point(0.5f, y) else null }
        val down = pose(0.10f, 0.22f, Point(0.46f, 0.34f), Point(0.46f, 0.48f), 0.50f, Point(0.5f, 0.72f), Point(0.5f, 0.96f), imp(0.34f))
        val up = pose(0.06f, 0.18f, Point(0.46f, 0.30f), Point(0.46f, 0.44f), 0.46f, Point(0.5f, 0.68f), Point(0.5f, 0.90f), imp(0.30f))
        return AnimationClip(listOf(down, up, down), 1800)
    }

    private fun coreClip(): AnimationClip {
        // Lying trunk flexion / plank style: head and shoulder lift slightly.
        val flat = Pose(Point(0.20f,0.55f), Point(0.30f,0.55f), Point(0.30f,0.62f), Point(0.30f,0.70f), Point(0.55f,0.55f), Point(0.75f,0.62f), Point(0.92f,0.62f), null)
        val crunch = Pose(Point(0.28f,0.46f), Point(0.34f,0.50f), Point(0.34f,0.58f), Point(0.34f,0.66f), Point(0.55f,0.55f), Point(0.72f,0.50f), Point(0.88f,0.52f), null)
        return AnimationClip(listOf(flat, crunch, flat), 2400)
    }

    private fun carryClip(impl: Boolean): AnimationClip {
        val imp = { x: Float -> if (impl) Point(x, 0.52f) else null }
        val left = pose(0.10f, 0.24f, Point(0.40f, 0.40f), Point(0.40f, 0.52f), 0.52f, Point(0.46f, 0.74f), Point(0.42f, 0.95f), imp(0.40f))
        val right = pose(0.10f, 0.24f, Point(0.40f, 0.40f), Point(0.40f, 0.52f), 0.52f, Point(0.54f, 0.74f), Point(0.58f, 0.95f), imp(0.40f))
        return AnimationClip(listOf(left, right, left), 1400)
    }

    private fun conditioningClip(): AnimationClip {
        val a = pose(0.10f, 0.24f, Point(0.36f, 0.30f), Point(0.30f, 0.22f), 0.52f, Point(0.42f, 0.72f), Point(0.38f, 0.92f), null)
        val b = pose(0.10f, 0.24f, Point(0.64f, 0.30f), Point(0.70f, 0.22f), 0.52f, Point(0.58f, 0.72f), Point(0.62f, 0.92f), null)
        return AnimationClip(listOf(a, b, a), 1000)
    }
}
