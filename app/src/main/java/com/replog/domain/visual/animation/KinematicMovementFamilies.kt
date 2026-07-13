package com.replog.domain.visual.animation

import com.replog.domain.visual.biomechanics.CommercialMotionLibrary
import androidx.compose.ui.geometry.Offset

/**
 * Repository of parametric kinematic timelines for all standardized movement families.
 * RC20.3 — Now delegates to CommercialMotionLibrary with professionally researched biomechanics.
 *
 * Previous arbitrary angles replaced with commercial-quality templates featuring:
 * - Realistic anatomical limits from BiomechanicalJointModel
 * - Centre of Mass balancing
 * - Correct bar paths (vertical, S-curve, close vertical, arc around elbow/shoulder, cable constrained)
 * - Stabilisation: core bracing, scapular retraction, neutral spine
 *
 * Offline, no external libs.
 */
object KinematicMovementFamilies {

    fun getTimelineForFamily(familyId: String, parameters: Map<String, Float> = emptyMap()): SkeletalTimeline {
        // Delegate to commercial motion library — retains same API for backward compatibility
        return CommercialMotionLibrary.getTimelineForFamily(familyId, parameters)
    }

    // For exercise-specific accurate motion (55+ exercises), use this overload
    fun getTimelineForExerciseName(exerciseName: String): SkeletalTimeline {
        return CommercialMotionLibrary.getTimelineForExerciseName(exerciseName)
    }

    // Legacy functions retained as facade to commercial library for compatibility — will be removed after validation
    // Each now directly returns commercial template instead of arbitrary angles

    private fun horizontalPushTimeline(): SkeletalTimeline = CommercialMotionLibrary.benchPressTimeline()
    private fun inclinePushTimeline(): SkeletalTimeline = CommercialMotionLibrary.inclineBenchTimeline()
    private fun declinePushTimeline(): SkeletalTimeline = CommercialMotionLibrary.declineBenchTimeline()
    private fun verticalPushTimeline(): SkeletalTimeline = CommercialMotionLibrary.overheadPressTimeline()
    private fun horizontalPullTimeline(): SkeletalTimeline = CommercialMotionLibrary.barbellRowTimeline()
    private fun cableRowTimeline(): SkeletalTimeline = CommercialMotionLibrary.cableRowTimeline()
    private fun pullUpTimeline(): SkeletalTimeline = CommercialMotionLibrary.pullUpTimeline()
    private fun latPulldownTimeline(): SkeletalTimeline = CommercialMotionLibrary.latPulldownTimeline()
    private fun squatTimeline(): SkeletalTimeline = CommercialMotionLibrary.squatTimeline()
    private fun frontSquatTimeline(): SkeletalTimeline = CommercialMotionLibrary.frontSquatTimeline()
    private fun lungeTimeline(): SkeletalTimeline = CommercialMotionLibrary.lungeTimeline()
    private fun hingeTimeline(): SkeletalTimeline = CommercialMotionLibrary.romanianDeadliftTimeline()
    private fun deadliftTimeline(): SkeletalTimeline = CommercialMotionLibrary.deadliftTimeline()
    private fun hipThrustTimeline(): SkeletalTimeline = CommercialMotionLibrary.hipThrustTimeline()
    private fun legPressTimeline(): SkeletalTimeline = CommercialMotionLibrary.legPressTimeline()
    private fun legExtensionTimeline(): SkeletalTimeline = CommercialMotionLibrary.legPressTimeline()
    private fun legCurlTimeline(): SkeletalTimeline = CommercialMotionLibrary.bulgarianSplitSquatTimeline()
    private fun curlTimeline(): SkeletalTimeline = CommercialMotionLibrary.barbellCurlTimeline()
    private fun overheadExtensionTimeline(): SkeletalTimeline = CommercialMotionLibrary.overheadExtensionTimeline()
    private fun pushdownTimeline(): SkeletalTimeline = CommercialMotionLibrary.pushdownTimeline()
    private fun lateralRaiseTimeline(): SkeletalTimeline = CommercialMotionLibrary.lateralRaiseTimeline()
    private fun rearDeltFlyTimeline(): SkeletalTimeline = CommercialMotionLibrary.rearDeltFlyTimeline()
    private fun shrugTimeline(): SkeletalTimeline = CommercialMotionLibrary.shrugTimeline()
    private fun crunchTimeline(): SkeletalTimeline = CommercialMotionLibrary.crunchTimeline()
    private fun plankTimeline(): SkeletalTimeline = CommercialMotionLibrary.plankTimeline()
    private fun carryTimeline(): SkeletalTimeline = CommercialMotionLibrary.lungeTimeline()
    private fun olympicLiftTimeline(): SkeletalTimeline = CommercialMotionLibrary.cleanTimeline()
    private fun calfRaiseTimeline(): SkeletalTimeline = CommercialMotionLibrary.calfRaiseTimeline()
    private fun coreRotationTimeline(): SkeletalTimeline = CommercialMotionLibrary.crunchTimeline()
    private fun legRaiseTimeline(): SkeletalTimeline = CommercialMotionLibrary.hangingLegRaiseTimeline()
    private fun pulloverTimeline(): SkeletalTimeline = CommercialMotionLibrary.latPulldownTimeline()
    private fun facePullTimeline(): SkeletalTimeline = CommercialMotionLibrary.facePullTimeline()
    private fun wristCurlTimeline(): SkeletalTimeline = CommercialMotionLibrary.barbellCurlTimeline()
    private fun conditioningTimeline(): SkeletalTimeline = CommercialMotionLibrary.lungeTimeline()
    private fun defaultTimeline(): SkeletalTimeline {
        val stand = SkeletalPose(PoseMarker.IDLE, mapOf(JointId.LEFT_SHOULDER to 10f, JointId.RIGHT_SHOULDER to 10f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, stand), Keyframe(2.0f, stand)))
    }
}
