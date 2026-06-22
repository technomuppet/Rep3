package com.replog.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.RecommendationHistory
import com.replog.data.repository.RecommendationRepository
import com.replog.data.repository.WorkoutRepository
import com.replog.domain.recommendation.ExplanationContribution
import com.replog.domain.recommendation.Recommendation
import com.replog.domain.recommendation.RecommendationExplanation
import com.replog.domain.recommendation.RecommendationType
import com.replog.util.PreferencesManager
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
    val accepted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val workoutRepository: WorkoutRepository,
    private val handoff: CoachHandoff,
    private val prefs: PreferencesManager
) : ViewModel() {

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
                accepted = false
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Could not build a recommendation.")
        }
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
