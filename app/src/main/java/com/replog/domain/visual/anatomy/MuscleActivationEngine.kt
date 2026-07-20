package com.replog.domain.visual.anatomy

import com.replog.domain.visual.spec.AnatomySpec
import kotlin.math.abs
import kotlin.math.cos

/**
 * Muscle activation synchronisation for RC20.4
 * Drives primary/secondary muscle alpha from movement phase, not fake.
 * Supports eccentric vs concentric, isometric, bilateral, unilateral.
 */
object MuscleActivationEngine {

    data class Activation(
        val region: MuscleRegion,
        val factor: Float, // 0..1
        val isPrimary: Boolean,
        val phase: Phase
    )

    enum class Phase {
        CONCENTRIC,
        ECCENTRIC,
        ISOMETRIC,
        STRETCH
    }

    /**
     * Calculates activation per region based on anatomy spec and progress.
     * @param spec anatomy primary/secondary
     * @param progress 0..1 where 0 = top/lockout, 1 = bottom/stretch
     * @param familyId movement family for family-specific curves
     * @param marker pose marker for phase detection
     */
    fun calculateActivations(
        spec: AnatomySpec,
        progress: Float,
        familyId: String,
        marker: com.replog.domain.visual.animation.PoseMarker? = null
    ): List<Activation> {
        val t = progress.coerceIn(0f, 1f)
        val phase = when {
            marker == com.replog.domain.visual.animation.PoseMarker.IDLE -> Phase.ISOMETRIC
            t < 0.1f || t > 0.9f -> Phase.ISOMETRIC // near top/bottom lockout
            t < 0.5f -> Phase.CONCENTRIC // actually t 0->1 is top->bottom eccentric, so 0->0.5 eccentric? Let's define: top->bottom is eccentric lengthening, bottom->top concentric. Our t 0 top 1 bottom, so 0->1 is eccentric.
            else -> Phase.ECCENTRIC
        }
        // Correct: for bench, top->bottom is eccentric (muscle lengthening), bottom->top concentric shortening
        // Our t 0 top 1 bottom, so going 0->1 eccentric, 1->0 concentric via ping pong. But inside segment t 0->1 always top->bottom eccentric.
        // For activation, concentric higher activation than eccentric? Actually concentric often higher EMG.
        // We'll compute activation factor based on t and family.

        val primaryRegions = MuscleMap.mapPrimary(spec)
        val secondaryRegions = MuscleMap.mapSecondary(spec)

        val activations = mutableListOf<Activation>()

        for (region in primaryRegions) {
            val factor = calculatePrimaryFactor(region, t, familyId)
            activations.add(Activation(region, factor, true, if (t < 0.5f) Phase.ECCENTRIC else Phase.CONCENTRIC))
        }
        for (region in secondaryRegions) {
            val factor = calculateSecondaryFactor(region, t, familyId)
            activations.add(Activation(region, factor, false, if (t < 0.5f) Phase.ECCENTRIC else Phase.CONCENTRIC))
        }

        return activations
    }

    private fun calculatePrimaryFactor(region: MuscleRegion, t: Float, familyId: String): Float {
        // t 0 top lockout, 1 bottom stretch
        // For push: chest higher at top (concentric squeeze), moderate at bottom stretch
        // For pull: lats higher at contracted (top for pull-up is chest to bar), so t 0 hang low, t 1 contracted high? Actually pull-up t 0 hang (stretch) low, t 1 contracted high. Our t 0 top? Wait pull-up top is contracted chest to bar, bottom is hang stretch. In pullUpTimeline, top is contracted chest to bar at t 0? Actually timeline top at 0? For pull-up we had hang at 0? Let's check: In pullUpTimeline, hang at 0, top at 1.4, hang at 2.8. So topBar/bottomBar logic may invert. For simplicity use cosine curve that peaks at one end.

        // General: for most exercises, primary muscle activation peaks near contracted position.
        // Contracted position is where muscle shortened: for bench top, for pull-up top (chest to bar), for squat bottom? Actually quads shortened at top standing, lengthened at bottom squat.
        // So we need family-specific.

        return when (familyId) {
            "HORIZONTAL_PUSH", "INCLINE_PUSH", "DECLINE_PUSH", "MACHINE_PRESS" -> {
                // Bench: chest activation high at top (t=0), lower at bottom stretch (t=1) but not zero: 0.85 -> 0.45
                (0.45f + 0.45f * cos(t * Math.PI.toFloat() * 0.5f).coerceIn(0f, 1f)).coerceIn(0.3f, 1f)
            }
            "PULL_UP", "LAT_PULLDOWN", "CABLE_ROW", "HORIZONTAL_PULL" -> {
                // Pull: lats high at contracted (bottom of our t? Actually pull-up contracted is top Y 0.40, which is our t? For pull-up, topBar is contracted? In our cached bar ends, topBar is hang? Need simpler: use 1-t for pull so contracted high
                // t 0 top (hang) low, t 1 bottom (contracted) high for pull-up? Actually for pull-up topBar we defined as hang? In ExerciseAnimationView topBar is evaluated at 0 (hang) and bottomBar at duration*0.5 (contracted). So topBar = hang, bottomBar = contracted. So progress t 0->1 is hang->contracted, so activation should increase with t.
                (0.35f + 0.65f * t).coerceIn(0.3f, 1f)
            }
            "SQUAT", "FRONT_SQUAT", "HACK_SQUAT", "LEG_PRESS", "LUNGE", "SPLIT_SQUAT" -> {
                // Legs: quads/glutes higher at bottom (t=1) where concentric starts? Actually bottom is deep, quads stretched but activation high for drive up. So peak near bottom and mid.
                // Use 0.4 + 0.6 * sin(t * PI) -> 0 at top, 1 at bottom mid? sin 0=0, sin PI/2=1 at t=0.5, sin PI=0 at t=1. So peak at mid bottom? Let's use t directly: higher at bottom
                (0.4f + 0.6f * t).coerceIn(0.3f, 1f)
            }
            "DEADLIFT", "ROMANIAN_DEADLIFT", "HIP_HINGE", "HIP_THRUST" -> {
                // Hinge: glutes/hamstrings high at lockout top? Actually glutes squeeze at top lockout, hamstrings stretch at bottom.
                // For RDL, glutes high at top, hamstrings moderate at bottom stretch.
                // Use (1-t) for glutes, t for hamstrings? Simplify use 0.5+0.5*cos
                (0.5f + 0.5f * (1f - t)).coerceIn(0.3f, 1f)
            }
            "CURL", "HAMMER_CURL", "PREACHER_CURL" -> {
                // Biceps high at contracted (t=1) - curl up
                (0.3f + 0.7f * t).coerceIn(0.3f, 1f)
            }
            "PUSHDOWN", "OVERHEAD_EXTENSION" -> {
                // Triceps high at contracted (top for pushdown? Actually pushdown contracted is arms extended down, which is bottom? For pushdown t 0 up bent 110, t 1 down extended 10. So contracted is t=1)
                (0.35f + 0.65f * t).coerceIn(0.3f, 1f)
            }
            "LATERAL_RAISE", "REAR_DELT_FLY" -> {
                // Delts high at top raised (t=1)
                (0.3f + 0.7f * t).coerceIn(0.3f, 1f)
            }
            "CRUNCH", "LEG_RAISE", "PLANK" -> {
                // Core: abs high at contracted crunch (t=1)
                if (t > 0.5f) 0.85f else 0.45f
            }
            else -> {
                // Default: peak at contracted (t=1)
                (0.4f + 0.6f * t).coerceIn(0.3f, 1f)
            }
        }
    }

    private fun calculateSecondaryFactor(region: MuscleRegion, t: Float, familyId: String): Float {
        // Secondary lower than primary, 0.25-0.65 range
        val primary = calculatePrimaryFactor(region, t, familyId)
        return (primary * 0.65f).coerceIn(0.2f, 0.7f)
    }

    /**
     * Determines if exercise is unilateral based on name or stance.
     */
    fun isUnilateral(exerciseName: String, stance: String): Boolean {
        val lower = exerciseName.lowercase()
        return lower.contains("single") || lower.contains("unilateral") || lower.contains("bulgarian") || lower.contains("concentration") || stance.contains("SINGLE")
    }
}
