package com.replog.ui.trainingdna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.repository.TrainingDNARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainingDnaUiState(
    val preferredRepRange: String = "—",
    val preferredVolumeRange: String = "—",
    val preferredFrequency: String = "—",
    val strongestMuscles: List<String> = emptyList(),
    val weakestMuscles: List<String> = emptyList(),
    val fastestProgressing: List<String> = emptyList(),
    val stalledExercises: List<String> = emptyList(),
    val averageWorkoutDuration: Double = 0.0,
    val averageRecoveryHours: Double = 0.0,
    val monthlyPRCount: Int = 0,
    val volumeToleranceScore: Double = 0.0,
    val interpretation: com.replog.domain.trainingdna.DnaInterpretation? = null,
    val hasData: Boolean = false
)

@HiltViewModel
class TrainingDnaViewModel @Inject constructor(
    private val trainingDNARepository: TrainingDNARepository
) : ViewModel() {

    val uiState: StateFlow<TrainingDnaUiState> = combine(
        trainingDNARepository.getLatestDNA(),
        trainingDNARepository.getPlateauEvents()
    ) { snapshot, plateaus ->
        if (snapshot == null) {
            TrainingDnaUiState()
        } else {
            TrainingDnaUiState(
                preferredRepRange = snapshot.preferredRepRange,
                preferredVolumeRange = snapshot.preferredVolumeRange,
                preferredFrequency = snapshot.preferredFrequency,
                strongestMuscles = snapshot.strongestMuscles.split(",").map { it.trim() }.filter { it.isNotBlank() },
                weakestMuscles = snapshot.weakestMuscles.split(",").map { it.trim() }.filter { it.isNotBlank() },
                fastestProgressing = snapshot.fastestProgressingExercises.split(",").map { it.trim() }.filter { it.isNotBlank() },
                stalledExercises = plateaus.map { it.exerciseName }.distinct().takeIf { it.isNotEmpty() }
                    ?: snapshot.stalledExercises.split(",").map { it.trim() }.filter { it.isNotBlank() },
                averageWorkoutDuration = snapshot.averageWorkoutDuration,
                averageRecoveryHours = snapshot.averageRecoveryHours,
                monthlyPRCount = snapshot.monthlyPRCount,
                volumeToleranceScore = snapshot.volumeToleranceScore,
                interpretation = com.replog.domain.trainingdna.DnaInterpreter.interpret(snapshot),
                hasData = true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingDnaUiState())

    fun refresh() = viewModelScope.launch {
        trainingDNARepository.generateDNA()
    }
}
