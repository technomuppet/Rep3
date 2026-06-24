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
import com.replog.util.FileExporter
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
import com.replog.util.timer.RestPresets

data class SettingsUiState(
    val useKg: Boolean = true,
    val customKgPlates: String = "25, 20, 15, 10, 5, 2.5, 1.25",
    val restPresets: RestPresets = RestPresets(),
    val customLbPlates: String = "45, 35, 25, 10, 5, 2.5",
    val exportStatus: String? = null,
    val backupStatus: String? = null,
    val latestCsvShareUri: String? = null,
    val latestJsonShareUri: String? = null,
    val latestCsvFileName: String? = null,
    val latestCsvLocation: String? = null,
    val latestJsonFileName: String? = null,
    val latestJsonLocation: String? = null,
    val exportFolderLabel: String = "Downloads/RepLog",
    val isBusy: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val bodyweightRepository: BodyweightRepository,
    private val dataResetManager: com.replog.util.DataResetManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    // Holds the result of the most recent CSV / JSON export so the UI can show
    // filename + location + Open/Share without juggling many separate flows.
    private data class ExportArtifacts(
        val csvShareUri: String? = null,
        val csvFileName: String? = null,
        val csvLocation: String? = null,
        val jsonShareUri: String? = null,
        val jsonFileName: String? = null,
        val jsonLocation: String? = null
    )

    private val exportStatus = MutableStateFlow<String?>(null)
    private val backupStatus = MutableStateFlow<String?>(null)
    private val artifacts = MutableStateFlow(ExportArtifacts())
    private val isBusy = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        listOf(
            prefs.useKg.map { it as Any? },
            prefs.restPresets.map { it as Any? },
            prefs.customKgPlates.map { it as Any? },
            prefs.customLbPlates.map { it as Any? },
            exportStatus.map { it as Any? },
            backupStatus.map { it as Any? },
            artifacts.map { it as Any? },
            isBusy.map { it as Any? },
            prefs.exportFolderLabel.map { it as Any? }
        )
    ) { values ->
        val a = values[6] as ExportArtifacts
        SettingsUiState(
            useKg = values[0] as Boolean,
            restPresets = values[1] as RestPresets,
            customKgPlates = values[2] as String,
            customLbPlates = values[3] as String,
            exportStatus = values[4] as String?,
            backupStatus = values[5] as String?,
            latestCsvShareUri = a.csvShareUri,
            latestJsonShareUri = a.jsonShareUri,
            latestCsvFileName = a.csvFileName,
            latestCsvLocation = a.csvLocation,
            latestJsonFileName = a.jsonFileName,
            latestJsonLocation = a.jsonLocation,
            isBusy = values[7] as Boolean,
            exportFolderLabel = values[8] as String
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setUseKg(value: Boolean) = viewModelScope.launch { prefs.setUseKg(value) }
    fun setRestPresets(value: RestPresets) = viewModelScope.launch { prefs.setRestPresets(value) }
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

    private fun dateStamp(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    /** Split "Downloads/RepLog/file.csv" into (location, fileName). */
    private fun splitPath(displayPath: String): Pair<String, String> {
        val idx = displayPath.lastIndexOf('/')
        return if (idx >= 0) displayPath.substring(0, idx) to displayPath.substring(idx + 1)
        else "" to displayPath
    }

    /** Persist the chosen SAF export folder (tree URI + readable label). */
    fun setExportFolder(treeUri: String, label: String) = viewModelScope.launch {
        prefs.setExportFolder(treeUri, label)
        exportStatus.value = "Export folder set to $label."
    }

    /** Reset the export folder back to the default Downloads/RepLog location. */
    fun resetExportFolder() = viewModelScope.launch {
        prefs.setExportFolder(null, "Downloads/RepLog")
        exportStatus.value = "Export folder reset to Downloads/RepLog."
    }

    fun exportCsv() = viewModelScope.launch {
        isBusy.value = true
        runCatching {
            val sessions = workoutRepository.getAllSessions().first()
            val prescriptions = sessions.associate { session ->
                session.session.id to workoutRepository.getPrescriptionsForSession(session.session.id)
            }
            val csv = WorkoutCsvExporter.toCsv(sessions, prescriptions)
            val result = FileExporter.save(
                context = context,
                fileName = "WorkoutHistory_${dateStamp()}.csv",
                mimeType = "text/csv",
                content = csv,
                treeUriString = prefs.exportTreeUri.first()
            )
            val (location, fileName) = splitPath(result.displayPath)
            artifacts.value = artifacts.value.copy(
                csvShareUri = result.shareUri?.toString(),
                csvFileName = fileName,
                csvLocation = location
            )
            exportStatus.value = "Exported ${sessions.size} workouts."
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
            // Keep an app-private copy so "Restore Local" still works offline.
            File(context.filesDir, "replog_backup.json").writeText(json)
            val result = FileExporter.save(
                context = context,
                fileName = "WorkoutBackup_${dateStamp()}.json",
                mimeType = "application/json",
                content = json,
                treeUriString = prefs.exportTreeUri.first()
            )
            val (location, fileName) = splitPath(result.displayPath)
            artifacts.value = artifacts.value.copy(
                jsonShareUri = result.shareUri?.toString(),
                jsonFileName = fileName,
                jsonLocation = location
            )
            backupStatus.value = "Backup complete."
        }.onFailure { error ->
            backupStatus.value = "JSON backup failed: ${error.message ?: "Unknown error"}"
        }
        isBusy.value = false
    }

    /**
     * Delete ALL workout history: sessions, sets, PRs, prescriptions, Training
     * DNA, recovery/rest history, plateau and recommendation history. Templates,
     * the exercise library, goals, bodyweight and all settings are preserved.
     * DNA and recovery recompute from the (now empty) data on next load.
     */
    fun deleteAllHistory() = viewModelScope.launch {
        isBusy.value = true
        runCatching {
            dataResetManager.deleteAllWorkoutHistory()
        }.onSuccess { count ->
            backupStatus.value = "Deleted all workout history ($count workouts). Templates, exercises and settings were kept."
        }.onFailure { error ->
            backupStatus.value = "Delete failed: ${error.message ?: "Unknown error"}"
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
