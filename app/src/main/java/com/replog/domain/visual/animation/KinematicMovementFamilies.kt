package com.replog.domain.visual.animation

import androidx.compose.ui.geometry.Offset

/**
 * Repository of parametric kinematic timelines for all standardized movement families.
 * Exercises reference these timelines rather than owning unique animation files.
 */
object KinematicMovementFamilies {

    fun getTimelineForFamily(familyId: String, parameters: Map<String, Float> = emptyMap()): SkeletalTimeline {
        return when (familyId) {
            "HORIZONTAL_PUSH" -> horizontalPushTimeline()
            "INCLINE_PUSH" -> inclinePushTimeline()
            "DECLINE_PUSH" -> declinePushTimeline()
            "VERTICAL_PUSH", "OVERHEAD_PRESS" -> verticalPushTimeline()
            "HORIZONTAL_PULL", "CHEST_SUPPORTED_ROW" -> horizontalPullTimeline()
            "CABLE_ROW" -> cableRowTimeline()
            "PULL_UP" -> pullUpTimeline()
            "LAT_PULLDOWN", "PULLDOWN" -> latPulldownTimeline()
            "DEADLIFT" -> deadliftTimeline()
            "ROMANIAN_DEADLIFT", "HIP_HINGE" -> hingeTimeline()
            "SQUAT" -> squatTimeline()
            "FRONT_SQUAT" -> frontSquatTimeline()
            "SPLIT_SQUAT", "LUNGE" -> lungeTimeline()
            "HACK_SQUAT" -> squatTimeline()
            "HIP_THRUST" -> hipThrustTimeline()
            "LEG_PRESS" -> legPressTimeline()
            "LEG_EXTENSION" -> legExtensionTimeline()
            "LEG_CURL" -> legCurlTimeline()
            "CURL", "HAMMER_CURL", "PREACHER_CURL" -> curlTimeline()
            "OVERHEAD_EXTENSION" -> overheadExtensionTimeline()
            "PUSHDOWN", "TRICEPS_PUSHDOWN" -> pushdownTimeline()
            "LATERAL_RAISE" -> lateralRaiseTimeline()
            "REAR_DELT_FLY" -> rearDeltFlyTimeline()
            "SHRUG" -> shrugTimeline()
            "CRUNCH" -> crunchTimeline()
            "PLANK" -> plankTimeline()
            "CARRY" -> carryTimeline()
            "OLYMPIC_LIFT" -> olympicLiftTimeline()
            "CALF_RAISE" -> calfRaiseTimeline()
            "CORE_ROTATION" -> coreRotationTimeline()
            "LEG_RAISE" -> legRaiseTimeline()
            "PULLOVER" -> pulloverTimeline()
            "FACE_PULL" -> facePullTimeline()
            "WRIST_CURL" -> wristCurlTimeline()
            "CONDITIONING" -> conditioningTimeline()
            else -> defaultTimeline()
        }
    }

    private fun horizontalPushTimeline(): SkeletalTimeline {
        val start = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -80f, JointId.RIGHT_ELBOW to 15f))
        val bottom = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.LEFT_SHOULDER to -40f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to -40f, JointId.RIGHT_ELBOW to 110f))
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, start), Keyframe(1.2f, bottom), Keyframe(2.4f, start)))
    }

    private fun inclinePushTimeline(): SkeletalTimeline {
        val start = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_SHOULDER to -110f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -110f, JointId.RIGHT_ELBOW to 15f))
        val bottom = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.LEFT_SHOULDER to -60f, JointId.LEFT_ELBOW to 115f, JointId.RIGHT_SHOULDER to -60f, JointId.RIGHT_ELBOW to 115f))
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, start), Keyframe(1.2f, bottom), Keyframe(2.4f, start)))
    }

    private fun declinePushTimeline(): SkeletalTimeline {
        val start = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_SHOULDER to -60f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -60f, JointId.RIGHT_ELBOW to 15f))
        val bottom = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.LEFT_SHOULDER to -25f, JointId.LEFT_ELBOW to 105f, JointId.RIGHT_SHOULDER to -25f, JointId.RIGHT_ELBOW to 105f))
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, start), Keyframe(1.2f, bottom), Keyframe(2.4f, start)))
    }

    private fun verticalPushTimeline(): SkeletalTimeline {
        val bottom = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_SHOULDER to -110f, JointId.LEFT_ELBOW to 130f, JointId.RIGHT_SHOULDER to -110f, JointId.RIGHT_ELBOW to 130f))
        val top = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_SHOULDER to -170f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -170f, JointId.RIGHT_ELBOW to 10f))
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, bottom), Keyframe(1.1f, top), Keyframe(2.2f, bottom)))
    }

    private fun horizontalPullTimeline(): SkeletalTimeline {
        val stretch = SkeletalPose(PoseMarker.STRETCH, mapOf(JointId.PELVIS to 35f, JointId.LEFT_SHOULDER to -45f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -45f, JointId.RIGHT_ELBOW to 10f))
        val contract = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.PELVIS to 35f, JointId.LEFT_SHOULDER to 35f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to 35f, JointId.RIGHT_ELBOW to 110f))
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, stretch), Keyframe(1.1f, contract), Keyframe(2.2f, stretch)))
    }

    private fun cableRowTimeline(): SkeletalTimeline {
        val stretch = SkeletalPose(PoseMarker.STRETCH, mapOf(JointId.LEFT_SHOULDER to -75f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -75f, JointId.RIGHT_ELBOW to 10f))
        val contract = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.LEFT_SHOULDER to 15f, JointId.LEFT_ELBOW to 115f, JointId.RIGHT_SHOULDER to 15f, JointId.RIGHT_ELBOW to 115f))
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, stretch), Keyframe(1.1f, contract), Keyframe(2.2f, stretch)))
    }

    private fun pullUpTimeline(): SkeletalTimeline {
        val hang = SkeletalPose(PoseMarker.STRETCH, mapOf(JointId.LEFT_SHOULDER to -175f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -175f, JointId.RIGHT_ELBOW to 10f), Offset(0.5f, 0.65f))
        val top = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_SHOULDER to -65f, JointId.LEFT_ELBOW to 135f, JointId.RIGHT_SHOULDER to -65f, JointId.RIGHT_ELBOW to 135f), Offset(0.5f, 0.40f))
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, hang), Keyframe(1.2f, top), Keyframe(2.4f, hang)))
    }

    private fun latPulldownTimeline(): SkeletalTimeline {
        val stretch = SkeletalPose(PoseMarker.STRETCH, mapOf(JointId.LEFT_SHOULDER to -165f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -165f, JointId.RIGHT_ELBOW to 15f))
        val contract = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.LEFT_SHOULDER to -60f, JointId.LEFT_ELBOW to 130f, JointId.RIGHT_SHOULDER to -60f, JointId.RIGHT_ELBOW to 130f))
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, stretch), Keyframe(1.1f, contract), Keyframe(2.2f, stretch)))
    }

    private fun squatTimeline(): SkeletalTimeline {
        val stand = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.LEFT_ANKLE to 0f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 5f, JointId.RIGHT_ANKLE to 0f), Offset(0.5f, 0.42f))
        val bottom = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.LEFT_HIP to -110f, JointId.LEFT_KNEE to 125f, JointId.LEFT_ANKLE to -25f, JointId.RIGHT_HIP to -110f, JointId.RIGHT_KNEE to 125f, JointId.RIGHT_ANKLE to -25f), Offset(0.5f, 0.58f))
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, stand), Keyframe(1.3f, bottom), Keyframe(2.6f, stand)))
    }

    private fun frontSquatTimeline(): SkeletalTimeline {
        val stand = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 140f, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_SHOULDER to -80f, JointId.RIGHT_ELBOW to 140f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 5f), Offset(0.5f, 0.42f))
        val bottom = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 140f, JointId.LEFT_HIP to -115f, JointId.LEFT_KNEE to 135f, JointId.RIGHT_SHOULDER to -80f, JointId.RIGHT_ELBOW to 140f, JointId.RIGHT_HIP to -115f, JointId.RIGHT_KNEE to 135f), Offset(0.5f, 0.58f))
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, stand), Keyframe(1.3f, bottom), Keyframe(2.6f, stand)))
    }

    private fun lungeTimeline(): SkeletalTimeline {
        val stand = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 5f), Offset(0.5f, 0.42f))
        val bottom = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.LEFT_HIP to -85f, JointId.LEFT_KNEE to 90f, JointId.RIGHT_HIP to 25f, JointId.RIGHT_KNEE to 90f), Offset(0.5f, 0.54f))
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, stand), Keyframe(1.2f, bottom), Keyframe(2.4f, stand)))
    }

    private fun hingeTimeline(): SkeletalTimeline {
        val stand = SkeletalPose(PoseMarker.START, mapOf(JointId.PELVIS to 0f, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 15f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 15f))
        val hinged = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.PELVIS to 75f, JointId.LEFT_HIP to -75f, JointId.LEFT_KNEE to 25f, JointId.RIGHT_HIP to -75f, JointId.RIGHT_KNEE to 25f))
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, stand), Keyframe(1.3f, hinged), Keyframe(2.6f, stand)))
    }

    private fun deadliftTimeline(): SkeletalTimeline {
        val stand = SkeletalPose(PoseMarker.TOP, mapOf(JointId.PELVIS to 0f, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 5f), Offset(0.5f, 0.42f))
        val floor = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.PELVIS to 50f, JointId.LEFT_HIP to -95f, JointId.LEFT_KNEE to 70f, JointId.RIGHT_HIP to -95f, JointId.RIGHT_KNEE to 70f), Offset(0.5f, 0.54f))
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, stand), Keyframe(1.3f, floor), Keyframe(2.6f, stand)))
    }

    private fun hipThrustTimeline(): SkeletalTimeline {
        val bottom = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.PELVIS to 45f, JointId.LEFT_HIP to -85f, JointId.LEFT_KNEE to 90f, JointId.RIGHT_HIP to -85f, JointId.RIGHT_KNEE to 90f), Offset(0.5f, 0.55f))
        val top = SkeletalPose(PoseMarker.TOP, mapOf(JointId.PELVIS to 0f, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 90f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 90f), Offset(0.5f, 0.48f))
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, bottom), Keyframe(1.1f, top), Keyframe(2.2f, bottom)))
    }

    private fun legPressTimeline(): SkeletalTimeline {
        val extend = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_HIP to -80f, JointId.LEFT_KNEE to 10f, JointId.RIGHT_HIP to -80f, JointId.RIGHT_KNEE to 10f))
        val flex = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.LEFT_HIP to -125f, JointId.LEFT_KNEE to 115f, JointId.RIGHT_HIP to -125f, JointId.RIGHT_KNEE to 115f))
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, extend), Keyframe(1.2f, flex), Keyframe(2.4f, extend)))
    }

    private fun legExtensionTimeline(): SkeletalTimeline {
        val down = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_KNEE to 90f, JointId.RIGHT_KNEE to 90f))
        val up = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_KNEE to 5f, JointId.RIGHT_KNEE to 5f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, down), Keyframe(1.0f, up), Keyframe(2.0f, down)))
    }

    private fun legCurlTimeline(): SkeletalTimeline {
        val extend = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_KNEE to 10f, JointId.RIGHT_KNEE to 10f))
        val curl = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.LEFT_KNEE to 120f, JointId.RIGHT_KNEE to 120f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, extend), Keyframe(1.0f, curl), Keyframe(2.0f, extend)))
    }

    private fun curlTimeline(): SkeletalTimeline {
        val down = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_ELBOW to 10f, JointId.RIGHT_ELBOW to 10f))
        val up = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.LEFT_ELBOW to 135f, JointId.RIGHT_ELBOW to 135f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, down), Keyframe(1.0f, up), Keyframe(2.0f, down)))
    }

    private fun overheadExtensionTimeline(): SkeletalTimeline {
        val stretch = SkeletalPose(PoseMarker.STRETCH, mapOf(JointId.LEFT_SHOULDER to -165f, JointId.LEFT_ELBOW to 125f, JointId.RIGHT_SHOULDER to -165f, JointId.RIGHT_ELBOW to 125f))
        val extend = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_SHOULDER to -165f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -165f, JointId.RIGHT_ELBOW to 15f))
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, stretch), Keyframe(1.1f, extend), Keyframe(2.2f, stretch)))
    }

    private fun pushdownTimeline(): SkeletalTimeline {
        val up = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_SHOULDER to 10f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to 10f, JointId.RIGHT_ELBOW to 110f))
        val down = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.LEFT_SHOULDER to 10f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to 10f, JointId.RIGHT_ELBOW to 10f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, up), Keyframe(1.0f, down), Keyframe(2.0f, up)))
    }

    private fun lateralRaiseTimeline(): SkeletalTimeline {
        val down = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_SHOULDER to 5f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to 5f, JointId.RIGHT_ELBOW to 15f))
        val up = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_SHOULDER to -85f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -85f, JointId.RIGHT_ELBOW to 15f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, down), Keyframe(1.0f, up), Keyframe(2.0f, down)))
    }

    private fun rearDeltFlyTimeline(): SkeletalTimeline {
        val start = SkeletalPose(PoseMarker.START, mapOf(JointId.PELVIS to 65f, JointId.LEFT_SHOULDER to -20f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -20f, JointId.RIGHT_ELBOW to 15f))
        val fly = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.PELVIS to 65f, JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -80f, JointId.RIGHT_ELBOW to 15f))
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, start), Keyframe(1.1f, fly), Keyframe(2.2f, start)))
    }

    private fun shrugTimeline(): SkeletalTimeline {
        val rest = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_SHOULDER to 5f, JointId.RIGHT_SHOULDER to 5f))
        val shrug = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_SHOULDER to -25f, JointId.RIGHT_SHOULDER to -25f))
        return SkeletalTimeline(1.6f, listOf(Keyframe(0f, rest), Keyframe(0.8f, shrug), Keyframe(1.6f, rest)))
    }

    private fun crunchTimeline(): SkeletalTimeline {
        val flat = SkeletalPose(PoseMarker.START, mapOf(JointId.CHEST to 0f, JointId.UPPER_CHEST to 0f, JointId.NECK to 0f))
        val crunch = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.CHEST to -25f, JointId.UPPER_CHEST to -20f, JointId.NECK to -20f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, flat), Keyframe(1.0f, crunch), Keyframe(2.0f, flat)))
    }

    private fun plankTimeline(): SkeletalTimeline {
        val hold = SkeletalPose(PoseMarker.IDLE, mapOf(JointId.LEFT_SHOULDER to -90f, JointId.LEFT_ELBOW to 90f, JointId.RIGHT_SHOULDER to -90f, JointId.RIGHT_ELBOW to 90f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, hold), Keyframe(2.0f, hold)))
    }

    private fun carryTimeline(): SkeletalTimeline {
        val stepL = SkeletalPose(PoseMarker.MID, mapOf(JointId.LEFT_HIP to -20f, JointId.LEFT_KNEE to 20f, JointId.RIGHT_HIP to 15f, JointId.RIGHT_KNEE to 5f))
        val stepR = SkeletalPose(PoseMarker.MID, mapOf(JointId.LEFT_HIP to 15f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_HIP to -20f, JointId.RIGHT_KNEE to 20f))
        return SkeletalTimeline(1.4f, listOf(Keyframe(0f, stepL), Keyframe(0.7f, stepR), Keyframe(1.4f, stepL)))
    }

    private fun olympicLiftTimeline(): SkeletalTimeline {
        val start = SkeletalPose(PoseMarker.BOTTOM, mapOf(JointId.PELVIS to 55f, JointId.LEFT_HIP to -90f, JointId.LEFT_KNEE to 75f, JointId.RIGHT_HIP to -90f, JointId.RIGHT_KNEE to 75f), Offset(0.5f, 0.55f))
        val catch = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_SHOULDER to -170f, JointId.LEFT_ELBOW to 10f, JointId.LEFT_HIP to -20f, JointId.LEFT_KNEE to 25f, JointId.RIGHT_SHOULDER to -170f, JointId.RIGHT_ELBOW to 10f, JointId.RIGHT_HIP to -20f, JointId.RIGHT_KNEE to 25f), Offset(0.5f, 0.44f))
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, start), Keyframe(1.1f, catch), Keyframe(2.2f, start)))
    }

    private fun calfRaiseTimeline(): SkeletalTimeline {
        val down = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_ANKLE to 15f, JointId.RIGHT_ANKLE to 15f))
        val up = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_ANKLE to -25f, JointId.RIGHT_ANKLE to -25f))
        return SkeletalTimeline(1.8f, listOf(Keyframe(0f, down), Keyframe(0.9f, up), Keyframe(1.8f, down)))
    }

    private fun coreRotationTimeline(): SkeletalTimeline {
        val left = SkeletalPose(PoseMarker.START, mapOf(JointId.CHEST to -20f))
        val right = SkeletalPose(PoseMarker.MID, mapOf(JointId.CHEST to 20f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, left), Keyframe(1.0f, right), Keyframe(2.0f, left)))
    }

    private fun legRaiseTimeline(): SkeletalTimeline {
        val hang = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_HIP to 5f, JointId.RIGHT_HIP to 5f))
        val raise = SkeletalPose(PoseMarker.TOP, mapOf(JointId.LEFT_HIP to -90f, JointId.LEFT_KNEE to 10f, JointId.RIGHT_HIP to -90f, JointId.RIGHT_KNEE to 10f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, hang), Keyframe(1.0f, raise), Keyframe(2.0f, hang)))
    }

    private fun pulloverTimeline(): SkeletalTimeline {
        val over = SkeletalPose(PoseMarker.STRETCH, mapOf(JointId.LEFT_SHOULDER to -155f, JointId.LEFT_ELBOW to 25f, JointId.RIGHT_SHOULDER to -155f, JointId.RIGHT_ELBOW to 25f))
        val chest = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.LEFT_SHOULDER to -75f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -75f, JointId.RIGHT_ELBOW to 15f))
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, over), Keyframe(1.1f, chest), Keyframe(2.2f, over)))
    }

    private fun facePullTimeline(): SkeletalTimeline {
        val reach = SkeletalPose(PoseMarker.STRETCH, mapOf(JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -80f, JointId.RIGHT_ELBOW to 10f))
        val pull = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.LEFT_SHOULDER to -95f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to -95f, JointId.RIGHT_ELBOW to 110f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, reach), Keyframe(1.0f, pull), Keyframe(2.0f, reach)))
    }

    private fun wristCurlTimeline(): SkeletalTimeline {
        val down = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_WRIST to 25f, JointId.RIGHT_WRIST to 25f))
        val up = SkeletalPose(PoseMarker.CONTRACTED, mapOf(JointId.LEFT_WRIST to -30f, JointId.RIGHT_WRIST to -30f))
        return SkeletalTimeline(1.6f, listOf(Keyframe(0f, down), Keyframe(0.8f, up), Keyframe(1.6f, down)))
    }

    private fun conditioningTimeline(): SkeletalTimeline {
        val a = SkeletalPose(PoseMarker.START, mapOf(JointId.LEFT_HIP to -40f, JointId.LEFT_KNEE to 45f, JointId.RIGHT_HIP to 20f, JointId.RIGHT_KNEE to 15f))
        val b = SkeletalPose(PoseMarker.MID, mapOf(JointId.LEFT_HIP to 20f, JointId.LEFT_KNEE to 15f, JointId.RIGHT_HIP to -40f, JointId.RIGHT_KNEE to 45f))
        return SkeletalTimeline(1.2f, listOf(Keyframe(0f, a), Keyframe(0.6f, b), Keyframe(1.2f, a)))
    }

    private fun defaultTimeline(): SkeletalTimeline {
        val stand = SkeletalPose(PoseMarker.IDLE, mapOf(JointId.LEFT_SHOULDER to 10f, JointId.RIGHT_SHOULDER to 10f))
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, stand), Keyframe(2.0f, stand)))
    }
}
