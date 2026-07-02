package com.replog.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.domain.templates.EquipmentAccess
import com.replog.domain.templates.GeneratedProgram
import com.replog.domain.templates.ProgramGenerator
import com.replog.domain.templates.ProgramRequest
import com.replog.domain.templates.TrainingGoal
import com.replog.domain.templates.TrainingLevel
import com.replog.domain.templates.WorkoutStyle
import com.replog.util.AppInfo
import com.replog.util.DataSeeder
import com.replog.util.PreferencesManager
import com.replog.util.legal.LegalAcceptance
import com.replog.util.legal.LegalDocuments
import com.replog.util.profile.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Steps in the mandatory onboarding + legal acceptance flow. */
enum class OnboardingStep {
    WELCOME,
    CREATE_PROFILE,
    TRAINING_PREFERENCES,
    DISCLAIMER,
    TERMS,
    PRIVACY,
    FINAL_CONFIRMATION
}

/** Draft of the user profile being collected during onboarding. */
data class ProfileDraft(
    val displayName: String = "",
    val birthYear: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val useKg: Boolean = true,
    val goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val level: TrainingLevel = TrainingLevel.BEGINNER,
    val equipment: EquipmentAccess = EquipmentAccess.FULL_GYM,
    val daysPerWeek: Int = 3,
    val style: WorkoutStyle = WorkoutStyle.NO_PREFERENCE
) {
    val nameValid: Boolean get() = displayName.trim().isNotBlank()
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val draft: ProfileDraft = ProfileDraft(),
    val disclaimerAccepted: Boolean = false,
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    /**
     * When true, an existing user is being asked to re-accept updated legal
     * documents only. The profile already exists, so the flow starts at the
     * disclaimer and the name is pre-filled and read-only.
     */
    val reacceptanceOnly: Boolean = false,
    val finished: Boolean = false
) {
    val allDocsAccepted: Boolean get() = disclaimerAccepted && termsAccepted && privacyAccepted
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val dataSeeder: DataSeeder
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /**
     * Switch the flow into "re-acceptance only" mode for an existing user whose
     * profile is intact but who must accept updated legal documents. Pre-fills the
     * name from the stored profile and jumps to the disclaimer.
     */
    fun startReacceptance(existingName: String) {
        _state.update {
            OnboardingUiState(
                step = OnboardingStep.DISCLAIMER,
                draft = it.draft.copy(displayName = existingName),
                reacceptanceOnly = true
            )
        }
    }

    // --- draft editing ---
    fun setDisplayName(v: String) = _state.update { it.copy(draft = it.draft.copy(displayName = v)) }
    fun setBirthYear(year: Int?) = _state.update { it.copy(draft = it.draft.copy(birthYear = year)) }
    fun setHeight(cm: Double?) = _state.update { it.copy(draft = it.draft.copy(heightCm = cm)) }
    fun setWeight(kg: Double?) = _state.update { it.copy(draft = it.draft.copy(weightKg = kg)) }
    fun setUseKg(v: Boolean) = _state.update { it.copy(draft = it.draft.copy(useKg = v)) }
    fun setGoal(v: TrainingGoal) = _state.update { it.copy(draft = it.draft.copy(goal = v)) }
    fun setLevel(v: TrainingLevel) = _state.update { it.copy(draft = it.draft.copy(level = v)) }
    fun setEquipment(v: EquipmentAccess) = _state.update { it.copy(draft = it.draft.copy(equipment = v)) }
    fun setDays(v: Int) = _state.update { it.copy(draft = it.draft.copy(daysPerWeek = v)) }
    fun setStyle(v: WorkoutStyle) = _state.update { it.copy(draft = it.draft.copy(style = v)) }

    // --- step navigation ---
    fun next() = _state.update { s ->
        val order = OnboardingStep.values()
        val i = order.indexOf(s.step)
        s.copy(step = order.getOrElse(i + 1) { s.step })
    }

    fun back() = _state.update { s ->
        // In re-acceptance mode, never step back before the disclaimer.
        val floor = if (s.reacceptanceOnly) OnboardingStep.DISCLAIMER else OnboardingStep.WELCOME
        val order = OnboardingStep.values()
        val i = order.indexOf(s.step)
        val target = order.getOrElse(i - 1) { floor }
        s.copy(step = if (target.ordinal < floor.ordinal) floor else target)
    }

    fun acceptDisclaimer() = _state.update { it.copy(disclaimerAccepted = true) }
    fun acceptTerms() = _state.update { it.copy(termsAccepted = true) }
    fun acceptPrivacy() = _state.update { it.copy(privacyAccepted = true) }

    /**
     * Declining any document cancels the entire flow: nothing is persisted (no
     * profile is written, no acceptance is recorded) and the user is returned to
     * the start. Home remains unreachable.
     */
    fun declineAndCancel() = _state.update { s ->
        // Declining clears the legal acceptances and cannot grant access to the app,
        // but it NEVER discards the details the user already entered. New users are
        // returned to the Training Preferences step (their profile + preferences are
        // kept); existing users stay on the disclaimer. No profile is written and no
        // acceptance is recorded until they accept all documents and finish.
        s.copy(
            disclaimerAccepted = false,
            termsAccepted = false,
            privacyAccepted = false,
            step = if (s.reacceptanceOnly) OnboardingStep.DISCLAIMER else OnboardingStep.TRAINING_PREFERENCES
        )
    }

    /** The exact display name the final confirmation must match. */
    fun confirmationName(): String = _state.value.draft.displayName.trim()

    /**
     * Final commit. Only callable once all three documents are accepted and the
     * typed name matches. Writes the profile (new users only), records the legal
     * acceptance with the current signature/app version/timestamp, and installs
     * the generated program for new users. Atomic from the user's perspective.
     */
    fun finish(typedName: String) = viewModelScope.launch {
        val s = _state.value
        if (!s.allDocsAccepted) return@launch
        if (typedName != confirmationName()) return@launch

        if (!s.reacceptanceOnly) {
            val d = s.draft
            // Convert the chosen birth year to an epoch-day (1 Jan of that year) only
            // here at commit time, using a real calendar - no lossy round-trip while
            // the user is typing.
            val dobEpochDay = d.birthYear?.takeIf { it in 1900..2100 }?.let {
                java.time.LocalDate.of(it, 1, 1).toEpochDay()
            }
            val profile = UserProfile(
                displayName = d.displayName.trim(),
                dateOfBirthEpochDay = dobEpochDay,
                heightCm = d.heightCm,
                weightKg = d.weightKg,
                useKg = d.useKg,
                experienceLevel = d.level.name,
                primaryGoal = d.goal.name,
                weeklyFrequency = d.daysPerWeek,
                equipment = d.equipment.name
            )
            preferencesManager.setUserProfile(profile)
            val program = ProgramGenerator.generate(
                ProgramRequest(d.goal, d.daysPerWeek, d.equipment, d.level, d.style)
            )
            dataSeeder.installGeneratedProgram(program)
        }

        recordAcceptance(s.draft.displayName.trim())
        // Keep the legacy flag in sync for any code still reading it.
        preferencesManager.setOnboardingComplete(true)
        _state.update { it.copy(finished = true) }
    }

    private suspend fun recordAcceptance(name: String) {
        val now = System.currentTimeMillis()
        preferencesManager.recordLegalAcceptance(
            LegalAcceptance(
                acceptedSignature = LegalDocuments.currentVersionSignature,
                disclaimerVersion = LegalDocuments.disclaimer.version,
                termsVersion = LegalDocuments.terms.version,
                privacyVersion = LegalDocuments.privacy.version,
                acceptedAtEpochMillis = now,
                appVersion = AppInfo.versionName,
                displayName = name,
                completed = true
            )
        )
    }

    /** Live program preview for the training-preferences step. */
    fun preview(draft: ProfileDraft): GeneratedProgram =
        ProgramGenerator.generate(ProgramRequest(draft.goal, draft.daysPerWeek, draft.equipment, draft.level, draft.style))
}
