package com.replog.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.domain.library.QuickWorkout
import com.replog.domain.library.QuickWorkouts
import com.replog.domain.library.WorkoutCategory
import com.replog.domain.library.WorkoutEquipment
import com.replog.domain.library.WorkoutGoal
import com.replog.util.DataSeeder
import com.replog.util.WorkoutStarter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickWorkoutsUiState(
    val results: List<QuickWorkout> = QuickWorkouts.ALL,
    val selectedCategory: WorkoutCategory? = null,
    val selectedGoal: WorkoutGoal? = null,
    val selectedEquipment: WorkoutEquipment? = null,
    val maxMinutes: Int? = null,
    val durationOptions: List<Int> = listOf(20, 30, 45, 60, 90),
    val message: String? = null
)

@HiltViewModel
class QuickWorkoutsViewModel @Inject constructor(
    private val workoutStarter: WorkoutStarter,
    private val dataSeeder: DataSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickWorkoutsUiState())
    val uiState: StateFlow<QuickWorkoutsUiState> = _uiState.asStateFlow()

    private fun recompute(s: QuickWorkoutsUiState): QuickWorkoutsUiState = s.copy(
        results = QuickWorkouts.filter(s.selectedCategory, s.selectedGoal, s.selectedEquipment, s.maxMinutes)
    )

    fun toggleCategory(value: WorkoutCategory) = _uiState.update {
        recompute(it.copy(selectedCategory = if (it.selectedCategory == value) null else value))
    }

    fun toggleGoal(value: WorkoutGoal) = _uiState.update {
        recompute(it.copy(selectedGoal = if (it.selectedGoal == value) null else value))
    }

    fun toggleEquipment(value: WorkoutEquipment) = _uiState.update {
        recompute(it.copy(selectedEquipment = if (it.selectedEquipment == value) null else value))
    }

    fun toggleDuration(value: Int) = _uiState.update {
        recompute(it.copy(maxMinutes = if (it.maxMinutes == value) null else value))
    }

    /** Start the workout immediately (sets it active; the Training screen resumes it). */
    fun startWorkout(workout: QuickWorkout) = viewModelScope.launch {
        workoutStarter.startQuickWorkout(workout)
    }

    /** Duplicate into the user's editable templates without touching the original. */
    fun saveToTemplates(workout: QuickWorkout) = viewModelScope.launch {
        val name = runCatching { dataSeeder.duplicateQuickWorkout(workout) }.getOrNull()
        _uiState.update {
            it.copy(message = if (name != null) "Saved \"$name\" to your templates" else "Could not save this workout")
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
