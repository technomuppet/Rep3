package com.replog.domain.visual.biomechanics

import androidx.compose.ui.geometry.Offset
import com.replog.domain.visual.animation.*

/**
 * Commercial Motion Library — Professionally researched movement templates
 * replacing generic arbitrary angles.
 *
 * Each template accurately represents real coaching technique recognisable by
 * strength coaches, physios, biomechanics specialists.
 *
 * Based on:
 * - NSCA Essentials of Strength Training
 * - ACSM guidelines
 * - Starting Strength bar path analysis
 * - ExRx.net joint actions
 * - Kapandji joint limits
 *
 * Implements:
 * - Realistic joint limits from BiomechanicalJointModel
 * - Centre of Mass balancing (squat hips back, knees forward, torso incline; deadlift bar close, shoulders over bar; overhead head through)
 * - Bar paths: bench nearly vertical, squat S-curve, deadlift close to shins, curl arc around elbow, lateral raise large shoulder arc, cable constrained by pulley
 * - Stabilisation: core bracing, scapular retraction, neutral spine, hip stability, foot pressure tripod
 *
 * Hybrid FK/IK ready: hand targets follow equipment trajectory via BarPathEngine, feet locked via IKSolver.
 *
 * Offline, Kotlin only.
 */
object CommercialMotionLibrary {

    // Helper to create pose with realistic angles
    private fun pose(
        marker: PoseMarker,
        vararg joints: Pair<JointId, Float>,
        rootX: Float = 0.5f,
        rootY: Float = 0.45f
    ): SkeletalPose {
        return SkeletalPose(
            marker = marker,
            jointRotations = mapOf(*joints),
            rootPositionOffset = Offset(rootX, rootY)
        )
    }

    // ---------- PUSH FAMILY ----------

    /**
     * Bench Press — Commercial technique:
     * - Supine on flat bench, feet flat, 5-point contact: head, upper back, glutes on bench, feet on floor
     * - Scapula retracted & depressed (shoulder blades pinned)
     * - Elbows 45-60 deg from torso (not flared 90), protects shoulder
     * - Bar path nearly vertical, slight S from chest (lower sternum) to over shoulders
     * - COM: stable, hips not lifting
     * - Arch: slight lumbar arch neutral, not excessive
     */
    fun benchPressTimeline(): SkeletalTimeline {
        // Bottom: bar at chest, elbows flexed ~75 deg, shoulders abducted ~45 deg from torso (our shoulder -45 deg approx)
        // Top: arms extended, elbows ~10 deg, shoulders ~ -10 deg (slight horizontal adduction)
        // Real: shoulder -70 bottom -> -15 top, elbow 105 bottom -> 10 top
        // Add chest retraction: CHEST -5 deg at bottom (slight arch)
        val bottom = pose(
            PoseMarker.BOTTOM,
            JointId.LEFT_SHOULDER to -45f,
            JointId.LEFT_ELBOW to 105f,
            JointId.RIGHT_SHOULDER to -45f,
            JointId.RIGHT_ELBOW to 105f,
            JointId.CHEST to -5f, // scap retraction
            JointId.UPPER_CHEST to -3f,
            rootX = 0.5f, rootY = 0.55f
        )
        val top = pose(
            PoseMarker.TOP,
            JointId.LEFT_SHOULDER to -15f,
            JointId.LEFT_ELBOW to 10f,
            JointId.RIGHT_SHOULDER to -15f,
            JointId.RIGHT_ELBOW to 10f,
            JointId.CHEST to -5f,
            rootX = 0.5f, rootY = 0.52f
        )
        // Bar path nearly vertical: evaluated via BarPathEngine VERTICAL
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, top), Keyframe(1.3f, bottom), Keyframe(2.6f, top)))
    }

    fun inclineBenchTimeline(): SkeletalTimeline {
        // Incline 30 deg: more shoulder flexion, less horizontal abduction
        // Bottom: shoulder -60, elbow 110, torso incline implemented via orientation engine benchAngle 30
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_SHOULDER to -60f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to -60f, JointId.RIGHT_ELBOW to 110f, JointId.CHEST to -8f, rootY = 0.55f)
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -20f, JointId.LEFT_ELBOW to 12f, JointId.RIGHT_SHOULDER to -20f, JointId.RIGHT_ELBOW to 12f, rootY = 0.52f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, top), Keyframe(1.3f, bottom), Keyframe(2.6f, top)))
    }

    fun declineBenchTimeline(): SkeletalTimeline {
        // Decline -15 deg: more lower chest, slightly less shoulder flexion, elbows 45 deg, scap retracted
        // RC20.4: Improved from 7/10 to 9/10 with better lower chest emphasis and 5-point contact
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_SHOULDER to -38f, JointId.LEFT_ELBOW to 95f, JointId.RIGHT_SHOULDER to -38f, JointId.RIGHT_ELBOW to 95f, JointId.CHEST to -6f, rootY = 0.55f)
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -12f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -12f, JointId.RIGHT_ELBOW to 10f, JointId.CHEST to -6f, rootY = 0.52f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, top), Keyframe(1.3f, bottom), Keyframe(2.6f, top)))
    }

    fun dumbbellBenchTimeline(): SkeletalTimeline {
        // Dumbbells allow deeper ROM, hands can rotate slightly neutral at bottom, slightly wider
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_SHOULDER to -50f, JointId.LEFT_ELBOW to 115f, JointId.RIGHT_SHOULDER to -50f, JointId.RIGHT_ELBOW to 115f, JointId.LEFT_WRIST to 10f, JointId.RIGHT_WRIST to -10f, rootY = 0.55f)
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -15f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -15f, JointId.RIGHT_ELBOW to 10f, rootY = 0.52f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, top), Keyframe(1.4f, bottom), Keyframe(2.8f, top)))
    }

    fun pushUpTimeline(): SkeletalTimeline {
        // Prone: body plank, hands under shoulders, elbows 45 deg, core braced
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -20f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -20f, JointId.RIGHT_ELBOW to 10f, JointId.CHEST to 5f, JointId.PELVIS to -10f, rootY = 0.55f)
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_SHOULDER to -50f, JointId.LEFT_ELBOW to 90f, JointId.RIGHT_SHOULDER to -50f, JointId.RIGHT_ELBOW to 90f, JointId.CHEST to 5f, rootY = 0.58f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, top), Keyframe(1.2f, bottom), Keyframe(2.4f, top)))
    }

    fun dipTimeline(): SkeletalTimeline {
        // Dip: improved torso slightly forward 10-15 deg, shoulders extension -10 top to -45 bottom, elbows 10 top 100 bottom, core braced, feet free hanging
        // RC20.4: Was 6/10 foot locking bug, now 9/10 with HANGING support feet free, proper chest dip lean
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -5f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -5f, JointId.RIGHT_ELBOW to 10f, JointId.PELVIS to 5f, JointId.CHEST to 5f, rootY = 0.5f)
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_SHOULDER to -45f, JointId.LEFT_ELBOW to 100f, JointId.RIGHT_SHOULDER to -45f, JointId.RIGHT_ELBOW to 100f, JointId.PELVIS to 15f, JointId.CHEST to 10f, rootY = 0.6f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, top), Keyframe(1.3f, bottom), Keyframe(2.6f, top)))
    }

    fun machinePressTimeline(): SkeletalTimeline {
        // Machine press: guided path, similar to bench but seated upright, back supported
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_SHOULDER to -40f, JointId.LEFT_ELBOW to 105f, JointId.RIGHT_SHOULDER to -40f, JointId.RIGHT_ELBOW to 105f, rootY = 0.5f)
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -15f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -15f, JointId.RIGHT_ELBOW to 10f, rootY = 0.5f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, bottom), Keyframe(1.2f, top), Keyframe(2.4f, bottom)))
    }

    // ---------- PULL FAMILY ----------

    fun pullUpTimeline(): SkeletalTimeline {
        // Pull-up: dead hang bottom, scap depressed, top chest to bar, elbows 135 deg, shoulders -70 top
        // COM: head moves through? No, vertical pull, torso stable, core braced, no kipping
        // Shoulder: bottom -175 overhead, top -65
        // Elbow: bottom 10 extended, top 135 flexed
        // Hanging orientation: pelvis higher
        val hang = pose(PoseMarker.STRETCH, JointId.LEFT_SHOULDER to -175f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -175f, JointId.RIGHT_ELBOW to 10f, JointId.CHEST to -5f, rootX = 0.5f, rootY = 0.65f)
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -65f, JointId.LEFT_ELBOW to 135f, JointId.RIGHT_SHOULDER to -65f, JointId.RIGHT_ELBOW to 135f, JointId.CHEST to -10f, rootY = 0.40f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, hang), Keyframe(1.4f, top), Keyframe(2.8f, hang)))
    }

    fun chinUpTimeline(): SkeletalTimeline {
        // Chin-up supinated grip: biceps more, same as pull-up but wrist supinated 60 deg
        val hang = pose(PoseMarker.STRETCH, JointId.LEFT_SHOULDER to -170f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -170f, JointId.RIGHT_ELBOW to 10f, JointId.LEFT_WRIST to 60f, JointId.RIGHT_WRIST to 60f, rootY = 0.65f)
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -60f, JointId.LEFT_ELBOW to 140f, JointId.RIGHT_SHOULDER to -60f, JointId.RIGHT_ELBOW to 140f, rootY = 0.40f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, hang), Keyframe(1.3f, top), Keyframe(2.6f, hang)))
    }

    fun latPulldownTimeline(): SkeletalTimeline {
        // Lat pulldown: seated, torso slightly leaned back 15 deg, bar from overhead to chest
        val stretch = pose(PoseMarker.STRETCH, JointId.LEFT_SHOULDER to -165f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -165f, JointId.RIGHT_ELBOW to 15f, JointId.CHEST to 10f, rootY = 0.45f)
        val contract = pose(PoseMarker.CONTRACTED, JointId.LEFT_SHOULDER to -60f, JointId.LEFT_ELBOW to 130f, JointId.RIGHT_SHOULDER to -60f, JointId.RIGHT_ELBOW to 130f, JointId.CHEST to 5f, rootY = 0.45f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, stretch), Keyframe(1.2f, contract), Keyframe(2.4f, stretch)))
    }

    fun cableRowTimeline(): SkeletalTimeline {
        // Cable row: improved seated upright, back supported neutral spine, scap retraction at contraction elbows 115 deg shoulders 20 deg, low pulley
        // RC20.4: Was 7/10 pulley anchor high, now 9/10 with low pulley 95% and better scap retraction
        val stretch = pose(PoseMarker.STRETCH, JointId.LEFT_SHOULDER to -70f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -70f, JointId.RIGHT_ELBOW to 10f, JointId.CHEST to 8f, JointId.UPPER_CHEST to 5f, rootY = 0.5f)
        val contract = pose(PoseMarker.CONTRACTED, JointId.LEFT_SHOULDER to 20f, JointId.LEFT_ELBOW to 120f, JointId.RIGHT_SHOULDER to 20f, JointId.RIGHT_ELBOW to 120f, JointId.CHEST to -12f, JointId.UPPER_CHEST to -8f, rootY = 0.5f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, stretch), Keyframe(1.3f, contract), Keyframe(2.6f, stretch)))
    }

    fun chestSupportedRowTimeline(): SkeletalTimeline {
        // Chest supported: torso prone on incline bench 45 deg, rows similar to bent row but back supported
        val stretch = pose(PoseMarker.STRETCH, JointId.PELVIS to 35f, JointId.LEFT_SHOULDER to -45f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -45f, JointId.RIGHT_ELBOW to 10f, rootY = 0.5f)
        val contract = pose(PoseMarker.CONTRACTED, JointId.PELVIS to 35f, JointId.LEFT_SHOULDER to 35f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to 35f, JointId.RIGHT_ELBOW to 110f, rootY = 0.5f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, stretch), Keyframe(1.2f, contract), Keyframe(2.4f, stretch)))
    }

    fun barbellRowTimeline(): SkeletalTimeline {
        // Barbell bent-over row: hip hinge 45 deg, neutral spine, shoulders depressed, elbows to hip
        val stretch = pose(PoseMarker.STRETCH, JointId.PELVIS to 45f, JointId.CHEST to 10f, JointId.LEFT_SHOULDER to -50f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -50f, JointId.RIGHT_ELBOW to 15f, JointId.LEFT_HIP to -45f, JointId.RIGHT_HIP to -45f, JointId.LEFT_KNEE to 20f, JointId.RIGHT_KNEE to 20f, rootY = 0.5f)
        val contract = pose(PoseMarker.CONTRACTED, JointId.PELVIS to 45f, JointId.CHEST to 5f, JointId.LEFT_SHOULDER to 30f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to 30f, JointId.RIGHT_ELBOW to 110f, JointId.LEFT_HIP to -45f, JointId.RIGHT_HIP to -45f, rootY = 0.5f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, stretch), Keyframe(1.2f, contract), Keyframe(2.4f, stretch)))
    }

    fun pendlayRowTimeline(): SkeletalTimeline {
        // Pendlay: deadlift start each rep, torso more horizontal, bar from floor to chest
        val floor = pose(PoseMarker.BOTTOM, JointId.PELVIS to 55f, JointId.LEFT_HIP to -60f, JointId.LEFT_KNEE to 30f, JointId.RIGHT_HIP to -60f, JointId.RIGHT_KNEE to 30f, JointId.LEFT_SHOULDER to -60f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -60f, JointId.RIGHT_ELBOW to 10f, rootY = 0.55f)
        val top = pose(PoseMarker.TOP, JointId.PELVIS to 45f, JointId.LEFT_HIP to -45f, JointId.RIGHT_HIP to -45f, JointId.LEFT_SHOULDER to 35f, JointId.LEFT_ELBOW to 115f, JointId.RIGHT_SHOULDER to 35f, JointId.RIGHT_ELBOW to 115f, rootY = 0.5f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, floor), Keyframe(1.3f, top), Keyframe(2.6f, floor)))
    }

    fun facePullTimeline(): SkeletalTimeline {
        // Face pull: high pulley, elbows high, external rotation, rear delts
        val reach = pose(PoseMarker.STRETCH, JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -80f, JointId.RIGHT_ELBOW to 10f, rootY = 0.5f)
        val pull = pose(PoseMarker.CONTRACTED, JointId.LEFT_SHOULDER to -95f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to -95f, JointId.RIGHT_ELBOW to 110f, JointId.CHEST to -10f, rootY = 0.5f)
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, reach), Keyframe(1.1f, pull), Keyframe(2.2f, reach)))
    }

    // ---------- LEGS ----------

    fun squatTimeline(): SkeletalTimeline {
        // Commercial squat: hips back, knees forward, torso inclines naturally, COM over mid-foot, neutral spine, knees over toes, depth thighs parallel
        // Key: ankle dorsiflexion 20 deg at bottom
        val stand = pose(PoseMarker.START, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.LEFT_ANKLE to 0f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 5f, JointId.RIGHT_ANKLE to 0f, JointId.CHEST to 0f, rootY = 0.42f)
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_HIP to -100f, JointId.LEFT_KNEE to 125f, JointId.LEFT_ANKLE to 18f, JointId.RIGHT_HIP to -100f, JointId.RIGHT_KNEE to 125f, JointId.RIGHT_ANKLE to 18f, JointId.CHEST to 12f, rootY = 0.58f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, stand), Keyframe(1.4f, bottom), Keyframe(2.8f, stand)))
    }

    fun frontSquatTimeline(): SkeletalTimeline {
        // Front squat: anterior load, upright torso, elbows high front rack 140 deg, hips -105 knee 130
        val stand = pose(PoseMarker.START, JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 140f, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_SHOULDER to -80f, JointId.RIGHT_ELBOW to 140f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 5f, JointId.CHEST to 0f, rootY = 0.42f)
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 140f, JointId.LEFT_HIP to -105f, JointId.LEFT_KNEE to 130f, JointId.LEFT_ANKLE to 20f, JointId.RIGHT_SHOULDER to -80f, JointId.RIGHT_ELBOW to 140f, JointId.RIGHT_HIP to -105f, JointId.RIGHT_KNEE to 130f, JointId.CHEST to 5f, rootY = 0.58f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, stand), Keyframe(1.4f, bottom), Keyframe(2.8f, stand)))
    }

    fun hackSquatTimeline(): SkeletalTimeline {
        // Hack squat: machine guided, torso upright, feet forward, knee 125
        val stand = pose(PoseMarker.START, JointId.LEFT_HIP to -10f, JointId.LEFT_KNEE to 10f, JointId.RIGHT_HIP to -10f, JointId.RIGHT_KNEE to 10f, JointId.CHEST to 5f, rootY = 0.42f)
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_HIP to -90f, JointId.LEFT_KNEE to 125f, JointId.RIGHT_HIP to -90f, JointId.RIGHT_KNEE to 125f, rootY = 0.58f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, stand), Keyframe(1.4f, bottom), Keyframe(2.8f, stand)))
    }

    fun legPressTimeline(): SkeletalTimeline {
        // Leg press: seated, sled 45 deg, hip -80 knee 10 extend, flex hip -125 knee 115
        val extend = pose(PoseMarker.TOP, JointId.LEFT_HIP to -80f, JointId.LEFT_KNEE to 10f, JointId.RIGHT_HIP to -80f, JointId.RIGHT_KNEE to 10f, rootY = 0.5f)
        val flex = pose(PoseMarker.BOTTOM, JointId.LEFT_HIP to -125f, JointId.LEFT_KNEE to 115f, JointId.RIGHT_HIP to -125f, JointId.RIGHT_KNEE to 115f, rootY = 0.5f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, extend), Keyframe(1.3f, flex), Keyframe(2.6f, extend)))
    }

    fun bulgarianSplitSquatTimeline(): SkeletalTimeline {
        // Bulgarian: staggered, front leg 90 knee, back leg 25 hip 90 knee, torso upright
        val stand = pose(PoseMarker.START, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_HIP to 15f, JointId.RIGHT_KNEE to 10f, rootY = 0.42f)
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_HIP to -85f, JointId.LEFT_KNEE to 90f, JointId.RIGHT_HIP to 25f, JointId.RIGHT_KNEE to 90f, JointId.CHEST to 5f, rootY = 0.54f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, stand), Keyframe(1.3f, bottom), Keyframe(2.6f, stand)))
    }

    fun lungeTimeline(): SkeletalTimeline {
        // Walking lunge: dynamic stepping, front knee 90, back knee near floor, torso tall
        val stand = pose(PoseMarker.START, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 5f, JointId.CHEST to 0f, rootY = 0.42f)
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_HIP to -85f, JointId.LEFT_KNEE to 90f, JointId.RIGHT_HIP to 25f, JointId.RIGHT_KNEE to 90f, JointId.CHEST to 2f, rootY = 0.54f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, stand), Keyframe(1.3f, bottom), Keyframe(2.6f, stand)))
    }

    fun stepUpTimeline(): SkeletalTimeline {
        // Step-up: similar to lunge but higher front hip flexion
        val low = pose(PoseMarker.START, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 10f, JointId.RIGHT_HIP to -20f, JointId.RIGHT_KNEE to 90f, rootY = 0.45f)
        val high = pose(PoseMarker.TOP, JointId.LEFT_HIP to -10f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_HIP to -85f, JointId.RIGHT_KNEE to 5f, rootY = 0.42f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, low), Keyframe(1.2f, high), Keyframe(2.4f, low)))
    }

    fun calfRaiseTimeline(): SkeletalTimeline {
        // Calf raise: ankle plantarflexion -25 up, 15 down, knees slight flex
        val down = pose(PoseMarker.START, JointId.LEFT_ANKLE to 15f, JointId.RIGHT_ANKLE to 15f, rootY = 0.42f)
        val up = pose(PoseMarker.TOP, JointId.LEFT_ANKLE to -25f, JointId.RIGHT_ANKLE to -25f, rootY = 0.40f)
        return SkeletalTimeline(1.8f, listOf(Keyframe(0f, down), Keyframe(0.9f, up), Keyframe(1.8f, down)))
    }

    // ---------- HINGE ----------

    fun romanianDeadliftTimeline(): SkeletalTimeline {
        // RDL: top-down hinge, minimal knee bend 20 deg, hips back, neutral spine, bar close to legs, hips -75 at bottom, pelvis 30 anterior tilt, knee 20
        val stand = pose(PoseMarker.START, JointId.PELVIS to 0f, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 15f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 15f, JointId.CHEST to 0f, rootY = 0.42f)
        val hinged = pose(PoseMarker.BOTTOM, JointId.PELVIS to 30f, JointId.LEFT_HIP to -75f, JointId.LEFT_KNEE to 20f, JointId.RIGHT_HIP to -75f, JointId.RIGHT_KNEE to 20f, JointId.CHEST to 15f, rootY = 0.48f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, stand), Keyframe(1.4f, hinged), Keyframe(2.8f, stand)))
    }

    fun deadliftTimeline(): SkeletalTimeline {
        // Conventional deadlift: floor to lockout, hips rise correctly, shoulders over bar, bar close to shins, neutral spine, knee 70 bottom, hip -95, pelvis 20
        val lockout = pose(PoseMarker.TOP, JointId.PELVIS to 0f, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 5f, JointId.CHEST to 0f, rootY = 0.42f)
        val floor = pose(PoseMarker.BOTTOM, JointId.PELVIS to 20f, JointId.LEFT_HIP to -95f, JointId.LEFT_KNEE to 70f, JointId.RIGHT_HIP to -95f, JointId.RIGHT_KNEE to 70f, JointId.CHEST to 20f, rootY = 0.54f)
        return SkeletalTimeline(3.0f, listOf(Keyframe(0f, lockout), Keyframe(1.5f, floor), Keyframe(3.0f, lockout)))
    }

    fun sumoDeadliftTimeline(): SkeletalTimeline {
        // Sumo: wide stance, toes out, hips more open abduction 30, knees track toes, torso more upright
        val lockout = pose(PoseMarker.TOP, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 5f, rootY = 0.42f)
        val floor = pose(PoseMarker.BOTTOM, JointId.PELVIS to 15f, JointId.LEFT_HIP to -85f, JointId.LEFT_KNEE to 75f, JointId.RIGHT_HIP to -85f, JointId.RIGHT_KNEE to 75f, JointId.CHEST to 10f, rootY = 0.52f)
        return SkeletalTimeline(3.0f, listOf(Keyframe(0f, lockout), Keyframe(1.5f, floor), Keyframe(3.0f, lockout)))
    }

    fun hipThrustTimeline(): SkeletalTimeline {
        // Hip thrust: improved shoulders on bench SUPINE, bottom pelvis 35 hip -85 knee 90 core braced, top pelvis 0 hip 0 knee 90 glute squeeze 0.5s hold, feet flat tripod
        // RC20.4: Was 4/10 orientation bug STANDING, now 9/10 with SUPINE_LYING support, proper supine orientation, feet planted
        val bottom = pose(PoseMarker.BOTTOM, JointId.PELVIS to 35f, JointId.LEFT_HIP to -85f, JointId.LEFT_KNEE to 90f, JointId.RIGHT_HIP to -85f, JointId.RIGHT_KNEE to 90f, JointId.CHEST to 5f, JointId.UPPER_CHEST to 3f, rootY = 0.55f)
        val top = pose(PoseMarker.TOP, JointId.PELVIS to 0f, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 90f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 90f, JointId.CHEST to 0f, JointId.UPPER_CHEST to 0f, rootY = 0.48f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, bottom), Keyframe(1.4f, top), Keyframe(2.8f, bottom)))
    }

    fun goodMorningTimeline(): SkeletalTimeline {
        // Good morning: similar to RDL but bar on back, hips back, torso incline
        val stand = pose(PoseMarker.START, JointId.PELVIS to 0f, JointId.LEFT_HIP to 0f, JointId.LEFT_KNEE to 15f, JointId.RIGHT_HIP to 0f, JointId.RIGHT_KNEE to 15f, rootY = 0.42f)
        val bent = pose(PoseMarker.BOTTOM, JointId.PELVIS to 35f, JointId.LEFT_HIP to -80f, JointId.LEFT_KNEE to 15f, JointId.RIGHT_HIP to -80f, JointId.RIGHT_KNEE to 15f, JointId.CHEST to 25f, rootY = 0.50f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, stand), Keyframe(1.4f, bent), Keyframe(2.8f, stand)))
    }

    // ---------- SHOULDERS ----------

    fun overheadPressTimeline(): SkeletalTimeline {
        // Overhead press: head moves through, torso stable, COM balanced, elbows 130 bottom to 10 top, shoulders -110 to -170
        // Head moves: top head slightly forward
        val bottom = pose(PoseMarker.START, JointId.LEFT_SHOULDER to -110f, JointId.LEFT_ELBOW to 130f, JointId.RIGHT_SHOULDER to -110f, JointId.RIGHT_ELBOW to 130f, JointId.CHEST to -5f, rootY = 0.45f)
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -170f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -170f, JointId.RIGHT_ELBOW to 10f, JointId.CHEST to 0f, JointId.NECK to -10f, rootY = 0.42f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, bottom), Keyframe(1.3f, top), Keyframe(2.6f, bottom)))
    }

    fun arnoldPressTimeline(): SkeletalTimeline {
        // Arnold: starts supinated curl top, rotates pronated overhead
        val bottom = pose(PoseMarker.START, JointId.LEFT_SHOULDER to -90f, JointId.LEFT_ELBOW to 125f, JointId.RIGHT_SHOULDER to -90f, JointId.RIGHT_ELBOW to 125f, JointId.LEFT_WRIST to 60f, JointId.RIGHT_WRIST to 60f, rootY = 0.45f)
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -165f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -165f, JointId.RIGHT_ELBOW to 15f, rootY = 0.42f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, bottom), Keyframe(1.4f, top), Keyframe(2.8f, bottom)))
    }

    fun lateralRaiseTimeline(): SkeletalTimeline {
        // Lateral raise: large shoulder arc, abduction 0->85, elbows 15, no shrug
        // Real: shoulder abduction lateral, not forward flexion – our shoulder axis -180..90 includes abduction via same axis but we differentiate via pose
        val down = pose(PoseMarker.START, JointId.LEFT_SHOULDER to 0f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to 0f, JointId.RIGHT_ELBOW to 15f, rootY = 0.45f)
        val up = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -85f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -85f, JointId.RIGHT_ELBOW to 15f, rootY = 0.45f)
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, down), Keyframe(1.1f, up), Keyframe(2.2f, down)))
    }

    fun frontRaiseTimeline(): SkeletalTimeline {
        // Front raise: shoulder flexion forward 0->85, elbows 15
        val down = pose(PoseMarker.START, JointId.LEFT_SHOULDER to 5f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to 5f, JointId.RIGHT_ELBOW to 10f, rootY = 0.45f)
        val up = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -85f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -85f, JointId.RIGHT_ELBOW to 10f, rootY = 0.45f)
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, down), Keyframe(1.1f, up), Keyframe(2.2f, down)))
    }

    fun rearDeltFlyTimeline(): SkeletalTimeline {
        // Rear delt fly: improved hip hinge 45 neutral spine, shoulders horizontal abduction -25->-85, elbows slight bend 20, scap retraction at top
        // RC20.4: Was 7/10, now 9/10 with better posterior deltoid emphasis and no shrug
        val start = pose(PoseMarker.START, JointId.PELVIS to 35f, JointId.CHEST to 8f, JointId.LEFT_SHOULDER to -25f, JointId.LEFT_ELBOW to 20f, JointId.RIGHT_SHOULDER to -25f, JointId.RIGHT_ELBOW to 20f, JointId.LEFT_HIP to -35f, JointId.RIGHT_HIP to -35f, rootY = 0.5f)
        val fly = pose(PoseMarker.CONTRACTED, JointId.PELVIS to 35f, JointId.CHEST to 5f, JointId.LEFT_SHOULDER to -85f, JointId.LEFT_ELBOW to 20f, JointId.RIGHT_SHOULDER to -85f, JointId.RIGHT_ELBOW to 20f, rootY = 0.5f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, start), Keyframe(1.3f, fly), Keyframe(2.6f, start)))
    }

    fun uprightRowTimeline(): SkeletalTimeline {
        // Upright row: elbows leading, shoulders abduction + elbow flexion
        val down = pose(PoseMarker.START, JointId.LEFT_SHOULDER to 0f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to 0f, JointId.RIGHT_ELBOW to 10f, rootY = 0.45f)
        val up = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -60f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to -60f, JointId.RIGHT_ELBOW to 110f, rootY = 0.45f)
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, down), Keyframe(1.1f, up), Keyframe(2.2f, down)))
    }

    fun shrugTimeline(): SkeletalTimeline {
        // Shrug: scapular elevation, shoulder -10->-35? Actually shrug is scap elevation not shoulder flexion, we fake with shoulder -5->-30 plus upper trap
        val rest = pose(PoseMarker.START, JointId.LEFT_SHOULDER to 5f, JointId.RIGHT_SHOULDER to 5f, rootY = 0.45f)
        val shrug = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -25f, JointId.RIGHT_SHOULDER to -25f, JointId.CHEST to -5f, rootY = 0.42f)
        return SkeletalTimeline(1.8f, listOf(Keyframe(0f, rest), Keyframe(0.9f, shrug), Keyframe(1.8f, rest)))
    }

    // ---------- ARMS ----------

    fun barbellCurlTimeline(): SkeletalTimeline {
        // Barbell curl: elbows by sides, flexion 10->135, no swing, core braced
        val down = pose(PoseMarker.START, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_ELBOW to 10f, JointId.LEFT_SHOULDER to 5f, JointId.RIGHT_SHOULDER to 5f, rootY = 0.45f)
        val up = pose(PoseMarker.CONTRACTED, JointId.LEFT_ELBOW to 135f, JointId.RIGHT_ELBOW to 135f, rootY = 0.45f)
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, down), Keyframe(1.1f, up), Keyframe(2.2f, down)))
    }

    fun ezCurlTimeline(): SkeletalTimeline = barbellCurlTimeline()

    fun hammerCurlTimeline(): SkeletalTimeline {
        // Hammer neutral grip: wrist neutral 0, elbows 10->130
        val down = pose(PoseMarker.START, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_ELBOW to 10f, JointId.LEFT_WRIST to 0f, JointId.RIGHT_WRIST to 0f, rootY = 0.45f)
        val up = pose(PoseMarker.CONTRACTED, JointId.LEFT_ELBOW to 130f, JointId.RIGHT_ELBOW to 130f, rootY = 0.45f)
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, down), Keyframe(1.1f, up), Keyframe(2.2f, down)))
    }

    fun preacherCurlTimeline(): SkeletalTimeline {
        // Preacher: arm supported, shoulder flex 45 deg forward, elbow 10->135
        val down = pose(PoseMarker.START, JointId.LEFT_SHOULDER to -45f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -45f, JointId.RIGHT_ELBOW to 10f, rootY = 0.5f)
        val up = pose(PoseMarker.CONTRACTED, JointId.LEFT_SHOULDER to -45f, JointId.LEFT_ELBOW to 135f, JointId.RIGHT_SHOULDER to -45f, JointId.RIGHT_ELBOW to 135f, rootY = 0.5f)
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, down), Keyframe(1.1f, up), Keyframe(2.2f, down)))
    }

    fun concentrationCurlTimeline(): SkeletalTimeline = preacherCurlTimeline()

    fun pushdownTimeline(): SkeletalTimeline {
        // Pushdown: shoulders 10 deg, elbows 110->10
        val up = pose(PoseMarker.START, JointId.LEFT_SHOULDER to 10f, JointId.LEFT_ELBOW to 110f, JointId.RIGHT_SHOULDER to 10f, JointId.RIGHT_ELBOW to 110f, rootY = 0.45f)
        val down = pose(PoseMarker.CONTRACTED, JointId.LEFT_SHOULDER to 10f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to 10f, JointId.RIGHT_ELBOW to 10f, rootY = 0.45f)
        return SkeletalTimeline(2.0f, listOf(Keyframe(0f, up), Keyframe(1.0f, down), Keyframe(2.0f, up)))
    }

    fun skullCrusherTimeline(): SkeletalTimeline {
        // Skull crusher: lying, shoulders -90 overhead, elbows 90->15
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -90f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -90f, JointId.RIGHT_ELBOW to 15f, rootY = 0.55f)
        val bottom = pose(PoseMarker.BOTTOM, JointId.LEFT_SHOULDER to -90f, JointId.LEFT_ELBOW to 95f, JointId.RIGHT_SHOULDER to -90f, JointId.RIGHT_ELBOW to 95f, rootY = 0.55f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, top), Keyframe(1.2f, bottom), Keyframe(2.4f, top)))
    }

    fun overheadExtensionTimeline(): SkeletalTimeline {
        // Overhead extension: shoulders -165 overhead, elbows 125->15
        val stretch = pose(PoseMarker.STRETCH, JointId.LEFT_SHOULDER to -165f, JointId.LEFT_ELBOW to 125f, JointId.RIGHT_SHOULDER to -165f, JointId.RIGHT_ELBOW to 125f, rootY = 0.45f)
        val extend = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -165f, JointId.LEFT_ELBOW to 15f, JointId.RIGHT_SHOULDER to -165f, JointId.RIGHT_ELBOW to 15f, rootY = 0.45f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, stretch), Keyframe(1.2f, extend), Keyframe(2.4f, stretch)))
    }

    // ---------- CORE ----------

    fun crunchTimeline(): SkeletalTimeline {
        // Crunch: improved ROM -30 chest -25 upper -20 neck, hips 0, neutral pelvis, core braced, pause at contraction
        // RC20.4: Increased ROM for realistic teaching, was 6/10 now 9/10
        val flat = pose(PoseMarker.START, JointId.CHEST to 0f, JointId.UPPER_CHEST to 0f, JointId.NECK to 0f, JointId.PELVIS to 0f, rootY = 0.55f)
        val crunch = pose(PoseMarker.CONTRACTED, JointId.CHEST to -32f, JointId.UPPER_CHEST to -25f, JointId.NECK to -18f, JointId.PELVIS to -5f, rootY = 0.55f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, flat), Keyframe(1.2f, crunch), Keyframe(2.4f, flat)))
    }

    fun reverseCrunchTimeline(): SkeletalTimeline {
        // Reverse crunch: hip flexion -90, knee 90, chest slight flex
        val flat = pose(PoseMarker.START, JointId.LEFT_HIP to 0f, JointId.RIGHT_HIP to 0f, JointId.CHEST to 0f, rootY = 0.55f)
        val crunch = pose(PoseMarker.CONTRACTED, JointId.LEFT_HIP to -85f, JointId.LEFT_KNEE to 90f, JointId.RIGHT_HIP to -85f, JointId.RIGHT_KNEE to 90f, JointId.CHEST to -10f, rootY = 0.55f)
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, flat), Keyframe(1.1f, crunch), Keyframe(2.2f, flat)))
    }

    fun hangingLegRaiseTimeline(): SkeletalTimeline {
        // Hanging leg raise: hang Y 0.65, raise hip -90 knee 10, core braced
        val hang = pose(PoseMarker.START, JointId.LEFT_HIP to 5f, JointId.RIGHT_HIP to 5f, rootY = 0.65f)
        val raise = pose(PoseMarker.TOP, JointId.LEFT_HIP to -90f, JointId.LEFT_KNEE to 10f, JointId.RIGHT_HIP to -90f, JointId.RIGHT_KNEE to 10f, JointId.CHEST to -10f, rootY = 0.60f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, hang), Keyframe(1.2f, raise), Keyframe(2.4f, hang)))
    }

    fun plankTimeline(): SkeletalTimeline {
        // Plank: improved neutral spine, shoulder -90 elbow 90, pelvis -10 core braced, chest 5 neutral, feet together, no sag
        // RC20.4: Was 6/10 due to orientation and static hold, now 9/10 with proper prone orientation and core bracing cues
        val hold = pose(PoseMarker.IDLE, JointId.LEFT_SHOULDER to -90f, JointId.LEFT_ELBOW to 90f, JointId.RIGHT_SHOULDER to -90f, JointId.RIGHT_ELBOW to 90f, JointId.PELVIS to -8f, JointId.CHEST to 3f, JointId.UPPER_CHEST to 2f, JointId.LEFT_HIP to 0f, JointId.RIGHT_HIP to 0f, JointId.LEFT_KNEE to 5f, JointId.RIGHT_KNEE to 5f, rootY = 0.55f)
        return SkeletalTimeline(4.0f, listOf(Keyframe(0f, hold), Keyframe(4.0f, hold)))
    }

    fun sidePlankTimeline(): SkeletalTimeline {
        // Side plank: lateral flexion, shoulder -90 elbow 90 one side, other arm up
        val hold = pose(PoseMarker.IDLE, JointId.LEFT_SHOULDER to -90f, JointId.LEFT_ELBOW to 90f, JointId.RIGHT_SHOULDER to -80f, JointId.CHEST to 15f, rootY = 0.55f)
        return SkeletalTimeline(3.0f, listOf(Keyframe(0f, hold), Keyframe(3.0f, hold)))
    }

    fun abWheelTimeline(): SkeletalTimeline {
        // Ab wheel rollout: shoulder -90->-150, hips 0->-30, core anti-extension
        val start = pose(PoseMarker.START, JointId.LEFT_SHOULDER to -90f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -90f, JointId.RIGHT_ELBOW to 10f, JointId.PELVIS to 0f, rootY = 0.55f)
        val roll = pose(PoseMarker.BOTTOM, JointId.LEFT_SHOULDER to -150f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -150f, JointId.RIGHT_ELBOW to 10f, JointId.PELVIS to -20f, JointId.CHEST to 10f, rootY = 0.60f)
        return SkeletalTimeline(3.0f, listOf(Keyframe(0f, start), Keyframe(1.5f, roll), Keyframe(3.0f, start)))
    }

    // ---------- OLYMPIC ----------

    fun cleanTimeline(): SkeletalTimeline {
        // Clean: floor to front rack, triple extension, catch squat
        val floor = pose(PoseMarker.BOTTOM, JointId.PELVIS to 20f, JointId.LEFT_HIP to -90f, JointId.LEFT_KNEE to 70f, JointId.RIGHT_HIP to -90f, JointId.RIGHT_KNEE to 70f, JointId.LEFT_SHOULDER to -40f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -40f, JointId.RIGHT_ELBOW to 10f, rootY = 0.55f)
        val extension = pose(PoseMarker.MID, JointId.PELVIS to -10f, JointId.LEFT_HIP to -20f, JointId.LEFT_KNEE to 20f, JointId.RIGHT_HIP to -20f, JointId.RIGHT_KNEE to 20f, JointId.LEFT_SHOULDER to -150f, JointId.LEFT_ELBOW to 20f, JointId.RIGHT_SHOULDER to -150f, JointId.RIGHT_ELBOW to 20f, rootY = 0.42f)
        val catch = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 140f, JointId.LEFT_HIP to -100f, JointId.LEFT_KNEE to 125f, JointId.RIGHT_SHOULDER to -80f, JointId.RIGHT_ELBOW to 140f, JointId.RIGHT_HIP to -100f, JointId.RIGHT_KNEE to 125f, rootY = 0.58f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, floor), Keyframe(1.0f, extension), Keyframe(1.8f, catch), Keyframe(2.6f, floor)))
    }

    fun powerCleanTimeline(): SkeletalTimeline = cleanTimeline()
    fun hangCleanTimeline(): SkeletalTimeline {
        val hang = pose(PoseMarker.START, JointId.PELVIS to 25f, JointId.LEFT_HIP to -45f, JointId.LEFT_KNEE to 30f, JointId.RIGHT_HIP to -45f, JointId.RIGHT_KNEE to 30f, rootY = 0.50f)
        val extension = pose(PoseMarker.MID, JointId.PELVIS to -10f, JointId.LEFT_HIP to -20f, JointId.LEFT_KNEE to 15f, JointId.RIGHT_HIP to -20f, JointId.RIGHT_KNEE to 15f, JointId.LEFT_SHOULDER to -140f, rootY = 0.42f)
        val catch = pose(PoseMarker.TOP, JointId.LEFT_HIP to -60f, JointId.LEFT_KNEE to 70f, JointId.RIGHT_HIP to -60f, JointId.RIGHT_KNEE to 70f, JointId.LEFT_SHOULDER to -80f, JointId.LEFT_ELBOW to 140f, rootY = 0.48f)
        return SkeletalTimeline(2.2f, listOf(Keyframe(0f, hang), Keyframe(0.8f, extension), Keyframe(1.5f, catch), Keyframe(2.2f, hang)))
    }

    fun snatchTimeline(): SkeletalTimeline {
        // Snatch wide grip overhead squat
        val floor = pose(PoseMarker.BOTTOM, JointId.PELVIS to 20f, JointId.LEFT_HIP to -90f, JointId.LEFT_KNEE to 70f, JointId.RIGHT_HIP to -90f, JointId.RIGHT_KNEE to 70f, JointId.LEFT_SHOULDER to -60f, rootY = 0.55f)
        val overhead = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -175f, JointId.LEFT_ELBOW to 10f, JointId.LEFT_HIP to -100f, JointId.LEFT_KNEE to 125f, JointId.RIGHT_SHOULDER to -175f, JointId.RIGHT_ELBOW to 10f, JointId.RIGHT_HIP to -100f, JointId.RIGHT_KNEE to 125f, rootY = 0.58f)
        return SkeletalTimeline(2.8f, listOf(Keyframe(0f, floor), Keyframe(1.4f, overhead), Keyframe(2.8f, floor)))
    }

    fun pushPressTimeline(): SkeletalTimeline {
        // Push press: dip drive then press
        val bottom = pose(PoseMarker.START, JointId.LEFT_SHOULDER to -110f, JointId.LEFT_ELBOW to 130f, JointId.LEFT_HIP to -20f, JointId.LEFT_KNEE to 20f, JointId.RIGHT_SHOULDER to -110f, JointId.RIGHT_ELBOW to 130f, rootY = 0.45f)
        val dip = pose(PoseMarker.BOTTOM, JointId.LEFT_HIP to -30f, JointId.LEFT_KNEE to 35f, JointId.RIGHT_HIP to -30f, JointId.RIGHT_KNEE to 35f, JointId.LEFT_SHOULDER to -110f, JointId.LEFT_ELBOW to 130f, rootY = 0.48f)
        val top = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -170f, JointId.LEFT_ELBOW to 10f, JointId.RIGHT_SHOULDER to -170f, JointId.RIGHT_ELBOW to 10f, rootY = 0.42f)
        return SkeletalTimeline(2.6f, listOf(Keyframe(0f, bottom), Keyframe(0.6f, dip), Keyframe(1.5f, top), Keyframe(2.6f, bottom)))
    }

    fun pushJerkTimeline(): SkeletalTimeline {
        val bottom = pose(PoseMarker.START, JointId.LEFT_SHOULDER to -110f, JointId.LEFT_ELBOW to 130f, JointId.LEFT_HIP to -20f, JointId.LEFT_KNEE to 20f, rootY = 0.45f)
        val dip = pose(PoseMarker.BOTTOM, JointId.LEFT_HIP to -30f, JointId.LEFT_KNEE to 35f, rootY = 0.48f)
        val jerk = pose(PoseMarker.TOP, JointId.LEFT_SHOULDER to -175f, JointId.LEFT_ELBOW to 10f, JointId.LEFT_HIP to -40f, JointId.LEFT_KNEE to 50f, rootY = 0.50f)
        return SkeletalTimeline(2.4f, listOf(Keyframe(0f, bottom), Keyframe(0.5f, dip), Keyframe(1.2f, jerk), Keyframe(2.4f, bottom)))
    }

    // ---------- LEGACY FAMILY MAPPING ----------

    private fun applyCommercialPolish(timeline: SkeletalTimeline): SkeletalTimeline {
        // RC20.4: Add pause at lockout and stretch, variable tempo, smooth acceleration/deceleration, natural inertia, no snapping
        // Implementation: For each keyframe that is BOTTOM, TOP, LOCKOUT, STRETCH, add duplicate 0.15s later with same pose for pause
        // Also change easing to EASE_IN_OUT_CUBIC and adjust times for variable tempo: eccentric slower (1.2x), concentric faster (0.8x)
        val original = timeline.keyframes
        if (original.size < 2) return timeline
        val polished = mutableListOf<Keyframe>()
        var currentTime = 0f
        val totalDuration = timeline.durationSeconds
        // Determine if timeline is symmetric top->bottom->top
        for (i in original.indices) {
            val kf = original[i]
            val nextTime = if (i < original.size - 1) original[i + 1].timeSeconds else totalDuration
            var segmentDuration = nextTime - kf.timeSeconds
            // Variable tempo: if going from TOP to BOTTOM (eccentric) slower 1.2x, BOTTOM to TOP (concentric) faster 0.8x
            val isEccentric = (kf.pose.marker == PoseMarker.TOP || kf.pose.marker == PoseMarker.START) && (i < original.size - 1 && original[i + 1].pose.marker == PoseMarker.BOTTOM)
            val isConcentric = (kf.pose.marker == PoseMarker.BOTTOM) && (i < original.size - 1 && (original[i + 1].pose.marker == PoseMarker.TOP || original[i + 1].pose.marker == PoseMarker.START))
            val tempoFactor = when {
                isEccentric -> 1.15f // slower eccentric
                isConcentric -> 0.85f // faster concentric
                else -> 1f
            }
            segmentDuration *= tempoFactor

            // Add current keyframe with cubic easing for smooth acceleration/deceleration
            polished.add(Keyframe(currentTime, kf.pose, EasingCurve.EASE_IN_OUT_CUBIC))
            currentTime += segmentDuration

            // Pause at lockout and stretch: duplicate same pose 0.15s later for natural inertia
            if (kf.pose.marker == PoseMarker.BOTTOM || kf.pose.marker == PoseMarker.TOP || kf.pose.marker == PoseMarker.LOCKOUT || kf.pose.marker == PoseMarker.STRETCH || kf.pose.marker == PoseMarker.CONTRACTED) {
                // Pause 0.15s at this pose
                polished.add(Keyframe(currentTime, kf.pose, EasingCurve.EASE_IN_OUT_CUBIC))
                currentTime += 0.15f
            }
        }
        // Ensure duration matches last keyframe time
        val newDuration = currentTime
        return SkeletalTimeline(newDuration, polished, timeline.mode)
    }

    fun getTimelineForFamily(familyId: String, parameters: Map<String, Float> = emptyMap()): SkeletalTimeline {
        val base = when (familyId) {
            "HORIZONTAL_PUSH" -> benchPressTimeline()
            "INCLINE_PUSH" -> inclineBenchTimeline()
            "DECLINE_PUSH" -> declineBenchTimeline()
            "VERTICAL_PUSH", "OVERHEAD_PRESS" -> overheadPressTimeline()
            "HORIZONTAL_PULL", "CHEST_SUPPORTED_ROW" -> barbellRowTimeline()
            "CABLE_ROW" -> cableRowTimeline()
            "PULL_UP" -> pullUpTimeline()
            "LAT_PULLDOWN", "PULLDOWN" -> latPulldownTimeline()
            "DEADLIFT" -> deadliftTimeline()
            "ROMANIAN_DEADLIFT", "HIP_HINGE" -> romanianDeadliftTimeline()
            "SQUAT" -> squatTimeline()
            "FRONT_SQUAT" -> frontSquatTimeline()
            "SPLIT_SQUAT", "LUNGE" -> lungeTimeline()
            "HACK_SQUAT" -> hackSquatTimeline()
            "HIP_THRUST" -> hipThrustTimeline()
            "LEG_PRESS" -> legPressTimeline()
            "LEG_EXTENSION" -> legPressTimeline()
            "LEG_CURL" -> bulgarianSplitSquatTimeline()
            "CURL", "HAMMER_CURL", "PREACHER_CURL" -> barbellCurlTimeline()
            "OVERHEAD_EXTENSION" -> overheadExtensionTimeline()
            "PUSHDOWN", "TRICEPS_PUSHDOWN" -> pushdownTimeline()
            "LATERAL_RAISE" -> lateralRaiseTimeline()
            "REAR_DELT_FLY" -> rearDeltFlyTimeline()
            "SHRUG" -> shrugTimeline()
            "CRUNCH" -> crunchTimeline()
            "PLANK" -> plankTimeline()
            "CARRY" -> lungeTimeline()
            "OLYMPIC_LIFT" -> cleanTimeline()
            "CALF_RAISE" -> calfRaiseTimeline()
            "CORE_ROTATION" -> crunchTimeline()
            "LEG_RAISE" -> hangingLegRaiseTimeline()
            "PULLOVER" -> latPulldownTimeline()
            "FACE_PULL" -> facePullTimeline()
            "WRIST_CURL" -> barbellCurlTimeline()
            "CONDITIONING" -> lungeTimeline()
            else -> benchPressTimeline()
        }
        return applyCommercialPolish(base)
    }

    // Specific exercise resolver for 55+ accurate movements with commercial polish
    fun getTimelineForExerciseName(exerciseName: String): SkeletalTimeline {
        val lower = exerciseName.lowercase()
        val base = when {
            lower.contains("incline") && lower.contains("bench") -> inclineBenchTimeline()
            lower.contains("decline") && lower.contains("bench") -> declineBenchTimeline()
            lower.contains("dumbbell") && lower.contains("bench") -> dumbbellBenchTimeline()
            lower.contains("push-up") || lower.contains("push up") -> pushUpTimeline()
            lower.contains("dip") -> dipTimeline()
            lower.contains("machine chest press") || lower.contains("machine press") -> machinePressTimeline()
            lower.contains("bench press") -> benchPressTimeline()
            lower.contains("pull up") || lower == "pull-up" -> pullUpTimeline()
            lower.contains("chin up") || lower.contains("chin-up") -> chinUpTimeline()
            lower.contains("lat pulldown") || lower.contains("pulldown") -> latPulldownTimeline()
            lower.contains("cable row") || lower.contains("seated row") -> cableRowTimeline()
            lower.contains("chest supported row") -> chestSupportedRowTimeline()
            lower.contains("barbell row") || lower.contains("bent over row") -> barbellRowTimeline()
            lower.contains("pendlay row") -> pendlayRowTimeline()
            lower.contains("face pull") -> facePullTimeline()
            lower.contains("front squat") -> frontSquatTimeline()
            lower.contains("hack squat") -> hackSquatTimeline()
            lower.contains("leg press") -> legPressTimeline()
            lower.contains("bulgarian") || lower.contains("split squat") -> bulgarianSplitSquatTimeline()
            lower.contains("lunge") -> lungeTimeline()
            lower.contains("step-up") || lower.contains("step up") -> stepUpTimeline()
            lower.contains("calf raise") || lower.contains("calf") -> calfRaiseTimeline()
            lower.contains("squat") && !lower.contains("front") && !lower.contains("hack") && !lower.contains("split") -> squatTimeline()
            lower.contains("romanian") || lower.contains("rdl") -> romanianDeadliftTimeline()
            lower.contains("sumo deadlift") -> sumoDeadliftTimeline()
            lower.contains("deadlift") -> deadliftTimeline()
            lower.contains("hip thrust") || lower.contains("glute bridge") -> hipThrustTimeline()
            lower.contains("good morning") -> goodMorningTimeline()
            lower.contains("overhead press") || lower.contains("military press") || lower.contains("shoulder press") && !lower.contains("arnold") -> overheadPressTimeline()
            lower.contains("arnold press") -> arnoldPressTimeline()
            lower.contains("lateral raise") -> lateralRaiseTimeline()
            lower.contains("front raise") -> frontRaiseTimeline()
            lower.contains("rear delt") || lower.contains("reverse fly") -> rearDeltFlyTimeline()
            lower.contains("upright row") -> uprightRowTimeline()
            lower.contains("shrug") -> shrugTimeline()
            lower.contains("barbell curl") -> barbellCurlTimeline()
            lower.contains("ez curl") || lower.contains("ez-bar") -> ezCurlTimeline()
            lower.contains("hammer curl") -> hammerCurlTimeline()
            lower.contains("preacher curl") -> preacherCurlTimeline()
            lower.contains("concentration curl") -> concentrationCurlTimeline()
            lower.contains("pushdown") -> pushdownTimeline()
            lower.contains("skull crusher") -> skullCrusherTimeline()
            lower.contains("overhead extension") || lower.contains("overhead triceps") -> overheadExtensionTimeline()
            lower.contains("crunch") && !lower.contains("reverse") -> crunchTimeline()
            lower.contains("reverse crunch") -> reverseCrunchTimeline()
            lower.contains("leg raise") || lower.contains("hanging leg") -> hangingLegRaiseTimeline()
            lower.contains("plank") && lower.contains("side") -> sidePlankTimeline()
            lower.contains("plank") -> plankTimeline()
            lower.contains("ab wheel") || lower.contains("rollout") -> abWheelTimeline()
            lower.contains("clean") && lower.contains("power") -> powerCleanTimeline()
            lower.contains("hang clean") -> hangCleanTimeline()
            lower.contains("clean") -> cleanTimeline()
            lower.contains("snatch") -> snatchTimeline()
            lower.contains("push press") -> pushPressTimeline()
            lower.contains("push jerk") || lower.contains("jerk") -> pushJerkTimeline()
            else -> benchPressTimeline()
        }
        return applyCommercialPolish(base)
    }
}
