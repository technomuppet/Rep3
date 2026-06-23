package com.replog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.model.SessionWithExercises
import com.replog.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(val sessions: List<SessionWithExercises> = emptyList(), val isLoading: Boolean = true)
@HiltViewModel
class HistoryViewModel @Inject constructor(private val repo: WorkoutRepository) : ViewModel() {
    val uiState = repo.getAllSessions().map { HistoryUiState(it, isLoading = false) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
    fun deleteSession(id: Int) = viewModelScope.launch { repo.deleteSessionById(id) }
}
