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
import com.replog.util.DataSeeder
import com.replog.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val onboardingComplete: Boolean = false
)

/** The personalization answers collected during onboarding. */
data class OnboardingAnswers(
    val useKg: Boolean = true,
    val goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val level: TrainingLevel = TrainingLevel.BEGINNER,
    val equipment: EquipmentAccess = EquipmentAccess.FULL_GYM,
    val daysPerWeek: Int = 3,
    val style: WorkoutStyle = WorkoutStyle.NO_PREFERENCE
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val dataSeeder: DataSeeder
) : ViewModel() {
    val uiState: StateFlow<OnboardingUiState> = preferencesManager.onboardingComplete
        .map { OnboardingUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    /** Preview the program that the current answers would generate (for the summary step). */
    fun preview(answers: OnboardingAnswers): GeneratedProgram =
        ProgramGenerator.generate(answers.toRequest())

    /**
     * Persist units + training profile, generate a personalized program from the
     * existing offline rules engine, install its templates, then complete onboarding.
     */
    fun finishWithPersonalization(answers: OnboardingAnswers) = viewModelScope.launch {
        preferencesManager.setUseKg(answers.useKg)
        preferencesManager.setTrainingProfile(
            goal = answers.goal.name,
            level = answers.level.name,
            equipment = answers.equipment.name,
            daysPerWeek = answers.daysPerWeek,
            style = answers.style.name
        )
        val program = ProgramGenerator.generate(answers.toRequest())
        dataSeeder.installGeneratedProgram(program)
        preferencesManager.setOnboardingComplete(true)
    }

    /** Backwards-compatible quick finish (units only, no personalization). */
    fun finish(useKg: Boolean) = viewModelScope.launch {
        preferencesManager.setUseKg(useKg)
        preferencesManager.setOnboardingComplete(true)
    }

    private fun OnboardingAnswers.toRequest() = ProgramRequest(
        goal = goal,
        daysPerWeek = daysPerWeek,
        equipment = equipment,
        level = level,
        style = style
    )
}
