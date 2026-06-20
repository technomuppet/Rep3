package com.replog.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.TemplateExercise
import com.replog.data.model.TemplateWithExercises
import com.replog.data.model.WorkoutPrescription
import com.replog.data.model.WorkoutSession
import com.replog.data.model.WorkoutTemplate
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.WorkoutRepository
import com.replog.util.ActiveWorkoutRecovery
import com.replog.util.ActiveWorkoutRecoveryDecision
import com.replog.util.AdaptiveProgramEngine
import com.replog.util.AdaptiveWorkoutPlan
import com.replog.util.PreferencesManager
import com.replog.util.ProgressionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NO_REST_TIMER = 0L

data class TemplateExerciseDraft(
    val exercise: Exercise,
    val defaultSets: Int = 3,
    val targetReps: Int = 8,
    val targetWeight: Double? = null
)

data class WorkoutTargetUi(
    val exerciseId: Int,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double?,
    val adjustment: String,
    val reason: String,
    val source: String
)

data class WorkoutSummary(
    val name: String,
    val durationMillis: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val volume: Double,
    val prCount: Int,
    val targetHitCount: Int = 0,
    val targetMissCount: Int = 0,
    val adaptiveNotes: List<String> = emptyList(),
    val progressionNotes: List<String> = emptyList()
)

data class ActiveWorkoutUiState(
    val activeSessionId: Int? = null,
    val startTime: Long? = null,
    val session: SessionWithExercises? = null,
    val allExercises: List<Exercise> = emptyList(),
    val templates: List<TemplateWithExercises> = emptyList(),
    val lastSetsByExerciseId: Map<Int, List<SetLog>> = emptyMap(),
    val restSeconds: Int = 90,
    val restRemainingSeconds: Int = 0,
    val useKg: Boolean = true,
    val isSaving: Boolean = false,
    val restoredWorkout: Boolean = false,
    val summary: WorkoutSummary? = null,
    val adaptivePlan: AdaptiveWorkoutPlan? = null,
    val targetsByExerciseId: Map<Int, WorkoutTargetUi> = emptyMap()
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workouts: WorkoutRepository,
    exercises: ExerciseRepository,
    private val prefs: PreferencesManager
) : ViewModel() {
    private val activeId = MutableStateFlow<Int?>(null)
    private val tick = MutableStateFlow(0)
    private val saving = MutableStateFlow(false)
    private val restored = MutableStateFlow(false)
    private val summary = MutableStateFlow<WorkoutSummary?>(null)
    private val restEndMillis = MutableStateFlow(NO_REST_TIMER)

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        activeId.map { it as Any? },
        tick.map { it as Any? },
        saving.map { it as Any? },
        exercises.getAllExercises().map { it as Any? },
        workouts.getAllTemplates().map { it as Any? },
        prefs.restSeconds.map { it as Any? },
        prefs.useKg.map { it as Any? },
        restored.map { it as Any? },
        summary.map { it as Any? },
        restEndMillis.map { it as Any? },
        workouts.getAllSessions().map { it as Any? }
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val id = values[0] as Int?
        val isSaving = values[2] as Boolean
        val allExercises = values[3] as List<Exercise>
        val templates = values[4] as List<TemplateWithExercises>
        val restSeconds = values[5] as Int
        val useKg = values[6] as Boolean
        val restoredWorkout = values[7] as Boolean
        val currentSummary = values[8] as WorkoutSummary?
        val restEnd = values[9] as Long
        @Suppress("UNCHECKED_CAST")
        val allSessions = values[10] as List<SessionWithExercises>

        val session = id?.let { workouts.getSessionById(it) }
        val prescriptions = id?.let { workouts.getPrescriptionsForSession(it) }.orEmpty()
        val lastSets = session?.exercises.orEmpty().associate { entry ->
            entry.exercise.id to workouts.getRecentSetsForExercise(entry.exercise.id, limit = 3).reversed()
        }
        val remaining = ((restEnd - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        val adaptivePlan = AdaptiveProgramEngine.buildPlan(allSessions)

        ActiveWorkoutUiState(
            activeSessionId = id,
            startTime = session?.session?.startTime,
            session = session,
            allExercises = allExercises,
            templates = templates,
            lastSetsByExerciseId = lastSets,
            restSeconds = restSeconds,
            restRemainingSeconds = remaining,
            useKg = useKg,
            isSaving = isSaving,
            restoredWorkout = restoredWorkout,
            summary = currentSummary,
            adaptivePlan = adaptivePlan,
            targetsByExerciseId = prescriptions.associate { prescription ->
                prescription.exerciseId to WorkoutTargetUi(
                    exerciseId = prescription.exerciseId,
                    exerciseName = session?.exercises?.firstOrNull { it.exercise.id == prescription.exerciseId }?.exercise?.name.orEmpty(),
                    targetSets = prescription.targetSets,
                    targetReps = prescription.targetReps,
                    targetWeight = prescription.targetWeight,
                    adjustment = prescription.adjustment,
                    reason = prescription.reason,
                    source = prescription.source
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    init {
        viewModelScope.launch {
            val storedId = prefs.activeSessionId.first()
            if (storedId != null) {
                val session = workouts.getSessionEntity(storedId)
                when (ActiveWorkoutRecovery.decide(storedId, sessionExists = session != null, sessionEnded = session?.endTime != null)) {
                    ActiveWorkoutRecoveryDecision.RESUME -> {
                        activeId.value = storedId
                        restored.value = true
                        refresh()
                    }
                    ActiveWorkoutRecoveryDecision.CLEAR_STALE -> prefs.setActiveSessionId(null)
                    ActiveWorkoutRecoveryDecision.NONE -> Unit
                }
            }
        }
    }

    fun refresh() { tick.value++ }
    fun dismissRestoredBanner() { restored.value = false }
    fun dismissSummary() { summary.value = null }
    fun skipRestTimer() { restEndMillis.value = NO_REST_TIMER; refresh() }

    fun startWorkout(name: String? = null) = viewModelScope.launch {
        summary.value = null
        val id = workouts.insertSession(
            WorkoutSession(
                templateName = name?.ifBlank { null },
                startTime = System.currentTimeMillis()
            )
        ).toInt()
        activeId.value = id
        prefs.setActiveSessionId(id)
        refresh()
    }

    fun createTemplate(name: String, selectedExercises: List<TemplateExerciseDraft>) = viewModelScope.launch {
        val cleanName = name.trim()
        if (cleanName.isBlank() || selectedExercises.isEmpty()) return@launch
        val templateId = workouts.insertTemplate(WorkoutTemplate(name = cleanName, isBuiltIn = false)).toInt()
        selectedExercises.forEachIndexed { index, draft ->
            workouts.insertTemplateExercise(
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = draft.exercise.id,
                    defaultSets = draft.defaultSets.coerceAtLeast(1),
                    orderIndex = index,
                    targetReps = draft.targetReps.coerceAtLeast(1),
                    targetWeight = draft.targetWeight
                )
            )
        }
        refresh()
    }

    fun updateTemplate(template: TemplateWithExercises, name: String, selectedExercises: List<TemplateExerciseDraft>) = viewModelScope.launch {
        val cleanName = name.trim()
        if (template.template.isBuiltIn || cleanName.isBlank() || selectedExercises.isEmpty()) return@launch
        workouts.updateTemplate(template.template.copy(name = cleanName))
        workouts.deleteTemplateExercises(template.template.id)
        selectedExercises.forEachIndexed { index, draft ->
            workouts.insertTemplateExercise(
                TemplateExercise(
                    templateId = template.template.id,
                    exerciseId = draft.exercise.id,
                    defaultSets = draft.defaultSets.coerceAtLeast(1),
                    orderIndex = index,
                    targetReps = draft.targetReps.coerceAtLeast(1),
                    targetWeight = draft.targetWeight
                )
            )
        }
        refresh()
    }

    fun deleteTemplate(template: TemplateWithExercises) = viewModelScope.launch {
        if (!template.template.isBuiltIn) {
            workouts.deleteTemplate(template.template)
            refresh()
        }
    }

    fun startAdaptiveWorkout(plan: AdaptiveWorkoutPlan) = viewModelScope.launch {
        val source = plan.sourceSessionId?.let { workouts.getSessionById(it) } ?: return@launch
        summary.value = null
        val id = workouts.insertSession(
            WorkoutSession(
                templateName = plan.title,
                startTime = System.currentTimeMillis()
            )
        ).toInt()
        source.exercises.sortedBy { it.sessionExercise.orderIndex }.forEachIndexed { index, entry ->
            workouts.insertSessionExercise(
                SessionExercise(
                    sessionId = id,
                    exerciseId = entry.exercise.id,
                    orderIndex = index,
                    supersetGroup = entry.sessionExercise.supersetGroup
                )
            )
        }
        workouts.insertPrescriptions(
            plan.targets.map { target ->
                WorkoutPrescription(
                    sessionId = id,
                    exerciseId = target.exerciseId,
                    source = "Adaptive",
                    targetSets = target.suggestedSets,
                    targetReps = target.suggestedReps,
                    targetWeight = target.suggestedWeight,
                    adjustment = target.adjustment,
                    reason = target.reason
                )
            }
        )
        activeId.value = id
        prefs.setActiveSessionId(id)
        refresh()
    }

    fun startWorkoutFromTemplate(template: TemplateWithExercises) = viewModelScope.launch {
        summary.value = null
        val id = workouts.insertSession(
            WorkoutSession(
                templateName = template.template.name,
                startTime = System.currentTimeMillis()
            )
        ).toInt()
        val sortedTemplateExercises = template.exercises.sortedBy { it.templateExercise.orderIndex }
        sortedTemplateExercises.forEachIndexed { index, templateExercise ->
            workouts.insertSessionExercise(
                SessionExercise(
                    sessionId = id,
                    exerciseId = templateExercise.exercise.id,
                    orderIndex = index
                )
            )
        }
        workouts.insertPrescriptions(
            sortedTemplateExercises.map { templateExercise ->
                WorkoutPrescription(
                    sessionId = id,
                    exerciseId = templateExercise.exercise.id,
                    source = "Template",
                    targetSets = templateExercise.templateExercise.defaultSets,
                    targetReps = templateExercise.templateExercise.targetReps,
                    targetWeight = templateExercise.templateExercise.targetWeight,
                    adjustment = "Programmed",
                    reason = "Template target"
                )
            }
        )
        activeId.value = id
        prefs.setActiveSessionId(id)
        refresh()
    }

    fun discardWorkout() = viewModelScope.launch {
        activeId.value?.let { workouts.deleteSessionById(it) }
        activeId.value = null
        prefs.setActiveSessionId(null)
        restEndMillis.value = NO_REST_TIMER
        restored.value = false
        refresh()
    }

    fun finishWorkout() = viewModelScope.launch {
        val id = activeId.value ?: return@launch
        val session = workouts.getSessionById(id) ?: return@launch
        val entity = session.session
        val endTime = System.currentTimeMillis()
        val prescriptions = workouts.getPrescriptionsForSession(id)
        workouts.updateSession(entity.copy(endTime = endTime))
        summary.value = session.toSummary(endTime, prescriptions)
        activeId.value = null
        prefs.setActiveSessionId(null)
        restEndMillis.value = NO_REST_TIMER
        restored.value = false
        refresh()
    }

    fun addExercise(exercise: Exercise) = viewModelScope.launch {
        val id = activeId.value ?: return@launch
        val current = workouts.getSessionById(id)
        val alreadyAdded = current?.exercises?.any { it.exercise.id == exercise.id } == true
        if (alreadyAdded) return@launch
        val count = current?.exercises?.size ?: 0
        workouts.insertSessionExercise(
            SessionExercise(
                sessionId = id,
                exerciseId = exercise.id,
                orderIndex = count
            )
        )
        refresh()
    }

    fun setSupersetGroup(entry: SessionExerciseWithSets, group: String?) = viewModelScope.launch {
        workouts.updateSessionExercise(entry.sessionExercise.copy(supersetGroup = group?.takeIf { it.isNotBlank() }))
        refresh()
    }

    fun removeExercise(sessionExerciseId: Int) = viewModelScope.launch {
        workouts.deleteSessionExercise(sessionExerciseId)
        refresh()
    }

    fun addSet(
        entry: SessionExerciseWithSets,
        weight: Double,
        reps: Int,
        setType: String,
        rpe: Double?,
        tempo: String?
    ) = viewModelScope.launch {
        if (weight < 0 || reps <= 0) return@launch
        saving.value = true
        val pr = setType != com.replog.data.model.SetType.WARMUP && workouts.isPR(entry.exercise.id, weight, reps)
        workouts.insertSet(
            SetLog(
                sessionExerciseId = entry.sessionExercise.id,
                setNumber = entry.sets.size + 1,
                weight = weight,
                reps = reps,
                isPR = pr,
                setType = setType,
                rpe = rpe,
                tempo = tempo?.takeIf { it.isNotBlank() }
            )
        )
        restEndMillis.value = System.currentTimeMillis() + (prefs.restSeconds.first() * 1000L)
        saving.value = false
        refresh()
    }

    fun editSet(
        entry: SessionExerciseWithSets,
        set: SetLog,
        weight: Double,
        reps: Int,
        setType: String,
        rpe: Double?,
        tempo: String?
    ) = viewModelScope.launch {
        if (weight < 0 || reps <= 0) return@launch
        saving.value = true
        val pr = setType != com.replog.data.model.SetType.WARMUP && workouts.isPRExcludingSet(entry.exercise.id, weight, reps, set.id)
        workouts.updateSet(
            set.copy(
                weight = weight,
                reps = reps,
                isPR = pr,
                timestamp = System.currentTimeMillis(),
                setType = setType,
                rpe = rpe,
                tempo = tempo?.takeIf { it.isNotBlank() }
            )
        )
        saving.value = false
        refresh()
    }

    fun deleteSet(setId: Int) = viewModelScope.launch {
        workouts.deleteSetById(setId)
        refresh()
    }

    private fun SessionWithExercises.toSummary(endTime: Long, prescriptions: List<WorkoutPrescription>): WorkoutSummary {
        val setCount = exercises.sumOf { it.sets.size }
        val volume = exercises.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } }
        val prCount = exercises.sumOf { entry -> entry.sets.count { it.isPR } }
        val evaluations = prescriptions.mapNotNull { target ->
            val entry = exercises.firstOrNull { it.exercise.id == target.exerciseId } ?: return@mapNotNull null
            val evaluation = ProgressionEngine.evaluate(target, entry.sets)
            Triple(entry.exercise.name, evaluation.hit, evaluation.recommendation)
        }
        val hits = evaluations.count { it.second }
        val misses = evaluations.count { !it.second }
        val notes = evaluations.take(5).map { (name, hit, _) -> if (hit) "$name target hit" else "$name target missed" }
        val progressionNotes = evaluations.take(5).map { (name, _, recommendation) -> "$name: $recommendation" }
        return WorkoutSummary(
            name = session.templateName ?: "Workout",
            durationMillis = endTime - session.startTime,
            exerciseCount = exercises.size,
            setCount = setCount,
            volume = volume,
            prCount = prCount,
            targetHitCount = hits,
            targetMissCount = misses,
            adaptiveNotes = notes,
            progressionNotes = progressionNotes
        )
    }
}
