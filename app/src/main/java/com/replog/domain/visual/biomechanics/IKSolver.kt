package com.replog.domain.visual.biomechanics

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/**
 * Hybrid FK/IK solver using FABRIK (Forward And Backward Reaching Inverse Kinematics)
 * for 2-bone and 3-bone chains.
 *
 * Features:
 * - Foot locking: feet remain planted on ground
 * - Hand targets: hands follow equipment
 * - Equipment targets: barbell, dumbbell, cable pulley
 * - Efficient, minimal allocations, cached calculations
 *
 * No external libraries, offline, Compose Canvas only.
 */
object IKSolver {

    /**
     * FABRIK solver for arbitrary chain length (2-3 bones typical for limbs).
     * Returns solved joint positions.
     *
     * @param jointPositions list of joint world positions from root to end (e.g., hip->knee->ankle->foot)
     * @param boneLengths lengths between joints (size = joints.size-1)
     * @param target desired end effector position
     * @param tolerance for convergence
     * @param maxIterations
     */
    fun solveFABRIK(
        jointPositions: List<Offset>,
        boneLengths: List<Float>,
        target: Offset,
        tolerance: Float = 0.001f,
        maxIterations: Int = 10
    ): List<Offset> {
        if (jointPositions.size != boneLengths.size + 1) return jointPositions
        if (jointPositions.isEmpty()) return jointPositions

        val totalLength = boneLengths.sum()
        val root = jointPositions.first()
        val distToTarget = hypot((target.x - root.x).toDouble(), (target.y - root.y).toDouble()).toFloat()

        // If target unreachable, stretch towards target
        if (distToTarget > totalLength) {
            val result = mutableListOf<Offset>()
            result.add(root)
            var current = root
            for (i in boneLengths.indices) {
                val direction = Offset(target.x - current.x, target.y - current.y)
                val dirLen = hypot(direction.x.toDouble(), direction.y.toDouble()).toFloat()
                if (dirLen < 0.0001f) {
                    result.add(current)
                    continue
                }
                val normalized = Offset(direction.x / dirLen, direction.y / dirLen)
                val next = Offset(
                    current.x + normalized.x * boneLengths[i],
                    current.y + normalized.y * boneLengths[i]
                )
                result.add(next)
                current = next
            }
            return result
        }

        var positions = jointPositions.toMutableList()
        var bPositions = positions.toMutableList()

        var diff = hypot((bPositions.last().x - target.x).toDouble(), (bPositions.last().y - target.y).toDouble()).toFloat()
        var iter = 0

        while (diff > tolerance && iter < maxIterations) {
            // Backward: set end to target
            bPositions[bPositions.lastIndex] = target
            for (i in bPositions.size - 2 downTo 0) {
                val current = bPositions[i]
                val next = bPositions[i + 1]
                val dir = Offset(next.x - current.x, next.y - current.y)
                val len = hypot(dir.x.toDouble(), dir.y.toDouble()).toFloat()
                if (len < 0.0001f) continue
                val normalized = Offset(dir.x / len, dir.y / len)
                // Move current towards next preserving bone length
                val corrected = Offset(
                    next.x - normalized.x * boneLengths[i],
                    next.y - normalized.y * boneLengths[i]
                )
                bPositions[i] = corrected
            }

            // Forward: set root to original
            bPositions[0] = root
            for (i in 0 until bPositions.size - 1) {
                val current = bPositions[i]
                val next = bPositions[i + 1]
                val dir = Offset(next.x - current.x, next.y - current.y)
                val len = hypot(dir.x.toDouble(), dir.y.toDouble()).toFloat()
                if (len < 0.0001f) continue
                val normalized = Offset(dir.x / len, dir.y / len)
                val corrected = Offset(
                    current.x + normalized.x * boneLengths[i],
                    current.y + normalized.y * boneLengths[i]
                )
                bPositions[i + 1] = corrected
            }

            diff = hypot((bPositions.last().x - target.x).toDouble(), (bPositions.last().y - target.y).toDouble()).toFloat()
            iter++
        }

        return bPositions
    }

    /**
     * 2-bone IK for arm: shoulder->elbow->wrist solving to hand target (equipment).
     * Returns elbow position that satisfies shoulder->elbow length and elbow->wrist length.
     */
    fun solveTwoBoneArm(
        shoulder: Offset,
        elbow: Offset,
        wrist: Offset,
        handTarget: Offset,
        upperArmLen: Float,
        forearmLen: Float
    ): Pair<Offset, Offset> {
        val chain = listOf(shoulder, elbow, wrist)
        val lengths = listOf(upperArmLen, forearmLen)
        val solved = solveFABRIK(chain, lengths, handTarget)
        return Pair(solved[1], solved[2]) // elbow, wrist
    }

    /**
     * 3-bone leg chain: hip->knee->ankle->foot with foot locking.
     * Foot target is ground planted position (fixed).
     * Returns knee, ankle, foot positions.
     */
    fun solveLegWithFootLock(
        hip: Offset,
        knee: Offset,
        ankle: Offset,
        foot: Offset,
        footTarget: Offset,
        thighLen: Float,
        shankLen: Float,
        footLen: Float,
        isLeft: Boolean = false
    ): Triple<Offset, Offset, Offset> {
        val footHeight = com.replog.domain.visual.body.Anthropometry.FOOT_HEIGHT
        // Correct flat ankle: raise ankle target by footHeight above floor
        val ankleTargetX = if (isLeft) footTarget.x + footLen * 0.5f else footTarget.x - footLen * 0.5f
        val ankleTarget = Offset(ankleTargetX, footTarget.y - footHeight)

        val chain = listOf(hip, knee, ankle)
        val lengths = listOf(thighLen, shankLen)
        val solved = solveFABRIK(chain, lengths, ankleTarget)

        val solvedAnkle = solved[2]
        // Compute exact horizontal offset dx to preserve footLen invariance while foot remains flat on the floor
        val dy = footHeight
        val dx = kotlin.math.sqrt((footLen * footLen - dy * dy).coerceAtLeast(0f))
        val solvedFoot = Offset(solvedAnkle.x + (if (isLeft) -dx else dx), footTarget.y)

        return Triple(solved[1], solvedAnkle, solvedFoot)
    }

    /**
     * Calculates bone length from two offsets (world normalized).
     */
    fun boneLength(a: Offset, b: Offset): Float {
        return hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
    }
}
