package com.replog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeInsight(
    val title: String = "Start your training signal",
    val message: String = "Log a few workouts and RepLog will surface recovery, plateau and progression insights here."
)

data class HomeUiState(
    val sessionCount: Int = 0,
    val totalVolume: Double = 0.0,
    val recentSessions: List<SessionWithExercises> = emptyList(),
    val recentPRs: List<SetLog> = emptyList(),
    val insight: HomeInsight = HomeInsight(),
    val sessionsThisWeek: Int = 0,
    val volumeThisWeek: Double = 0.0,
    val dayStreak: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val repo: WorkoutRepository) : ViewModel() {
    private val stats = MutableStateFlow(0 to 0.0)

    val uiState: StateFlow<HomeUiState> = combine(
        stats,
        repo.getRecentSessions(3),
        repo.getRecentPRs(),
        repo.getAllSessions()
    ) { s, sessions, prs, allSessions ->
        val completed = allSessions.filter { it.session.endTime != null }
        val now = System.currentTimeMillis()
        val startTimes = completed.map { it.session.startTime }
        val startTimesToVolume = completed.map { c ->
            c.session.startTime to c.exercises.sumOf { e -> e.sets.sumOf { it.weight * it.reps } }
        }
        val weekly = com.replog.domain.home.HomeDashboardStats.weeklyProgress(startTimesToVolume, now)
        HomeUiState(
            sessionCount = s.first,
            totalVolume = s.second,
            recentSessions = sessions,
            recentPRs = prs,
            insight = buildHomeInsight(completed),
            sessionsThisWeek = weekly.sessionsThisWeek,
            volumeThisWeek = weekly.volumeThisWeek,
            dayStreak = com.replog.domain.home.HomeDashboardStats.dayStreak(startTimes, now),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        stats.value = repo.getCompletedSessionCount() to repo.getTotalVolume()
    }

    private fun buildHomeInsight(sessions: List<SessionWithExercises>): HomeInsight {
        if (sessions.size < 3) return HomeInsight()
        val latest = sessions.maxByOrNull { it.session.startTime } ?: return HomeInsight()
        val latestVolume = latest.exercises.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } }
        val avgRecentVolume = sessions.sortedByDescending { it.session.startTime }
            .take(4)
            .map { session -> session.exercises.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } } }
            .average()
        val prCount = latest.exercises.sumOf { entry -> entry.sets.count { it.isPR } }
        return when {
            prCount > 0 -> HomeInsight("PR momentum", "Your last workout produced $prCount PR${if (prCount == 1) "" else "s"}. Keep the next session focused and repeatable.")
            latestVolume > avgRecentVolume * 1.25 -> HomeInsight("Volume jump detected", "Your last session was much higher volume than recent average. Watch recovery before adding more work.")
            latestVolume < avgRecentVolume * 0.7 -> HomeInsight("Lower-volume session", "Your last session was lighter than usual. Good if intentional; if not, check fatigue or schedule pressure.")
            else -> HomeInsight("Training looks steady", "Recent workload is consistent. Keep progressing one variable at a time: reps, load, sets or technique quality.")
        }
    }
}
