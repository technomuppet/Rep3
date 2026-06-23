package com.replog.ui.trainingdna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.Exercise
import com.replog.data.model.TemplateExercise
import com.replog.data.model.WorkoutTemplate
import com.replog.data.repository.BodyweightRepository
import com.replog.data.repository.ExerciseRepository
import com.replog.data.repository.TrainingDNARepository
import com.replog.data.repository.WorkoutRepository
import com.replog.domain.adaptive.AdaptiveSwap
import com.replog.domain.adaptive.AdaptiveTemplateAdvisor
import com.replog.domain.forecast.ProgressionForecast
import com.replog.domain.forecast.ProgressionForecaster
import com.replog.domain.genome.TrainingGenome
import com.replog.domain.genome.TrainingGenomeEngine
import com.replog.domain.musclegap.MuscleGapAnalyzer
import com.replog.domain.musclegap.MuscleGapSuggestion
import com.replog.domain.recovery.RecoveryCalendar
import com.replog.domain.recovery.RecoveryCalendarDay
import com.replog.domain.volume.VolumeLandmark
import com.replog.domain.volume.VolumeLandmarks
import com.replog.domain.recommendation.RecoveryAnalyzer
import com.replog.domain.recovery.RecoveryDashboard
import com.replog.domain.recovery.RecoveryDashboardState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    // Priority 2 (#8) — muscle gap suggestions
    val muscleGaps: List<MuscleGapSuggestion> = emptyList(),
    val volumeLandmarks: List<VolumeLandmark> = emptyList(),
    val recoveryCalendar: List<RecoveryCalendarDay> = emptyList(),
    val genome: TrainingGenome? = null,
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

        // Recovery calendar — green/yellow/red days from training density.
        val calendar = RecoveryCalendar.build(
            completed.map { c -> c.session.startTime to c.exercises.sumOf { e -> e.sets.sumOf { it.weight * it.reps } } },
            now,
            days = 14
        )

        // Training Genome — long-term response patterns (how THIS user grows best).
        val genome = TrainingGenomeEngine.analyze(completed, now)

        if (snapshot == null) {
            return@combine TrainingDnaUiState(recovery = recovery, recoveryCalendar = calendar, genome = genome, hasData = false)
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

        // Muscle gap suggestions (#8) from the weakest muscles DNA already found.
        val weak = snapshot.weakestMuscles.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val gaps = MuscleGapAnalyzer.analyze(weak, exercises)

        // Volume landmarks (#12) — weekly working sets per muscle group vs optimal.
        val landmarks = VolumeLandmarks.analyze(completed, now, weeks = 1)

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
            muscleGaps = gaps,
            volumeLandmarks = landmarks,
            recoveryCalendar = calendar,
            genome = genome,
            hasData = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingDnaUiState())

    /** One-shot status message (e.g. after adding a muscle-gap template). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun clearMessage() { _message.value = null }

    /**
     * Priority 2 (#8) — one-tap "Add to template": create a built-in-style
     * template that targets a weak muscle using its suggested exercises.
     */
    fun addMuscleGapToTemplate(muscle: String, exercises: List<Exercise>) = viewModelScope.launch {
        if (exercises.isEmpty()) {
            _message.value = "No exercises to add."
            return@launch
        }
        val name = "$muscle Focus"
        val templateId = workoutRepository.insertTemplate(
            WorkoutTemplate(name = name, isBuiltIn = false)
        ).toInt()
        exercises.forEachIndexed { index, ex ->
            workoutRepository.insertTemplateExercise(
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = ex.id,
                    defaultSets = 3,
                    orderIndex = index,
                    targetReps = 10
                )
            )
        }
        _message.value = "Added \"$name\" template (${exercises.size} exercises)."
    }

    fun refresh() = viewModelScope.launch {
        trainingDNARepository.generateDNA()
    }
}
