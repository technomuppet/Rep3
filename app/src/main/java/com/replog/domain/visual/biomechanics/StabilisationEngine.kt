package com.replog.domain.visual.biomechanics

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.animation.JointId
import com.replog.domain.visual.animation.SolvedSkeleton

/**
 * Automatic stabilisation system:
 * - Core bracing (neutral spine)
 * - Scapular retraction (shoulders pinned back for bench)
 * - Shoulder depression (pull-up, deadlift)
 * - Neutral spine (hinge, squat, deadlift)
 * - Hip stability (knee over toe, no valgus)
 * - Foot pressure (mid-foot, tripod)
 * - Balance correction (COM over base)
 *
 * Offline, no external libs.
 */
object StabilisationEngine {

    data class StabilisationCues(
        val coreBraced: Boolean,
        val scapulaRetracted: Boolean,
        val shoulderDepressed: Boolean,
        val neutralSpine: Boolean,
        val hipStable: Boolean,
        val footPressureBalanced: Boolean,
        val balanced: Boolean,
        val messages: List<String>
    )

    /**
     * Evaluates stabilisation for given skeleton and exercise family.
     */
    fun evaluate(
        skeleton: SolvedSkeleton,
        familyId: String
    ): StabilisationCues {
        val messages = mutableListOf<String>()

        // Core bracing: check pelvis and chest alignment — for squat, chest should not excessively flex
        val pelvis = skeleton.getWorldPosition(JointId.PELVIS)
        val chest = skeleton.getWorldPosition(JointId.CHEST)
        val upperChest = skeleton.getWorldPosition(JointId.UPPER_CHEST)

        // Neutral spine: lumbar + thoracic flexion within -10..20 for standing lifts
        val lumbarFlex = chest.y - upperChest.y // simplified: if chest much below upper chest, flexed
        val neutralSpine = kotlin.math.abs(chest.x - pelvis.x) < 0.15f // for standing, chest over pelvis X
        if (!neutralSpine) messages.add("Maintain neutral spine")

        // Scapular retraction: for bench, shoulder width should be slightly less than max (retracted)
        val leftShoulder = skeleton.getWorldPosition(JointId.LEFT_SHOULDER)
        val rightShoulder = skeleton.getWorldPosition(JointId.RIGHT_SHOULDER)
        val shoulderWidth = kotlin.math.hypot((rightShoulder.x - leftShoulder.x).toDouble(), (rightShoulder.y - leftShoulder.y).toDouble()).toFloat()
        val scapulaRetracted = when (familyId) {
            "HORIZONTAL_PUSH", "INCLINE_PUSH", "DECLINE_PUSH" -> shoulderWidth < 0.22f // retracted narrower
            else -> true
        }
        if (!scapulaRetracted) messages.add("Retract scapula")

        // Shoulder depression: for pull-up, shoulders should be depressed not shrugged
        val leftElbow = skeleton.getWorldPosition(JointId.LEFT_ELBOW)
        val shoulderDepressed = when (familyId) {
            "PULL_UP", "LAT_PULLDOWN", "DEADLIFT" -> leftShoulder.y > upperChest.y - 0.05f // shoulder not elevated above upper chest too much
            else -> true
        }
        if (!shoulderDepressed) messages.add("Depress shoulders")

        // Hip stability: knees over toes, no valgus (knee X should be near ankle X)
        val leftKnee = skeleton.getWorldPosition(JointId.LEFT_KNEE)
        val leftAnkle = skeleton.getWorldPosition(JointId.LEFT_ANKLE)
        val rightKnee = skeleton.getWorldPosition(JointId.RIGHT_KNEE)
        val rightAnkle = skeleton.getWorldPosition(JointId.RIGHT_ANKLE)
        val leftKneeAnkleXDiff = kotlin.math.abs(leftKnee.x - leftAnkle.x)
        val rightKneeAnkleXDiff = kotlin.math.abs(rightKnee.x - rightAnkle.x)
        val hipStable = leftKneeAnkleXDiff < 0.08f && rightKneeAnkleXDiff < 0.08f
        if (!hipStable) messages.add("Knees tracking over toes, avoid valgus")

        // Foot pressure: mid-foot balanced
        val comResult = CentreOfMassCalculator.calculate(skeleton)
        val footPressureBalanced = comResult.isBalanced
        if (!footPressureBalanced) messages.add("COM over mid-foot, tripod foot")

        // Core braced: for all, pelvis tilt within realistic -10..10 for standing
        val pelvisTiltOk = kotlin.math.abs(pelvis.y - chest.y) < 0.3f // simplified
        val coreBraced = pelvisTiltOk && neutralSpine
        if (!coreBraced) messages.add("Brace core, neutral pelvis")

        val balanced = neutralSpine && hipStable && footPressureBalanced

        return StabilisationCues(
            coreBraced = coreBraced,
            scapulaRetracted = scapulaRetracted,
            shoulderDepressed = shoulderDepressed,
            neutralSpine = neutralSpine,
            hipStable = hipStable,
            footPressureBalanced = footPressureBalanced,
            balanced = balanced,
            messages = messages
        )
    }

    /**
     * Applies automatic stabilisation corrections to skeleton (small adjustments).
     * Currently returns same skeleton with cues, but could adjust joint rotations slightly.
     */
    fun stabilise(skeleton: SolvedSkeleton, familyId: String): Pair<SolvedSkeleton, StabilisationCues> {
        val cues = evaluate(skeleton, familyId)
        // For now, no auto-correction to avoid instability; in future, could adjust chest flexion slightly
        return Pair(skeleton, cues)
    }

    /**
     * Generates coaching overlay messages based on stabilisation.
     */
    fun generateCoachingOverlay(cues: StabilisationCues): List<String> {
        return cues.messages
    }
}
