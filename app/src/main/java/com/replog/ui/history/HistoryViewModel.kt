package com.replog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.SessionWithExercises
import com.replog.data.repository.WorkoutRepository
import com.replog.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<SessionWithExercises> = emptyList(),
    val isLoading: Boolean = true,
    /** Profile weight is optional; null keeps the history screen honest. */
    val profileWeightKg: Double? = null,
    /** True while more older sessions exist beyond the current window (P2). */
    val canLoadMore: Boolean = false
)

/**
 * Sprint 10 P2: scalable history. Instead of loading the entire history into
 * memory, History renders a bounded window of the most recent completed sessions
 * and lets the user "Load more" to grow the window incrementally. The window is
 * always the most recent sessions, so the current-month calendar/stats are
 * always covered. Smooth regardless of database size.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: WorkoutRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val pageSize = 30
    private val windowLimit = MutableStateFlow(pageSize)

    val uiState = windowLimit
        .flatMapLatest { limit ->
            combine(
                repo.getRecentCompletedSessions(limit),
                repo.getCompletedSessionCountFlow(),
                prefs.userProfile
            ) { sessions, total, profile ->
                HistoryUiState(
                    sessions = sessions,
                    isLoading = false,
                    profileWeightKg = profile?.weightKg,
                    canLoadMore = sessions.size >= limit && limit < total
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun loadMore() {
        windowLimit.value += pageSize
    }

    fun deleteSession(id: Int) = viewModelScope.launch { repo.deleteSessionById(id) }
}
