package com.replog.ui.exercise.adapter

import com.replog.data.model.Exercise
import com.replog.domain.library.AnimationClip
import com.replog.domain.visual.animation.KinematicMovementFamilies
import com.replog.domain.visual.animation.SkeletalTimeline
import com.replog.domain.visual.resolver.ExerciseVisualResolver
import com.replog.domain.visual.spec.AnatomySpec
import com.replog.domain.visual.spec.ExerciseVisualSpec

/**
 * UI compatibility facade — RC23 Production
 * Safe for JVM unit tests (uses safe println logging instead of android.util.Log).
 */
object VisualEngineAdapter {

    private const val TAG = "VisualEngineAdapter"

    sealed interface AnatomyRenderMode {
        data class VectorEngine(val spec: AnatomySpec) : AnatomyRenderMode
    }

    sealed interface AnimationRenderMode {
        data class SkeletalEngine(
            val spec: ExerciseVisualSpec,
            val timeline: SkeletalTimeline
        ) : AnimationRenderMode
        // Legacy kept for binary compatibility but no longer used as primary path
        data class LegacyStickFigure(
            val exercise: Exercise,
            val clip: AnimationClip
        ) : AnimationRenderMode
        data class LegacyBoxes(val exercise: Exercise) : AnimationRenderMode
    }

    fun resolveAnatomy(exercise: Exercise): AnatomyRenderMode {
        return try {
            val spec = ExerciseVisualResolver.resolve(exercise)
            if (spec.anatomy.primaryMuscles.isNotEmpty() || spec.anatomy.secondaryMuscles.isNotEmpty()) {
                AnatomyRenderMode.VectorEngine(spec.anatomy)
            } else {
                println("WARN: $TAG: Exercise '${exercise.name}' has empty AnatomySpec; using vector fallback.")
                AnatomyRenderMode.VectorEngine(spec.anatomy)
            }
        } catch (e: Exception) {
            println("ERROR: $TAG: Error resolving AnatomySpec for '${exercise.name}': ${e.message}")
            e.printStackTrace()
            val spec = ExerciseVisualResolver.resolve(exercise.copy(primaryMuscles = "Chest", secondaryMuscles = "Triceps"))
            AnatomyRenderMode.VectorEngine(spec.anatomy)
        }
    }

    fun resolveAnimation(exercise: Exercise): AnimationRenderMode {
        return try {
            val spec = ExerciseVisualResolver.resolve(exercise)
            val timeline = try {
                com.replog.domain.visual.biomechanics.CommercialMotionLibrary.getTimelineForExerciseName(exercise.name)
            } catch (ex: Exception) {
                println("WARN: $TAG: Exercise-specific template failed for '${exercise.name}', falling back to family: ${ex.message}")
                KinematicMovementFamilies.getTimelineForFamily(
                    familyId = spec.movementFamily.familyId,
                    parameters = spec.movementFamily.parameters
                )
            }
            AnatomyValidationLogger.logResolution(exercise, spec)
            AnimationRenderMode.SkeletalEngine(spec, timeline)
        } catch (e: Exception) {
            println("ERROR: $TAG: Error resolving SkeletalTimeline for '${exercise.name}': ${e.message}")
            e.printStackTrace()
            // RC20.4: No longer fallback to legacy stick figure, fallback to commercial generic bench press
            try {
                val spec = ExerciseVisualResolver.resolve(exercise)
                val timeline = com.replog.domain.visual.biomechanics.CommercialMotionLibrary.benchPressTimeline()
                AnimationRenderMode.SkeletalEngine(spec, timeline)
            } catch (fallbackEx: Exception) {
                // Last resort generic
                val spec = ExerciseVisualResolver.resolve(exercise.copy(movementPattern = "Push • Horizontal Press"))
                val timeline = com.replog.domain.visual.biomechanics.CommercialMotionLibrary.benchPressTimeline()
                AnimationRenderMode.SkeletalEngine(spec, timeline)
            }
        }
    }
}

internal object AnatomyValidationLogger {
    private val loggedExercises = mutableSetOf<Int>()
    fun logResolution(exercise: Exercise, spec: ExerciseVisualSpec) {
        if (loggedExercises.add(exercise.id)) {
            if (spec.movementFamily.familyId == "GENERIC_UNMAPPED") {
                println("INFO: VisualEngineAdapter: Unmapped pattern for exercise #${exercise.id} '${exercise.name}'; using generic skeletal timeline.")
            }
        }
    }
}
