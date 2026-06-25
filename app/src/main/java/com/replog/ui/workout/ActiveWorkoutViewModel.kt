package com.replog.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.SetType
import com.replog.data.model.TemplateExercise
import com.replog.data.model.TemplateWithExercises
import com.replog.data.model.WorkoutPrescription
import com.replog.data.model.WorkoutSession
import com.replog.data.model.WorkoutTemplate
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.TrainingDNARepository
import com.replog.data.repository.WorkoutRepository
import com.replog.domain.pr.PRDetector
import com.replog.domain.progression.ProgressionSuggester
import com.replog.domain.scoring.WorkoutScorer
import com.replog.util.ActiveWorkoutRecovery
import com.replog.util.ActiveWorkoutRecoveryDecision
import com.replog.util.AdaptiveProgramEngine
import com.replog.util.AdaptiveWorkoutPlan
import com.replog.util.PreferencesManager
import com.replog.util.ProgressionEngine
import com.replog.util.timer.RestTimerManager
import com.replog.util.timer.RestTimerState
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

data class ProgressionSuggestionUi(
    val weight: Double,
    val reps: Int,
    val rpe: Double?,
    val label: String
)

data class WorkoutSummary(
    val sessionId: Int = 0,
    val name: String,
    val durationMillis: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val repCount: Int,
    val volume: Double,
    val prCount: Int,
    val qualityScore: Int,
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
    val progressionByExerciseId: Map<Int, ProgressionSuggestionUi> = emptyMap(),
    val restTimer: RestTimerState = RestTimerState(),
    val restAutoStart: Boolean = true,
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
    private val trainingDnaRepository: TrainingDNARepository,
    exercises: ExerciseRepository,
    private val prefs: PreferencesManager,
    private val restTimer: RestTimerManager,
    private val coachHandoff: com.replog.ui.coach.CoachHandoff,
    private val recommendationRepository: com.replog.data.repository.RecommendationRepository,
    private val dataSeeder: com.replog.util.DataSeeder
) : ViewModel() {

    /** Install the full built-in template catalog (idempotent — no duplicates). */
    fun installAllBuiltInTemplates() = viewModelScope.launch {
        dataSeeder.installAllBuiltInTemplates()
        refresh()
    }

    // --- Template sharing / import (account-free, offline) ---

    /** Transient one-line confirmation for share/import actions. */
    private val templateMessageFlow = MutableStateFlow<String?>(null)
    val templateMessage: StateFlow<String?> = templateMessageFlow

    fun clearTemplateMessage() { templateMessageFlow.value = null }

    /**
     * Serialize a template to a portable .replogtemplate file in the cache and
     * return (fileName, json) so the screen can share it via the OS share sheet.
     */
    fun buildShareableTemplate(template: TemplateWithExercises): Pair<String, String> {
        val json = com.replog.util.TemplateShare.encode(template)
        val fileName = com.replog.util.TemplateShare.fileNameFor(template.template.name)
        return fileName to json
    }

    /** Import a shared template from raw JSON text (read from a picked file). */
    fun importTemplateJson(json: String) = viewModelScope.launch {
        runCatching {
            val shared = com.replog.util.TemplateShare.decode(json)
            dataSeeder.importSharedTemplate(shared)
        }.onSuccess { name ->
            templateMessageFlow.value = "Imported template: $name"
            refresh()
        }.onFailure { error ->
            templateMessageFlow.value = "Import failed: ${error.message ?: "not a valid RepLog template"}"
        }
    }
    // Phase 3 — recommendation accepted via the Smart Coach card and pending completion.
    private var pendingCoachRecommendation: com.replog.domain.recommendation.Recommendation? = null
    private val activeId = MutableStateFlow<Int?>(null)
    private val tick = MutableStateFlow(0)
    private val saving = MutableStateFlow(false)
    private val restored = MutableStateFlow(false)
    private val summary = MutableStateFlow<WorkoutSummary?>(null)

    private val previousWorkoutCache = mutableMapOf<Int, List<SetLog>>()
    private var cachedForSessionId: Int? = null
    private var cachedAdaptivePlan: AdaptiveWorkoutPlan? = null
    private var cachedAdaptivePlanForSessionCount: Int = -1

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        activeId, tick, saving,
        exercises.getAllExercises(),
        workouts.getAllTemplates(),
        prefs.useKg,
        restored,
        summary,
        restTimer.state,
        workouts.getAllSessions(),
        prefs.restAutoStart
    ) { args ->
        val id = args[0] as Int?
        @Suppress("UNCHECKED_CAST") val isSaving = args[2] as Boolean
        @Suppress("UNCHECKED_CAST") val allExercises = args[3] as List<Exercise>
        @Suppress("UNCHECKED_CAST") val templates = args[4] as List<TemplateWithExercises>
        val useKg = args[5] as Boolean
        val restoredWorkout = args[6] as Boolean
        val currentSummary = args[7] as WorkoutSummary?
        val timerState = args[8] as RestTimerState
        @Suppress("UNCHECKED_CAST") val allSessions = args[9] as List<SessionWithExercises>
        val restAutoStart = args[10] as Boolean

        val session = id?.let { workouts.getSessionById(it) }
        val prescriptions = id?.let { workouts.getPrescriptionsForSession(it) }.orEmpty()

        if (cachedForSessionId != id) {
            previousWorkoutCache.clear()
            cachedForSessionId = id
        }
        val exerciseIds = session?.exercises?.map { it.exercise.id }.orEmpty()
        previousWorkoutCache.keys.retainAll(exerciseIds.toSet())
        for (exId in exerciseIds) {
            if (!previousWorkoutCache.containsKey(exId)) {
                previousWorkoutCache[exId] = workouts.getPreviousWorkoutSetsForExercise(exId, id)
            }
        }
        val lastSets = previousWorkoutCache.toMap()

        val progression = lastSets.mapValues { (_, sets) ->
            ProgressionSuggester.suggest(sets, useKg)?.let {
                ProgressionSuggestionUi(it.weight, it.reps, it.rpe, it.source)
            }
        }.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

        val adaptivePlan = if (id == null) {
            if (cachedAdaptivePlan == null || cachedAdaptivePlanForSessionCount != allSessions.size) {
                cachedAdaptivePlan = AdaptiveProgramEngine.buildPlan(allSessions)
                cachedAdaptivePlanForSessionCount = allSessions.size
            }
            cachedAdaptivePlan
        } else null

        ActiveWorkoutUiState(
            activeSessionId = id,
            startTime = session?.session?.startTime,
            session = session,
            allExercises = allExercises,
            templates = templates,
            lastSetsByExerciseId = lastSets,
            progressionByExerciseId = progression,
            restTimer = timerState,
            restAutoStart = restAutoStart,
            useKg = useKg,
            isSaving = isSaving,
            restoredWorkout = restoredWorkout,
            summary = currentSummary,
            adaptivePlan = adaptivePlan,
            targetsByExerciseId = prescriptions.associate { p ->
                p.exerciseId to WorkoutTargetUi(
                    p.exerciseId,
                    session?.exercises?.firstOrNull { it.exercise.id == p.exerciseId }?.exercise?.name.orEmpty(),
                    p.targetSets, p.targetReps, p.targetWeight, p.adjustment, p.reason, p.source
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    init {
        viewModelScope.launch {
            val storedId = prefs.activeSessionId.first()
            if (storedId != null) {
                val session = workouts.getSessionEntity(storedId)
                when (ActiveWorkoutRecovery.decide(storedId, session != null, session?.endTime != null)) {
                    ActiveWorkoutRecoveryDecision.RESUME -> { activeId.value = storedId; restored.value = true; refresh() }
                    ActiveWorkoutRecoveryDecision.CLEAR_STALE -> prefs.setActiveSessionId(null)
                    ActiveWorkoutRecoveryDecision.NONE -> Unit
                }
            }
        }
    }

    fun refresh() { tick.value++ }
    fun dismissRestoredBanner() { restored.value = false }
    fun dismissSummary() { summary.value = null }

    /**
     * Session Rating (#10): persist the user's 1–5 rating for the just-finished
     * session and refresh Training DNA so the signal feeds future recommendations.
     */
    fun rateWorkout(rating: Int) {
        val id = summary.value?.sessionId ?: return
        if (id <= 0) return
        viewModelScope.launch {
            workouts.setSessionRating(id, rating.coerceIn(1, 5))
            try { trainingDnaRepository.generateDNA() } catch (_: Exception) {}
        }
    }

    fun skipRestTimer() = viewModelScope.launch { restTimer.skip() }
    fun addRestSeconds(seconds: Int) = viewModelScope.launch { restTimer.addSeconds(seconds) }
    fun restartRestTimer() = viewModelScope.launch { restTimer.restart() }
    fun startRestTimer(seconds: Int) = viewModelScope.launch { restTimer.start(seconds, null, null) }
    fun setRestAutoStart(enabled: Boolean) = viewModelScope.launch { prefs.setRestAutoStart(enabled) }

    fun startWorkout(name: String? = null) = viewModelScope.launch {
        summary.value = null; previousWorkoutCache.clear(); cachedForSessionId = null; restTimer.cancel()
        val id = workouts.insertSession(WorkoutSession(templateName = name?.ifBlank { null }, startTime = System.currentTimeMillis())).toInt()
        activeId.value = id; prefs.setActiveSessionId(id); refresh()
    }

    fun createTemplate(name: String, selectedExercises: List<TemplateExerciseDraft>) = viewModelScope.launch {
        val cleanName = name.trim()
        if (cleanName.isBlank() || selectedExercises.isEmpty()) return@launch
        val templateId = workouts.insertTemplate(WorkoutTemplate(name = cleanName, isBuiltIn = false)).toInt()
        selectedExercises.forEachIndexed { index, draft ->
            workouts.insertTemplateExercise(TemplateExercise(templateId = templateId, exerciseId = draft.exercise.id, defaultSets = draft.defaultSets.coerceAtLeast(1), orderIndex = index, targetReps = draft.targetReps.coerceAtLeast(1), targetWeight = draft.targetWeight))
        }
        refresh()
    }

    fun updateTemplate(template: TemplateWithExercises, name: String, selectedExercises: List<TemplateExerciseDraft>) = viewModelScope.launch {
        val cleanName = name.trim()
        if (template.template.isBuiltIn || cleanName.isBlank() || selectedExercises.isEmpty()) return@launch
        workouts.updateTemplate(template.template.copy(name = cleanName))
        workouts.deleteTemplateExercises(template.template.id)
        selectedExercises.forEachIndexed { index, draft ->
            workouts.insertTemplateExercise(TemplateExercise(templateId = template.template.id, exerciseId = draft.exercise.id, defaultSets = draft.defaultSets.coerceAtLeast(1), orderIndex = index, targetReps = draft.targetReps.coerceAtLeast(1), targetWeight = draft.targetWeight))
        }
        refresh()
    }

    fun deleteTemplate(template: TemplateWithExercises) = viewModelScope.launch {
        if (!template.template.isBuiltIn) { workouts.deleteTemplate(template.template); refresh() }
    }

    fun startAdaptiveWorkout(plan: AdaptiveWorkoutPlan) = viewModelScope.launch {
        val source = plan.sourceSessionId?.let { workouts.getSessionById(it) } ?: return@launch
        summary.value = null; previousWorkoutCache.clear(); restTimer.cancel()
        val id = workouts.insertSession(WorkoutSession(templateName = plan.title, startTime = System.currentTimeMillis())).toInt()
        source.exercises.sortedBy { it.sessionExercise.orderIndex }.forEachIndexed { index, entry ->
            workouts.insertSessionExercise(SessionExercise(sessionId = id, exerciseId = entry.exercise.id, orderIndex = index, supersetGroup = entry.sessionExercise.supersetGroup, notes = ""))
        }
        workouts.insertPrescriptions(plan.targets.map { t -> WorkoutPrescription(sessionId = id, exerciseId = t.exerciseId, source = "Adaptive", targetSets = t.suggestedSets, targetReps = t.suggestedReps, targetWeight = t.suggestedWeight, adjustment = t.adjustment, reason = t.reason) })
        activeId.value = id; prefs.setActiveSessionId(id); refresh()
    }

    /**
     * Phase 3 — Smart Start. If the Coach card staged a recommendation, build the
     * active session directly from its WorkoutPlan (uses the existing prescription
     * pipeline; introduces no new planning logic). Safe no-op if nothing is staged.
     */
    fun consumePendingRecommendation() = viewModelScope.launch {
        val rec = coachHandoff.consume() ?: return@launch
        val plan = rec.workoutPlan ?: return@launch
        summary.value = null; previousWorkoutCache.clear(); cachedForSessionId = null; restTimer.cancel()
        val id = workouts.insertSession(
            WorkoutSession(templateName = rec.title, startTime = System.currentTimeMillis())
        ).toInt()
        plan.exercises.forEachIndexed { index, planned ->
            workouts.insertSessionExercise(
                SessionExercise(sessionId = id, exerciseId = planned.exerciseId, orderIndex = index, notes = "")
            )
        }
        workouts.insertPrescriptions(plan.exercises.map { planned ->
            WorkoutPrescription(
                sessionId = id,
                exerciseId = planned.exerciseId,
                source = "Coach",
                targetSets = planned.targetSets,
                targetReps = planned.targetReps,
                targetWeight = planned.targetWeight,
                adjustment = planned.progression.name,
                reason = planned.reason
            )
        })
        pendingCoachRecommendation = rec
        activeId.value = id; prefs.setActiveSessionId(id); refresh()
    }

    fun startWorkoutFromTemplate(template: TemplateWithExercises) = viewModelScope.launch {
        summary.value = null; previousWorkoutCache.clear(); cachedForSessionId = null; restTimer.cancel()
        val id = workouts.insertSession(WorkoutSession(templateName = template.template.name, startTime = System.currentTimeMillis())).toInt()
        template.exercises.sortedBy { it.templateExercise.orderIndex }.forEachIndexed { index, te ->
            workouts.insertSessionExercise(SessionExercise(sessionId = id, exerciseId = te.exercise.id, orderIndex = index, notes = ""))
        }
        workouts.insertPrescriptions(template.exercises.map { te -> WorkoutPrescription(sessionId = id, exerciseId = te.exercise.id, source = "Template", targetSets = te.templateExercise.defaultSets, targetReps = te.templateExercise.targetReps, targetWeight = te.templateExercise.targetWeight, adjustment = "Programmed", reason = "Template target") })
        activeId.value = id; prefs.setActiveSessionId(id); refresh()
    }

    fun discardWorkout() = viewModelScope.launch {
        activeId.value?.let { workouts.deleteSessionById(it) }
        activeId.value = null; previousWorkoutCache.clear(); cachedForSessionId = null
        prefs.setActiveSessionId(null); restTimer.cancel(); restored.value = false; refresh()
    }

    fun finishWorkout() = viewModelScope.launch {
        val id = activeId.value ?: return@launch
        val session = workouts.getSessionById(id) ?: return@launch
        val endTime = System.currentTimeMillis()
        val prescriptions = workouts.getPrescriptionsForSession(id)
        val setCount = session.exercises.sumOf { it.sets.size }
        val repCount = session.exercises.sumOf { e -> e.sets.sumOf { it.reps } }
        val volume = session.exercises.sumOf { e -> e.sets.sumOf { it.weight * it.reps } }
        val prCount = session.exercises.sumOf { e -> e.sets.count { it.isPR } }
        val scoreResult = WorkoutScorer.score(session, (endTime - session.session.startTime) / 60000, prCount)
        workouts.updateSession(session.session.copy(endTime = endTime, qualityScore = scoreResult.score, totalVolume = volume, totalSets = setCount, totalReps = repCount, prCount = prCount))
        // Sprint 5: regenerate Training DNA after the completed session is persisted.
        // Must never block or fail the workout summary.
        try {
            trainingDnaRepository.generateDNA()
        } catch (_: Exception) {
            // DNA generation is best-effort; ignore failures here.
        }
        // Phase 3 — if this workout came from an accepted Coach recommendation,
        // upgrade its history outcome to "Completed". Best-effort.
        pendingCoachRecommendation?.let { rec ->
            try {
                recommendationRepository.recordAcceptance(rec, completed = true)
            } catch (_: Exception) {
            } finally {
                pendingCoachRecommendation = null
            }
        }
        summary.value = session.toSummary(endTime, prescriptions, scoreResult.score, repCount)
        activeId.value = null; previousWorkoutCache.clear(); cachedForSessionId = null
        prefs.setActiveSessionId(null); restTimer.cancel(); restored.value = false; refresh()
    }

    fun addExercise(exercise: Exercise) = viewModelScope.launch {
        val id = activeId.value ?: return@launch
        val current = workouts.getSessionById(id)
        if (current?.exercises?.any { it.exercise.id == exercise.id } == true) return@launch
        val count = current?.exercises?.size ?: 0
        workouts.insertSessionExercise(SessionExercise(sessionId = id, exerciseId = exercise.id, orderIndex = count, notes = ""))
        previousWorkoutCache[exercise.id] = workouts.getPreviousWorkoutSetsForExercise(exercise.id, id)
        refresh()
    }

    fun setSupersetGroup(entry: SessionExerciseWithSets, group: String?) = viewModelScope.launch {
        workouts.updateSessionExercise(entry.sessionExercise.copy(supersetGroup = group?.takeIf { it.isNotBlank() }))
        refresh()
    }

    fun removeExercise(sessionExerciseId: Int) = viewModelScope.launch {
        workouts.deleteSessionExercise(sessionExerciseId); refresh()
    }

    fun moveExercise(sessionId: Int, fromIndex: Int, toIndex: Int) = viewModelScope.launch {
        val session = workouts.getSessionById(sessionId) ?: return@launch
        val ordered = session.exercises.sortedBy { it.sessionExercise.orderIndex }.toMutableList()
        if (fromIndex !in ordered.indices || toIndex !in 0..ordered.lastIndex) return@launch
        val item = ordered.removeAt(fromIndex); ordered.add(toIndex, item)
        workouts.reorderSessionExercises(sessionId, ordered.map { it.sessionExercise.id })
        refresh()
    }

    fun updateExerciseNotes(entry: SessionExerciseWithSets, notes: String) = viewModelScope.launch {
        workouts.updateSessionExercise(entry.sessionExercise.copy(notes = notes.take(500))); refresh()
    }

    fun updateSessionNotes(notes: String) = viewModelScope.launch {
        val id = activeId.value ?: return@launch
        val session = workouts.getSessionEntity(id) ?: return@launch
        workouts.updateSession(session.copy(notes = notes.take(2000))); refresh()
    }

    fun addSet(entry: SessionExerciseWithSets, weight: Double, reps: Int, setType: String, rpe: Double?, tempo: String?) = viewModelScope.launch {
        if (weight < 0 || reps <= 0) return@launch
        saving.value = true
        val pr = workouts.checkPR(entry.exercise.id, weight, reps)
        val setId = workouts.insertSet(SetLog(sessionExerciseId = entry.sessionExercise.id, setNumber = entry.sets.size + 1, weight = weight, reps = reps, isPR = pr.isPR, prType = pr.types.firstOrNull(), setType = setType, rpe = rpe, tempo = tempo?.takeIf { it.isNotBlank() }, completed = true)).toInt()
        if (prefs.restAutoStart.first()) {
            val restSeconds = restTimer.resolveRestSeconds(entry.exercise)
            restTimer.start(restSeconds, entry.sessionExercise.id, setId)
        }
        saving.value = false; refresh()
    }

    fun repeatLastSet(entry: SessionExerciseWithSets) = viewModelScope.launch {
        val lastCurrent = entry.sets.maxByOrNull { it.setNumber }
        val lastPrevious = previousWorkoutCache[entry.exercise.id]?.lastOrNull()
        val source = lastCurrent ?: lastPrevious ?: return@launch
        saving.value = true
        val pr = workouts.checkPR(entry.exercise.id, source.weight, source.reps)
        val setId = workouts.insertSet(SetLog(sessionExerciseId = entry.sessionExercise.id, setNumber = entry.sets.size + 1, weight = source.weight, reps = source.reps, isPR = pr.isPR, prType = pr.types.firstOrNull(), setType = source.setType, rpe = source.rpe, tempo = source.tempo, completed = true)).toInt()
        if (prefs.restAutoStart.first()) {
            val restSeconds = restTimer.resolveRestSeconds(entry.exercise)
            restTimer.start(restSeconds, entry.sessionExercise.id, setId)
        }
        saving.value = false; refresh()
    }

    fun quickCompleteSet(entry: SessionExerciseWithSets, weight: Double, reps: Int) = viewModelScope.launch {
        // track progression acceptance
        prefs.trackProgressionAccepted()
        addSet(entry, weight, reps, SetType.WORKING, null, null)
    }

    fun editSet(entry: SessionExerciseWithSets, set: SetLog, weight: Double, reps: Int, setType: String, rpe: Double?, tempo: String?) = viewModelScope.launch {
        if (weight < 0 || reps <= 0) return@launch
        saving.value = true
        val pr = workouts.checkPR(entry.exercise.id, weight, reps, set.id)
        workouts.updateSet(set.copy(weight = weight, reps = reps, isPR = pr.isPR, prType = pr.types.firstOrNull(), timestamp = System.currentTimeMillis(), setType = setType, rpe = rpe, tempo = tempo?.takeIf { it.isNotBlank() }, completed = true))
        saving.value = false; refresh()
    }

    fun deleteSet(setId: Int) = viewModelScope.launch { workouts.deleteSetById(setId); refresh() }

    private fun SessionWithExercises.toSummary(endTime: Long, prescriptions: List<WorkoutPrescription>, qualityScore: Int = 0, repCount: Int = 0): WorkoutSummary {
        val setCount = exercises.sumOf { it.sets.size }
        val reps = if (repCount > 0) repCount else exercises.sumOf { e -> e.sets.sumOf { it.reps } }
        val volume = exercises.sumOf { e -> e.sets.sumOf { it.weight * it.reps } }
        val prCount = exercises.sumOf { e -> e.sets.count { it.isPR } }
        val evaluations = prescriptions.mapNotNull { target ->
            val entry = exercises.firstOrNull { it.exercise.id == target.exerciseId } ?: return@mapNotNull null
            val evaluation = ProgressionEngine.evaluate(target, entry.sets)
            Triple(entry.exercise.name, evaluation.hit, evaluation.recommendation)
        }
        val hits = evaluations.count { it.second }
        val misses = evaluations.count { !it.second }
        return WorkoutSummary(
            sessionId = session.id,
            name = session.templateName ?: "Workout",
            durationMillis = endTime - session.startTime,
            exerciseCount = exercises.size,
            setCount = setCount,
            repCount = reps,
            volume = volume,
            prCount = prCount,
            qualityScore = qualityScore,
            targetHitCount = hits,
            targetMissCount = misses,
            adaptiveNotes = evaluations.take(5).map { (n, h, _) -> if (h) "$n target hit" else "$n target missed" },
            progressionNotes = evaluations.take(5).map { (n, _, r) -> "$n: $r" }
        )
    }
}
