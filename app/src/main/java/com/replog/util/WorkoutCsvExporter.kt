package com.replog.util

import com.replog.data.model.SessionWithExercises
import com.replog.data.model.WorkoutPrescription

object WorkoutCsvExporter {
    fun toCsv(
        sessions: List<SessionWithExercises>,
        prescriptionsBySessionId: Map<Int, List<WorkoutPrescription>> = emptyMap()
    ): String {
        val builder = StringBuilder()
        builder.appendLine("session_id,workout_name,start_time,end_time,exercise,category,equipment,movement_pattern,primary_muscles,secondary_muscles,superset_group,set_number,set_type,weight,reps,rpe,tempo,is_pr,volume,prescription_source,target_sets,target_reps,target_weight,target_hit")
        sessions.forEach { session ->
            val prescriptions = prescriptionsBySessionId[session.session.id].orEmpty().associateBy { it.exerciseId }
            session.exercises.forEach { entry ->
                val prescription = prescriptions[entry.exercise.id]
                val hardSets = entry.sets.filter { it.setType != "Warmup" }
                val targetHit = prescription?.let { target ->
                    val targetWeight = target.targetWeight ?: 0.0
                    hardSets.size >= target.targetSets && hardSets.any { it.weight >= targetWeight && it.reps >= target.targetReps }
                }
                entry.sets.forEach { set ->
                    builder.appendLine(
                        listOf(
                            session.session.id.toString(),
                            csv(session.session.templateName ?: "Workout"),
                            session.session.startTime.toString(),
                            session.session.endTime?.toString().orEmpty(),
                            csv(entry.exercise.name),
                            csv(entry.exercise.category),
                            csv(entry.exercise.equipment),
                            csv(entry.exercise.movementPattern),
                            csv(entry.exercise.primaryMuscles),
                            csv(entry.exercise.secondaryMuscles),
                            csv(entry.sessionExercise.supersetGroup.orEmpty()),
                            set.setNumber.toString(),
                            csv(set.setType),
                            set.weight.toString(),
                            set.reps.toString(),
                            set.rpe?.toString().orEmpty(),
                            csv(set.tempo.orEmpty()),
                            set.isPR.toString(),
                            (set.weight * set.reps).toString(),
                            csv(prescription?.source.orEmpty()),
                            prescription?.targetSets?.toString().orEmpty(),
                            prescription?.targetReps?.toString().orEmpty(),
                            prescription?.targetWeight?.toString().orEmpty(),
                            targetHit?.toString().orEmpty()
                        ).joinToString(",")
                    )
                }
            }
        }
        return builder.toString()
    }

    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}
