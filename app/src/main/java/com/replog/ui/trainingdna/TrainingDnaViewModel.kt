package com.replog.ui.trainingdna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.repository.BodyweightRepository
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.TrainingDNARepository
import com.replog.data.repository.WorkoutRepository
import com.replog.domain.adaptive.AdaptiveSwap
import com.replog.domain.adaptive.AdaptiveTemplateAdvisor
import com.replog.domain.forecast.ProgressionForecast
import com.replog.domain.forecast.ProgressionForecaster
import com.replog.domain.recommendation.RecoveryAnalyzer
import com.replog.domain.recovery.RecoveryDashboard
import com.replog.domain.recovery.RecoveryDashboardState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A forecast paired with the exercise name, ready for display. */
data class NamedForecast(val exerciseName: String, val forecast: ProgressionForecast)

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
    // Priority 3 additions
    val recovery: RecoveryDashboardState? = null,
    val forecasts: List<NamedForecast> = emptyList(),
    val adaptiveSwaps: List<AdaptiveSwap> = emptyList(),
    val hasData: Boolean = false
)

@HiltViewModel
class TrainingDnaViewModel @Inject constructor(
    private val trainingDNARepository: TrainingDNARepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val bodyweightRepository: BodyweightRepository
) : ViewModel() {

    val uiState: StateFlow<TrainingDnaUiState> = combine(
        trainingDNARepository.getLatestDNA(),
        trainingDNARepository.getPlateauEvents(),
        trainingDNARepository.getProgressionScores(),
        exerciseRepository.getAllExercises(),
        combine(workoutRepository.getAllSessions(), bodyweightRepository.getAllBodyweights()) { s, b -> s to b }
    ) { snapshot, plateaus, scores, exercises, sessionsAndBw ->
        val (sessions, bodyweights) = sessionsAndBw
        val now = System.currentTimeMillis()

        // Recovery dashboard (#13) from the existing analyzer.
        val completed = sessions.filter { it.session.endTime != null }
        val recovery = RecoveryDashboard.from(
            RecoveryAnalyzer.overallRecovery(completed, bodyweights, now)
        )

        if (snapshot == null) {
            return@combine TrainingDnaUiState(recovery = recovery, hasData = false)
        }

        val exById = exercises.associateBy { it.id }

        // Progression forecasts (#11) — top rising/most-recent exercises.
        val forecasts = scores
            .filter { it.estimatedOneRm30Day > 0 || it.estimatedOneRm90Day > 0 }
            .sortedByDescending { it.calculatedAt }
            .distinctBy { it.exerciseId }
            .take(5)
            .mapNotNull { score ->
                val name = exById[score.exerciseId]?.name ?: return@mapNotNull null
                NamedForecast(name, ProgressionForecaster.forecast(score))
            }

        // Adaptive template swaps (#14) for stalled lifts.
        val swaps = AdaptiveTemplateAdvisor.suggestSwaps(plateaus, exercises)

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
            recovery = recovery,
            forecasts = forecasts,
            adaptiveSwaps = swaps,
            hasData = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingDnaUiState())

    fun refresh() = viewModelScope.launch {
        trainingDNARepository.generateDNA()
    }
}
