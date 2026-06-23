package com.replog.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.Exercise
import com.replog.data.model.Goal
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.GoalRepository
import com.replog.data.repository.GoalWithForecast
import com.replog.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<GoalWithForecast> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val useKg: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val exerciseRepository: ExerciseRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.getAll(),
        exerciseRepository.getAllExercises(),
        prefs.useKg,
        refreshTick
    ) { goals, exercises, useKg, _ ->
        val withForecasts = goals.map { goal ->
            GoalWithForecast(goal, goalRepository.forecastFor(goal, useKg))
        }
        GoalsUiState(
            goals = withForecasts,
            exercises = exercises,
            useKg = useKg,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun createStrengthGoal(exercise: Exercise, target1rm: Double) = viewModelScope.launch {
        goalRepository.createStrengthGoal(exercise.id, exercise.name, target1rm, prefs.useKg.first())
        refreshTick.value++
    }

    fun createRepGoal(exercise: Exercise, targetReps: Int) = viewModelScope.launch {
        goalRepository.createRepGoal(exercise.id, exercise.name, targetReps)
        refreshTick.value++
    }

    fun createBodyweightGoal(targetKg: Double) = viewModelScope.launch {
        goalRepository.createBodyweightGoal(targetKg, prefs.useKg.first())
        refreshTick.value++
    }

    fun deleteGoal(goal: Goal) = viewModelScope.launch {
        goalRepository.delete(goal)
        refreshTick.value++
    }
}
