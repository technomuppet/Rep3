package com.replog.util

import com.google.gson.GsonBuilder
import com.replog.data.model.BodyweightLog
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.WorkoutPrescription

data class RepLogBackup(
    val schemaVersion: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val sessions: List<BackupSession> = emptyList(),
    val bodyweights: List<BackupBodyweight> = emptyList()
)

data class BackupSession(
    val templateName: String?,
    val startTime: Long,
    val endTime: Long?,
    val notes: String?,
    val exercises: List<BackupSessionExercise>,
    val prescriptions: List<BackupPrescription> = emptyList()
)

data class BackupSessionExercise(
    val exerciseName: String,
    val category: String,
    val equipment: String,
    val muscles: String,
    val primaryMuscles: String = "",
    val secondaryMuscles: String = "",
    val movementPattern: String = "",
    val difficulty: String = "Intermediate",
    val mediaAsset: String = "",
    val orderIndex: Int,
    val supersetGroup: String? = null,
    val sets: List<BackupSet>
)

data class BackupSet(
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isBodyweight: Boolean,
    val isPR: Boolean,
    val timestamp: Long,
    val setType: String = "Working",
    val rpe: Double? = null,
    val tempo: String? = null
)

data class BackupPrescription(
    val exerciseName: String,
    val source: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double? = null,
    val adjustment: String = "Maintain",
    val reason: String = "Programmed target",
    val createdAt: Long = System.currentTimeMillis()
)

data class BackupBodyweight(
    val weight: Double,
    val timestamp: Long,
    val note: String?
)

object BackupJson {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun encode(
        sessions: List<SessionWithExercises>,
        bodyweights: List<BodyweightLog>,
        prescriptionsBySessionId: Map<Int, List<WorkoutPrescription>> = emptyMap()
    ): String = gson.toJson(
        RepLogBackup(
            sessions = sessions.map { session ->
                val exerciseNamesById = session.exercises.associate { it.exercise.id to it.exercise.name }
                BackupSession(
                    templateName = session.session.templateName,
                    startTime = session.session.startTime,
                    endTime = session.session.endTime,
                    notes = session.session.notes,
                    exercises = session.exercises.sortedBy { it.sessionExercise.orderIndex }.map { entry ->
                        BackupSessionExercise(
                            exerciseName = entry.exercise.name,
                            category = entry.exercise.category,
                            equipment = entry.exercise.equipment,
                            muscles = entry.exercise.muscles,
                            primaryMuscles = entry.exercise.primaryMuscles,
                            secondaryMuscles = entry.exercise.secondaryMuscles,
                            movementPattern = entry.exercise.movementPattern,
                            difficulty = entry.exercise.difficulty,
                            mediaAsset = entry.exercise.mediaAsset,
                            orderIndex = entry.sessionExercise.orderIndex,
                            supersetGroup = entry.sessionExercise.supersetGroup,
                            sets = entry.sets.sortedBy { it.setNumber }.map { set ->
                                BackupSet(
                                    setNumber = set.setNumber,
                                    weight = set.weight,
                                    reps = set.reps,
                                    isBodyweight = set.isBodyweight,
                                    isPR = set.isPR,
                                    timestamp = set.timestamp,
                                    setType = set.setType,
                                    rpe = set.rpe,
                                    tempo = set.tempo
                                )
                            }
                        )
                    },
                    prescriptions = prescriptionsBySessionId[session.session.id].orEmpty().mapNotNull { prescription ->
                        val exerciseName = exerciseNamesById[prescription.exerciseId] ?: return@mapNotNull null
                        BackupPrescription(
                            exerciseName = exerciseName,
                            source = prescription.source,
                            targetSets = prescription.targetSets,
                            targetReps = prescription.targetReps,
                            targetWeight = prescription.targetWeight,
                            adjustment = prescription.adjustment,
                            reason = prescription.reason,
                            createdAt = prescription.createdAt
                        )
                    }
                )
            },
            bodyweights = bodyweights.map { BackupBodyweight(it.weight, it.timestamp, it.note) }
        )
    )

    fun decode(json: String): RepLogBackup = gson.fromJson(json, RepLogBackup::class.java)
}
