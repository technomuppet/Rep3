package com.replog.ui.intelligence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.data.repository.DnaEvolutionData
import com.replog.data.repository.IntelligenceRepository
import com.replog.data.repository.MuscleBalanceData
import com.replog.data.repository.RecoveryCentreData
import com.replog.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Small presentation ViewModels for the three intelligence screens. Each loads
 * its data once (cached in state) from IntelligenceRepository, which reuses the
 * existing engines. No analysis happens here.
 */

@HiltViewModel
class RecoveryCentreViewModel @Inject constructor(
    private val repo: IntelligenceRepository,
    prefs: PreferencesManager
) : ViewModel() {
    private val _state = MutableStateFlow<RecoveryCentreData?>(null)
    val state: StateFlow<RecoveryCentreData?> = _state.asStateFlow()
    val displayName: StateFlow<String?> = prefs.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun load() = viewModelScope.launch {
        if (_state.value == null) _state.value = runCatching { repo.buildRecoveryCentre() }.getOrNull()
    }
}

@HiltViewModel
class MuscleBalanceViewModel @Inject constructor(
    private val repo: IntelligenceRepository
) : ViewModel() {
    private val _state = MutableStateFlow<MuscleBalanceData?>(null)
    val state: StateFlow<MuscleBalanceData?> = _state.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _dismissed = MutableStateFlow<Set<String>>(emptySet())
    val dismissed: StateFlow<Set<String>> = _dismissed.asStateFlow()

    fun load() = viewModelScope.launch {
        if (_state.value == null) _state.value = runCatching { repo.buildMuscleBalance() }.getOrNull()
    }

    fun startWorkout(muscle: String, onStarted: () -> Unit) = viewModelScope.launch {
        if (runCatching { repo.startMuscleGapWorkout(muscle) }.getOrNull() == true) onStarted()
    }

    fun addToTemplate(muscle: String) = viewModelScope.launch {
        val name = runCatching { repo.addMuscleGapToTemplate(muscle) }.getOrNull()
        _message.update { if (name != null) "Saved \"$name\" to your templates" else "Could not save this workout" }
    }

    fun dismiss(muscle: String) = _dismissed.update { it + muscle }
    fun clearMessage() = _message.update { null }
}

@HiltViewModel
class DnaEvolutionViewModel @Inject constructor(
    private val repo: IntelligenceRepository
) : ViewModel() {
    private val _state = MutableStateFlow<DnaEvolutionData?>(null)
    val state: StateFlow<DnaEvolutionData?> = _state.asStateFlow()
    fun load() = viewModelScope.launch {
        if (_state.value == null) _state.value = runCatching { repo.buildDnaEvolution() }.getOrNull()
    }
}
