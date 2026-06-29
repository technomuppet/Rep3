package com.replog.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.Exercise
import com.replog.data.model.ExerciseInsight
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.IntelligenceRepository
import com.replog.data.repository.WorkoutRepository
import com.replog.domain.library.BeginnerGuidance
import com.replog.domain.library.CoachingInfo
import com.replog.domain.library.ConfidenceCard
import com.replog.domain.library.EasierAlternative
import com.replog.domain.library.ExerciseCoach
import com.replog.domain.library.ExerciseFilter
import com.replog.domain.library.ExerciseFilterState
import com.replog.domain.library.WhyThisExercise
import com.replog.domain.swap.ExerciseSwap
import com.replog.domain.swap.ExerciseSwapEngine
import com.replog.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseUiState(
    val exercises: List<Exercise> = emptyList(),
    val muscleGroups: List<String> = ExerciseFilter.MUSCLE_GROUPS,
    val equipmentOptions: List<String> = emptyList(),
    val difficultyOptions: List<String> = listOf("Beginner", "Intermediate", "Advanced"),
    val patternOptions: List<String> = ExerciseFilter.PATTERNS,
    val goalOptions: List<String> = ExerciseFilter.GOALS,
    val experienceOptions: List<String> = ExerciseFilter.EXPERIENCES,
    val equipmentPresetOptions: List<String> = ExerciseFilter.EQUIPMENT_PRESETS,
    val filter: ExerciseFilterState = ExerciseFilterState(),
    val resultCount: Int = 0,
    val useKg: Boolean = true,
    val isLoading: Boolean = true
)

/**
 * The generated beginner-coaching bundle for the selected exercise (Sprint 13).
 * All fields are derived at runtime; nothing is stored.
 */
data class ExerciseCoaching(
    val coaching: CoachingInfo,
    val confidence: ConfidenceCard,
    val why: String,
    val easier: EasierAlternative?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val intelligenceRepository: IntelligenceRepository,
    private val prefs: PreferencesManager
) : ViewModel() {
    private val filter = MutableStateFlow(ExerciseFilterState())
    private val selectedExercise = MutableStateFlow<Exercise?>(null)

    val uiState: StateFlow<ExerciseUiState> = combine(
        exerciseRepository.getAllExercises(),
        filter,
        prefs.useKg
    ) { exercises, f, useKg ->
        val filtered = ExerciseFilter.apply(exercises, f)
        ExerciseUiState(
            exercises = filtered,
            equipmentOptions = exercises.map { it.equipment }.distinct().sorted(),
            filter = f,
            resultCount = filtered.size,
            useKg = useKg,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseUiState())

    val selectedInsight: StateFlow<ExerciseInsight?> = selectedExercise.flatMapLatest { exercise ->
        if (exercise == null) {
            flowOf(null)
        } else {
            workoutRepository.getExerciseHistory(exercise.id).map { history ->
                ExerciseInsight(
                    exercise = exercise,
                    history = history,
                    bestWeight = history.maxOfOrNull { it.weight } ?: 0.0,
                    bestEstimatedOneRm = history.maxOfOrNull { it.estimatedOneRm } ?: 0.0,
                    bestVolumeSet = history.maxOfOrNull { it.volume } ?: 0.0,
                    totalVolume = history.sumOf { it.volume },
                    totalSets = history.size,
                    prCount = history.count { it.isPR }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Smart Exercise Swap: alternatives for the currently-selected exercise. */
    val swapSuggestions: StateFlow<List<ExerciseSwap>> = combine(
        selectedExercise,
        exerciseRepository.getAllExercises()
    ) { selected, library ->
        if (selected == null) emptyList()
        else ExerciseSwapEngine.alternatives(selected, library, limit = 5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Cached set of muscles the Recovery Centre currently reports as FRESH (fully
    // recovered). Loaded lazily and reused; never invented. Lowercased names.
    private val recoveredMuscles = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            val data = runCatching { intelligenceRepository.buildRecoveryCentre() }.getOrNull()
            recoveredMuscles.value = data?.recovered
                ?.filter { it.status.equals("FRESH", true) }
                ?.map { it.muscle.lowercase() }?.toSet()
                ?: emptySet()
        }
    }

    /** Generated coaching bundle for the selected exercise (Sprint 13). */
    val selectedCoaching: StateFlow<ExerciseCoaching?> = combine(
        selectedExercise,
        exerciseRepository.getAllExercises(),
        prefs.profileGoal,
        prefs.displayName,
        recoveredMuscles
    ) { selected, library, goal, name, recovered ->
        if (selected == null) return@combine null
        val primary = selected.primaryMuscles.split(",").firstOrNull()?.trim()?.lowercase()
        val recoveredMatch = primary?.let { p -> recovered.firstOrNull { it == p || it.contains(p) || p.contains(it) } }
        ExerciseCoaching(
            coaching = ExerciseCoach.coach(selected),
            confidence = BeginnerGuidance.confidence(selected),
            why = WhyThisExercise.rationale(selected, goal, name, recoveredMatch),
            easier = BeginnerGuidance.easierAlternative(selected, library)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onSearchQueryChanged(value: String) { filter.value = filter.value.copy(query = value) }

    private fun toggle(set: Set<String>, value: String): Set<String> =
        if (value in set) set - value else set + value

    fun toggleMuscle(value: String) { filter.value = filter.value.copy(muscles = toggle(filter.value.muscles, value)) }
    fun toggleEquipment(value: String) { filter.value = filter.value.copy(equipment = toggle(filter.value.equipment, value)) }
    fun toggleDifficulty(value: String) { filter.value = filter.value.copy(difficulties = toggle(filter.value.difficulties, value)) }
    fun togglePattern(value: String) { filter.value = filter.value.copy(patterns = toggle(filter.value.patterns, value)) }
    fun toggleGoal(value: String) { filter.value = filter.value.copy(goals = toggle(filter.value.goals, value)) }
    fun toggleExperience(value: String) { filter.value = filter.value.copy(experiences = toggle(filter.value.experiences, value)) }
    fun toggleEquipmentPreset(value: String) { filter.value = filter.value.copy(equipmentPresets = toggle(filter.value.equipmentPresets, value)) }
    fun clearFilters() { filter.value = ExerciseFilterState(query = filter.value.query) }

    fun selectExercise(exercise: Exercise) { selectedExercise.value = exercise }
    fun clearSelectedExercise() { selectedExercise.value = null }

    fun addCustomExercise(name: String, category: String, equipment: String, muscles: String) = viewModelScope.launch {
        if (name.isNotBlank()) {
            exerciseRepository.insertExercise(
                Exercise(
                    name = name.trim(),
                    category = category.ifBlank { "Custom" }.trim(),
                    equipment = equipment.ifBlank { "Other" }.trim(),
                    muscles = muscles.trim(),
                    primaryMuscles = muscles.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(1).joinToString(", "),
                    secondaryMuscles = muscles.split(",").map { it.trim() }.filter { it.isNotBlank() }.drop(1).joinToString(", "),
                    movementPattern = category.ifBlank { "Custom" }.trim(),
                    difficulty = "Custom",
                    isCustom = true
                )
            )
        }
    }

    fun deleteExercise(exercise: Exercise) = viewModelScope.launch {
        if (exercise.isCustom) exerciseRepository.deleteExercise(exercise)
    }
}
