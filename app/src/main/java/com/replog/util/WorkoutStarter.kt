package com.replog.util

import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetType
import com.replog.data.model.TemplateWithExercises
import com.replog.data.model.WorkoutPrescription
import com.replog.data.model.WorkoutSession
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.WorkoutRepository
import com.replog.domain.library.QuickWorkout
import com.replog.domain.recommendation.WorkoutPlan
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for turning a curated Quick Workout into a live
 * session, so the Quick Workouts screen and the workout engine share one code
 * path (no duplicated session-creation logic). Sets the active session id in
 * preferences; the active-workout screen then resumes it on next load.
 */
@Singleton
class WorkoutStarter @Inject constructor(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val prefs: PreferencesManager
) {
    /** Parse "5", "8-12", "AMRAP", "30s" into a representative integer rep target. */
    fun targetReps(reps: String): Int {
        val nums = Regex("\\d+").findAll(reps).map { it.value.toInt() }.toList()
        return when {
            nums.isEmpty() -> 8
            nums.size >= 2 -> (nums[0] + nums[1]) / 2
            else -> nums[0]
        }.coerceAtLeast(1)
    }

    /**
     * Create a live session from a Quick Workout and mark it active. Returns the
     * new session id, or null if none of its exercises resolved against the
     * library.
     */
    suspend fun startQuickWorkout(workout: QuickWorkout): Int? {
        val id = workouts.insertSession(
            WorkoutSession(templateName = workout.name, startTime = System.currentTimeMillis())
        ).toInt()
        val prescriptions = mutableListOf<WorkoutPrescription>()
        var order = 0
        workout.exercises.forEach { spec ->
            val exercise = exercises.getExerciseByName(spec.exerciseName) ?: return@forEach
            workouts.insertSessionExercise(SessionExercise(sessionId = id, exerciseId = exercise.id, orderIndex = order, notes = ""))
            prescriptions += WorkoutPrescription(
                sessionId = id, exerciseId = exercise.id, source = "Quick Workout",
                targetSets = spec.sets.coerceAtLeast(1), targetReps = targetReps(spec.reps), targetWeight = null,
                adjustment = "Programmed", reason = workout.name
            )
            order++
        }
        if (order == 0) {
            workouts.deleteSessionById(id)
            return null
        }
        if (prescriptions.isNotEmpty()) workouts.insertPrescriptions(prescriptions)
        prefs.setActiveSessionId(id)
        return id
    }

    /** Create a live session from a saved template and mark it active. */
    suspend fun startTemplate(template: TemplateWithExercises): Int {
        val id = workouts.insertSession(
            WorkoutSession(templateName = template.template.name, startTime = System.currentTimeMillis())
        ).toInt()
        template.exercises.sortedBy { it.templateExercise.orderIndex }.forEachIndexed { index, te ->
            workouts.insertSessionExercise(SessionExercise(sessionId = id, exerciseId = te.exercise.id, orderIndex = index, notes = ""))
        }
        workouts.insertPrescriptions(
            template.exercises.map { te ->
                WorkoutPrescription(
                    sessionId = id, exerciseId = te.exercise.id, source = "Template",
                    targetSets = te.templateExercise.defaultSets, targetReps = te.templateExercise.targetReps,
                    targetWeight = te.templateExercise.targetWeight, adjustment = "Programmed", reason = "Template target"
                )
            }
        )
        prefs.setActiveSessionId(id)
        return id
    }

    /**
     * Recreate a previous session (P6 repeat): same exercises in order, notes
     * carried over, prior top working set pre-loaded as the target. Marks active.
     */
    suspend fun repeatSession(session: SessionWithExercises): Int {
        val id = workouts.insertSession(
            WorkoutSession(templateName = session.session.templateName ?: "Repeat workout", startTime = System.currentTimeMillis())
        ).toInt()
        val prescriptions = mutableListOf<WorkoutPrescription>()
        session.exercises.sortedBy { it.sessionExercise.orderIndex }.forEachIndexed { index, entry ->
            workouts.insertSessionExercise(
                SessionExercise(sessionId = id, exerciseId = entry.exercise.id, orderIndex = index, notes = entry.sessionExercise.notes)
            )
            val working = entry.sets.filter { it.setType == SetType.WORKING }.ifEmpty { entry.sets }
            working.maxByOrNull { it.weight }?.let { top ->
                prescriptions += WorkoutPrescription(
                    sessionId = id, exerciseId = entry.exercise.id, source = "Repeat",
                    targetSets = working.size.coerceAtLeast(1), targetReps = top.reps, targetWeight = top.weight,
                    adjustment = "Repeat", reason = "Same as last time"
                )
            }
        }
        if (prescriptions.isNotEmpty()) workouts.insertPrescriptions(prescriptions)
        prefs.setActiveSessionId(id)
        return id
    }

    /**
     * Phase 3 Gap 1 — create a live session from a recommendation engine
     * [WorkoutPlan] and mark it active. Same pattern as [startQuickWorkout]
     * and [startTemplate]: insert session -> insert exercises -> insert
     * prescriptions -> set active id.
     */
    suspend fun startFromRecommendation(plan: WorkoutPlan, templateName: String): Int {
        val id = workouts.insertSession(
            WorkoutSession(templateName = templateName, startTime = System.currentTimeMillis())
        ).toInt()
        val prescriptions = mutableListOf<WorkoutPrescription>()
        plan.exercises.forEachIndexed { index, planned ->
            workouts.insertSessionExercise(
                SessionExercise(
                    sessionId = id,
                    exerciseId = planned.exerciseId,
                    orderIndex = index,
                    notes = planned.reason
                )
            )
            prescriptions += WorkoutPrescription(
                sessionId = id,
                exerciseId = planned.exerciseId,
                source = "Recommendation",
                targetSets = planned.targetSets.coerceAtLeast(1),
                targetReps = planned.targetReps,
                targetWeight = planned.targetWeight,
                adjustment = planned.progression.name,
                reason = planned.reason
            )
        }
        if (prescriptions.isNotEmpty()) workouts.insertPrescriptions(prescriptions)
        prefs.setActiveSessionId(id)
        return id
    }
}
