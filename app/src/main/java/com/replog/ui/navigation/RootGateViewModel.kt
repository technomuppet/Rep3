package com.replog.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.util.PreferencesManager
import com.replog.util.legal.GateState
import com.replog.util.legal.LegalDocuments
import com.replog.util.legal.OnboardingGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RootGateState(
    val gate: GateState? = null,        // null = still loading (avoid flashing onboarding)
    val displayName: String = ""
)

/**
 * Computes the single app-entry gate from persisted profile + legal acceptance.
 * Recomputed on every cold start, so onboarding cannot be bypassed and a legal
 * version bump forces re-acceptance.
 */
@HiltViewModel
class RootGateViewModel @Inject constructor(
    prefs: PreferencesManager
) : ViewModel() {

    val state: StateFlow<RootGateState> =
        combine(prefs.hasProfile, prefs.legalAcceptance, prefs.displayName) { hasProfile, acceptance, name ->
            RootGateState(
                gate = OnboardingGate.evaluate(hasProfile, acceptance, LegalDocuments.currentVersionSignature),
                displayName = name.orEmpty()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootGateState())
}
