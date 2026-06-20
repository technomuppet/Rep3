package com.replog.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.BodyweightLog
import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SetLog
import com.replog.data.model.WorkoutPrescription
import com.replog.data.model.WorkoutSession
import com.replog.data.repository.BodyweightRepository
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.WorkoutRepository
import com.replog.util.BackupJson
import com.replog.util.DemoDataGenerator
import com.replog.util.PreferencesManager
import com.replog.util.RestoreMergePlanner
import com.replog.util.WorkoutCsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val useKg: Boolean = true,
    val restSeconds: Int = 90,
    val customKgPlates: String = "25, 20, 15, 10, 5, 2.5, 1.25",
    val customLbPlates: String = "45, 35, 25, 10, 5, 2.5",
    val exportStatus: String? = null,
    val backupStatus: String? = null,
    val latestCsvPath: String? = null,
    val latestJsonPath: String? = null,
    val isBusy: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val bodyweightRepository: BodyweightRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val exportStatus = MutableStateFlow<String?>(null)
    private val backupStatus = MutableStateFlow<String?>(null)
    private val latestCsvPath = MutableStateFlow<String?>(null)
    private val latestJsonPath = MutableStateFlow<String?>(null)
    private val isBusy = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.useKg.map { it as Any? },
        prefs.restSeconds.map { it as Any? },
        prefs.customKgPlates.map { it as Any? },
        prefs.customLbPlates.map { it as Any? },
        exportStatus.map { it as Any? },
        backupStatus.map { it as Any? },
        latestCsvPath.map { it as Any? },
        latestJsonPath.map { it as Any? },
        isBusy.map { it as Any? }
    ) { values ->
        SettingsUiState(
            useKg = values[0] as Boolean,
            restSeconds = values[1] as Int,
            customKgPlates = values[2] as String,
            customLbPlates = values[3] as String,
            exportStatus = values[4] as String?,
            backupStatus = values[5] as String?,
            latestCsvPath = values[6] as String?,
            latestJsonPath = values[7] as String?,
            isBusy = values[8] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setUseKg(value: Boolean) = viewModelScope.launch { prefs.setUseKg(value) }
    fun setRestSeconds(value: Int) = viewModelScope.launch { prefs.setRestSeconds(value) }
    fun setCustomKgPlates(value: String) = viewModelScope.launch { prefs.setCustomKgPlates(value) }
    fun setCustomLbPlates(value: String) = viewModelScope.launch { prefs.setCustomLbPlates(value) }

    fun generateDemoData() = viewModelScope.launch {
        isBusy.value = true
        runCatching {
            val count = DemoDataGenerator.generate(exerciseRepository, workoutRepository, bodyweightRepository)
            backupStatus.value = if (count > 0) "Generated $count demo workouts for screenshots and QA." else "Demo data could not be generated until exercises are seeded."
        }.onFailure { error ->
            backupStatus.value = "Demo data generation failed: ${error.message ?: "Unknown error"}"
        }
        isBusy.value = false
    }

    fun exportCsv() = viewModelScope.launch {
        isBusy.value = true
        runCatching {
            val sessions = workoutRepository.getAllSessions().first()
            val prescriptions = sessions.associate { session ->
                session.session.id to workoutRepository.getPrescriptionsForSession(session.session.id)
            }
            val csv = WorkoutCsvExporter.toCsv(sessions, prescriptions)
            val file = File(context.filesDir, "replog_export_${System.currentTimeMillis()}.csv")
            file.writeText(csv)
            latestCsvPath.value = file.absolutePath
            exportStatus.value = "Exported ${sessions.size} workouts. Ready to share."
        }.onFailure { error ->
            exportStatus.value = "CSV export failed: ${error.message ?: "Unknown error"}"
        }
        isBusy.value = false
    }

    fun exportJsonBackup() = viewModelScope.launch {
        isBusy.value = true
        runCatching {
            val sessions = workoutRepository.getAllSessions().first()
            val bodyweights = bodyweightRepository.getAllBodyweights().first()
            val prescriptions = sessions.associate { session ->
                session.session.id to workoutRepository.getPrescriptionsForSession(session.session.id)
            }
            val json = BackupJson.encode(sessions, bodyweights, prescriptions)
            val file = File(context.filesDir, "replog_backup_${System.currentTimeMillis()}.json")
            file.writeText(json)
            File(context.filesDir, "replog_backup.json").writeText(json)
            latestJsonPath.value = file.absolutePath
            backupStatus.value = "JSON backup saved. Ready to share or restore later."
        }.onFailure { error ->
            backupStatus.value = "JSON backup failed: ${error.message ?: "Unknown error"}"
        }
        isBusy.value = false
    }

    fun restoreLatestJsonBackup() = viewModelScope.launch {
        val file = File(context.filesDir, "replog_backup.json")
        if (!file.exists()) {
            backupStatus.value = "No local JSON backup found yet. Export one first or import a file."
            return@launch
        }
        restoreJson(file.readText(), sourceLabel = "local backup")
    }

    fun restoreJsonText(json: String) = viewModelScope.launch {
        restoreJson(json, sourceLabel = "selected file")
    }

    private suspend fun restoreJson(json: String, sourceLabel: String) {
        isBusy.value = true
        runCatching {
            val backup = BackupJson.decode(json)
            val existingSessions = workoutRepository.getAllSessions().first()
            val existingSessionKeys = existingSessions.map { session ->
                RestoreMergePlanner.sessionFingerprint(
                    startTime = session.session.startTime,
                    exerciseCount = session.exercises.size,
                    setCount = session.exercises.sumOf { it.sets.size }
                )
            }.toSet()
            val existingBodyweightKeys = bodyweightRepository.getAllBodyweights().first().map {
                RestoreMergePlanner.bodyweightFingerprint(it.timestamp, it.weight)
            }.toSet()

            val bodyweightPlan = RestoreMergePlanner.filterDuplicates(
                incoming = backup.bodyweights,
                existingFingerprints = existingBodyweightKeys,
                fingerprint = { RestoreMergePlanner.bodyweightFingerprint(it.timestamp, it.weight) }
            )
            val sessionPlan = RestoreMergePlanner.filterDuplicates(
                incoming = backup.sessions,
                existingFingerprints = existingSessionKeys,
                fingerprint = {
                    RestoreMergePlanner.sessionFingerprint(
                        startTime = it.startTime,
                        exerciseCount = it.exercises.size,
                        setCount = it.exercises.sumOf { exercise -> exercise.sets.size }
                    )
                }
            )

            bodyweightPlan.itemsToRestore.forEach { bodyweight ->
                bodyweightRepository.insertBodyweight(
                    BodyweightLog(
                        weight = bodyweight.weight,
                        timestamp = bodyweight.timestamp,
                        note = bodyweight.note
                    )
                )
            }
            sessionPlan.itemsToRestore.forEach { backupSession ->

                val restoredExerciseIdsByName = mutableMapOf<String, Int>()
                val sessionId = workoutRepository.insertSession(
                    WorkoutSession(
                        templateName = backupSession.templateName,
                        startTime = backupSession.startTime,
                        endTime = backupSession.endTime,
                        notes = backupSession.notes
                    )
                ).toInt()

                backupSession.exercises.sortedBy { it.orderIndex }.forEach { backupExercise ->
                    val exercise = exerciseRepository.getExerciseByName(backupExercise.exerciseName)
                        ?: exerciseRepository.insertExercise(
                            Exercise(
                                name = backupExercise.exerciseName,
                                category = backupExercise.category.ifBlank { "Imported" },
                                equipment = backupExercise.equipment.ifBlank { "Other" },
                                muscles = backupExercise.muscles,
                                primaryMuscles = backupExercise.primaryMuscles,
                                secondaryMuscles = backupExercise.secondaryMuscles,
                                movementPattern = backupExercise.movementPattern,
                                difficulty = backupExercise.difficulty,
                                mediaAsset = backupExercise.mediaAsset,
                                isCustom = true
                            )
                        ).let { id -> exerciseRepository.getExerciseById(id.toInt()) }
                        ?: return@forEach

                    restoredExerciseIdsByName[backupExercise.exerciseName] = exercise.id

                    val sessionExerciseId = workoutRepository.insertSessionExercise(
                        SessionExercise(
                            sessionId = sessionId,
                            exerciseId = exercise.id,
                            orderIndex = backupExercise.orderIndex,
                            supersetGroup = backupExercise.supersetGroup
                        )
                    ).toInt()

                    backupExercise.sets.forEach { backupSet ->
                        workoutRepository.insertSet(
                            SetLog(
                                sessionExerciseId = sessionExerciseId,
                                setNumber = backupSet.setNumber,
                                weight = backupSet.weight,
                                reps = backupSet.reps,
                                isBodyweight = backupSet.isBodyweight,
                                isPR = backupSet.isPR,
                                timestamp = backupSet.timestamp,
                                setType = backupSet.setType,
                                rpe = backupSet.rpe,
                                tempo = backupSet.tempo
                            )
                        )
                    }
                }

                val prescriptions = backupSession.prescriptions.mapNotNull { prescription ->
                    val exerciseId = restoredExerciseIdsByName[prescription.exerciseName] ?: return@mapNotNull null
                    WorkoutPrescription(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        source = prescription.source,
                        targetSets = prescription.targetSets,
                        targetReps = prescription.targetReps,
                        targetWeight = prescription.targetWeight,
                        adjustment = prescription.adjustment,
                        reason = prescription.reason,
                        createdAt = prescription.createdAt
                    )
                }
                if (prescriptions.isNotEmpty()) workoutRepository.insertPrescriptions(prescriptions)
            }
            backupStatus.value = "Restored ${sessionPlan.itemsToRestore.size} workouts and ${bodyweightPlan.itemsToRestore.size} bodyweight logs from $sourceLabel. Skipped ${sessionPlan.skippedDuplicateCount} duplicate workouts and ${bodyweightPlan.skippedDuplicateCount} duplicate bodyweight logs."
        }.onFailure { error ->
            backupStatus.value = "Restore failed: ${error.message ?: "Invalid backup"}"
        }
        isBusy.value = false
    }
}
