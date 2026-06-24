package com.replog.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.RecommendationHistory
import com.replog.data.repository.BodyweightRepository
import com.replog.data.repository.GoalRepository
import com.replog.data.repository.RecommendationRepository
import com.replog.data.repository.TrainingDNARepository
import com.replog.data.repository.WorkoutRepository
import com.replog.domain.coachdash.CoachBriefing
import com.replog.domain.coachdash.CoachBriefingBuilder
import com.replog.domain.recommendation.ExplanationContribution
import com.replog.domain.recommendation.Recommendation
import com.replog.domain.recommendation.RecommendationExplanation
import com.replog.domain.recommendation.RecommendationType
import com.replog.domain.recommendation.RecoveryAnalyzer
import com.replog.domain.recovery.RecoveryDashboard
import com.replog.util.PreferencesManager
import java.util.Calendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class CoachUiState(
    val isLoading: Boolean = true,
    val recommendation: Recommendation? = null,
    val contributions: List<ExplanationContribution> = emptyList(),
    val briefing: CoachBriefing? = null,
    val accepted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val workoutRepository: WorkoutRepository,
    private val trainingDNARepository: TrainingDNARepository,
    private val bodyweightRepository: BodyweightRepository,
    private val goalRepository: GoalRepository,
    private val handoff: CoachHandoff,
    private val prefs: PreferencesManager,
    private val restDayOverrideRepository: com.replog.data.repository.RestDayOverrideRepository
) : ViewModel() {

    /**
     * P5: the user chose "Train Anyway" against a recommended rest day. Record
     * the override locally (recovery score + reason) so future recovery
     * intelligence can learn the user's true tolerance for training through
     * fatigue. Also marks the rest recommendation as dismissed.
     */
    fun recordTrainAnywayOverride() {
        val briefing = _state.value.briefing
        viewModelScope.launch {
            restDayOverrideRepository.record(
                recoveryScore = briefing?.recoveryScore,
                recommendationReason = briefing?.recoveryDirective
            )
        }
        dismissRecommendation(reason = "train_anyway")
    }

    private val _state = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    /** Phase 4 — Coach History feed, straight from the RecommendationHistory table. */
    val history: StateFlow<List<RecommendationHistory>> =
        recommendationRepository.getRecommendationHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Accepted vs rejected coaching counts for the history header. */
    val successRate: StateFlow<Pair<Int, Int>> =
        recommendationRepository.getRecommendationHistory()
            .map { list ->
                val accepted = list.count { it.outcome == "Accepted" || it.outcome == "Completed" }
                val rejected = list.count { it.outcome == "Rejected" }
                accepted to rejected
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    init { loadRecommendation(force = false) }

    /**
     * Phase 1 — "Recompute only when necessary".
     * Regenerates only when the day has changed OR a new workout has been
     * completed since the last generation. Otherwise reuses the most recent
     * generated recommendation held in memory.
     */
    fun loadRecommendation(force: Boolean) = viewModelScope.launch {
        if (!force && _state.value.recommendation != null && !shouldRecompute()) return@launch
        _state.value = _state.value.copy(isLoading = true, error = null)
        runCatching {
            val recommendation = recommendationRepository.generateRecommendationAndEnsureDNA()
            markGenerated()
            recommendation
        }.onSuccess { rec ->
            _state.value = CoachUiState(
                isLoading = false,
                recommendation = rec,
                contributions = RecommendationExplanation.contributions(rec),
                briefing = buildBriefing(rec),
                accepted = false
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Could not build a recommendation.")
        }
    }

    /** Assemble the unified Coach Dashboard briefing from existing intelligence. */
    private suspend fun buildBriefing(rec: Recommendation): CoachBriefing {
        val useKg = prefs.useKg.first()
        val now = System.currentTimeMillis()

        // Recovery (reuse the existing analyzer + dashboard mapping).
        val recoveryState = runCatching {
            val sessions = workoutRepository.getAllSessions().first().filter { it.session.endTime != null }
            val bodyweights = bodyweightRepository.getAllBodyweights().first()
            RecoveryDashboard.from(RecoveryAnalyzer.overallRecovery(sessions, bodyweights, now))
        }.getOrNull()

        // Weakest muscle from the latest DNA snapshot (best-effort).
        val weakest = runCatching {
            trainingDNARepository.getLatestDNA().first()
                ?.weakestMuscles?.split(",")?.map { it.trim() }?.firstOrNull { it.isNotBlank() }
        }.getOrNull()

        // Top active goal forecast (best-effort).
        val goalLine = runCatching {
            goalRepository.getActive().first().firstOrNull()?.let { goal ->
                goalRepository.forecastFor(goal, useKg).summaryLine
            }
        }.getOrNull()

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return CoachBriefingBuilder.build(
            recommendation = rec,
            recoveryScore = recoveryState?.score,
            recoveryDirective = recoveryState?.directive,
            weakestMuscle = weakest,
            topGoalSummary = goalLine,
            useKg = useKg,
            hourOfDay = hour
        )
    }

    private suspend fun shouldRecompute(): Boolean {
        val today = todayEpochDay()
        val lastDay = prefs.coachLastGenDay.first()
        val lastCount = prefs.coachLastGenSessionCount.first()
        val currentCount = workoutRepository.getCompletedSessionCount()
        return today != lastDay || currentCount != lastCount
    }

    private suspend fun markGenerated() {
        prefs.setCoachGenerationMarker(todayEpochDay(), workoutRepository.getCompletedSessionCount())
    }

    private fun todayEpochDay(): Long =
        TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())

    /**
     * Phase 3 — Smart Start. Persists acceptance, stages the recommendation for
     * the workout screen, and signals whether a workout should be launched.
     * REST recommendations are accepted but launch nothing.
     */
    fun acceptRecommendation(onLaunchWorkout: () -> Unit, onRestDay: () -> Unit) {
        val rec = _state.value.recommendation ?: return
        viewModelScope.launch {
            val isRest = rec.type == RecommendationType.REST
            recommendationRepository.recordAcceptance(rec, completed = false)
            _state.value = _state.value.copy(accepted = true)
            if (isRest || rec.workoutPlan == null) {
                onRestDay()
            } else {
                handoff.set(rec)
                onLaunchWorkout()
            }
        }
    }

    fun dismissRecommendation(reason: String? = null) {
        val rec = _state.value.recommendation ?: return
        viewModelScope.launch {
            recommendationRepository.recordRejection(rec, reason)
            // Refresh to offer the next-best guidance.
            loadRecommendation(force = true)
        }
    }

    fun clearOldHistory() = viewModelScope.launch {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365)
        recommendationRepository.clearOldHistory(cutoff)
    }
}
