package com.replog.ui.exercise.adapter

import android.util.Log
import com.replog.data.model.Exercise
import com.replog.domain.library.AnimationClip
import com.replog.domain.library.ExerciseAnimation
import com.replog.domain.visual.animation.KinematicMovementFamilies
import com.replog.domain.visual.animation.SkeletalTimeline
import com.replog.domain.visual.resolver.ExerciseVisualResolver
import com.replog.domain.visual.spec.AnatomySpec
import com.replog.domain.visual.spec.ExerciseVisualSpec

/**
 * UI compatibility facade and adapter bridging legacy presentation layer components
 * (`ExerciseAnimationView`, `MuscleBodyDiagram`) to the new rotational Skeletal Animation
 * Engine and vector Anatomical Muscle Renderer. Guarantees safe fallback mechanisms.
 */
object VisualEngineAdapter {

    private const val TAG = "VisualEngineAdapter"

    sealed interface AnatomyRenderMode {
        data class VectorEngine(val spec: AnatomySpec) : AnatomyRenderMode
        data class LegacyBoxes(val exercise: Exercise) : AnatomyRenderMode
    }

    sealed interface AnimationRenderMode {
        data class SkeletalEngine(
            val spec: ExerciseVisualSpec,
            val timeline: SkeletalTimeline
        ) : AnimationRenderMode

        data class LegacyStickFigure(
            val exercise: Exercise,
            val clip: AnimationClip
        ) : AnimationRenderMode
    }

    /**
     * Resolves the anatomical rendering strategy for an exercise.
     * Prefers the scalable vector engine; falls back to legacy rectangular boxes only if unmapped.
     */
    fun resolveAnatomy(exercise: Exercise): AnatomyRenderMode {
        return try {
            val spec = ExerciseVisualResolver.resolve(exercise)
            if (spec.anatomy.primaryMuscles.isNotEmpty() || spec.anatomy.secondaryMuscles.isNotEmpty()) {
                AnatomyRenderMode.VectorEngine(spec.anatomy)
            } else {
                Log.w(TAG, "Exercise '${exercise.name}' has empty AnatomySpec; falling back to legacy boxes.")
                AnatomyRenderMode.LegacyBoxes(exercise)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving AnatomySpec for '${exercise.name}': ${e.message}", e)
            AnatomyRenderMode.LegacyBoxes(exercise)
        }
    }

    /**
     * Resolves the kinematic animation strategy for an exercise.
     * Fallback order:
     * 1. Closest Movement Family (Skeletal Engine)
     * 2. Generic movement (Skeletal Engine)
     * 3. Legacy Stick Figure Renderer
     */
    fun resolveAnimation(exercise: Exercise): AnimationRenderMode {
        return try {
            val spec = ExerciseVisualResolver.resolve(exercise)
            val timeline = KinematicMovementFamilies.getTimelineForFamily(
                familyId = spec.movementFamily.familyId,
                parameters = spec.movementFamily.parameters
            )
            AnatomyValidationLogger.logResolution(exercise, spec)
            AnimationRenderMode.SkeletalEngine(spec, timeline)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving SkeletalTimeline for '${exercise.name}': ${e.message}", e)
            try {
                val clip = ExerciseAnimation.clip(exercise)
                AnimationRenderMode.LegacyStickFigure(exercise, clip)
            } catch (fallbackEx: Exception) {
                // Should never crash; construct a safe minimal legacy clip
                AnimationRenderMode.LegacyStickFigure(
                    exercise,
                    ExerciseAnimation.clip(exercise.copy(movementPattern = "Push • Horizontal Press"))
                )
            }
        }
    }
}

/**
 * Internal helper logger tracking resolved vs fallback exercises during runtime.
 */
internal object AnatomyValidationLogger {
    private val loggedExercises = mutableSetOf<Int>()

    fun logResolution(exercise: Exercise, spec: ExerciseVisualSpec) {
        if (loggedExercises.add(exercise.id)) {
            if (spec.movementFamily.familyId == "GENERIC_UNMAPPED") {
                Log.i("VisualEngineAdapter", "Unmapped pattern for exercise #${exercise.id} '${exercise.name}'; using generic skeletal timeline.")
            }
        }
    }
}
