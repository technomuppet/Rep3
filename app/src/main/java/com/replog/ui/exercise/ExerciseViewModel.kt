package com.replog.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.Exercise
import com.replog.data.model.ExerciseInsight
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.WorkoutRepository
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
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val useKg: Boolean = true,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val prefs: PreferencesManager
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow("All")
    private val selectedExercise = MutableStateFlow<Exercise?>(null)

    val uiState: StateFlow<ExerciseUiState> = combine(
        exerciseRepository.getAllExercises(),
        exerciseRepository.getAllCategories(),
        query,
        category,
        prefs.useKg
    ) { exercises, categories, q, cat, useKg ->
        val filtered = exercises.filter {
            (cat == "All" || it.category == cat) &&
                (q.isBlank() || it.name.contains(q, true) || it.category.contains(q, true) || it.equipment.contains(q, true) || it.muscles.contains(q, true) || it.primaryMuscles.contains(q, true) || it.secondaryMuscles.contains(q, true) || it.movementPattern.contains(q, true))
        }
        ExerciseUiState(
            exercises = filtered,
            categories = listOf("All") + categories,
            selectedCategory = cat,
            searchQuery = q,
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

    fun onSearchQueryChanged(value: String) { query.value = value }
    fun onCategorySelected(value: String) { category.value = value }
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
