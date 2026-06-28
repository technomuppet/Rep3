package com.replog.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.replog.domain.templates.EquipmentAccess
import com.replog.domain.templates.TrainingGoal
import com.replog.domain.templates.TrainingLevel
import com.replog.domain.templates.WorkoutStyle
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton
import com.replog.util.legal.LegalDocId
import com.replog.util.legal.LegalDocuments

/**
 * The mandatory onboarding + legal acceptance flow. Drives a single-screen step
 * machine: Welcome -> Create Profile -> Training Preferences -> Disclaimer ->
 * Terms -> Privacy -> Final Confirmation. Home is never reachable until Finish.
 */
@Composable
fun OnboardingFlow(viewModel: OnboardingViewModel) {
    val state by viewModel.state.collectAsState()

    when (state.step) {
        OnboardingStep.WELCOME -> WelcomeStep(onContinue = viewModel::next)
        OnboardingStep.CREATE_PROFILE -> CreateProfileStep(state, viewModel)
        OnboardingStep.TRAINING_PREFERENCES -> TrainingPreferencesStep(state, viewModel)
        OnboardingStep.DISCLAIMER -> LegalStep(
            doc = LegalDocuments.disclaimer,
            accepted = state.disclaimerAccepted,
            showEmergency = true,
            canGoBack = !state.reacceptanceOnly,
            onAccept = viewModel::acceptDisclaimer,
            onContinue = viewModel::next,
            onDecline = viewModel::declineAndCancel,
            onBack = viewModel::back
        )
        OnboardingStep.TERMS -> LegalStep(
            doc = LegalDocuments.terms,
            accepted = state.termsAccepted,
            showEmergency = false,
            canGoBack = true,
            onAccept = viewModel::acceptTerms,
            onContinue = viewModel::next,
            onDecline = viewModel::declineAndCancel,
            onBack = viewModel::back
        )
        OnboardingStep.PRIVACY -> LegalStep(
            doc = LegalDocuments.privacy,
            accepted = state.privacyAccepted,
            showEmergency = false,
            canGoBack = true,
            onAccept = viewModel::acceptPrivacy,
            onContinue = viewModel::next,
            onDecline = viewModel::declineAndCancel,
            onBack = viewModel::back
        )
        OnboardingStep.FINAL_CONFIRMATION -> FinalConfirmationStep(
            expectedName = viewModel.confirmationName(),
            allAccepted = state.allDocsAccepted,
            onFinish = viewModel::finish,
            onBack = viewModel::back
        )
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Welcome to RepLog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "RepLog is a private, offline-first strength-training app. Before you start, we will set up your profile and ask you to review a few important documents.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            RepLogCard {
                Text("What to expect", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("1. Create your profile", style = MaterialTheme.typography.bodyMedium)
                Text("2. Set your training preferences", style = MaterialTheme.typography.bodyMedium)
                Text("3. Review the Health & Safety Disclaimer, Terms of Use and Privacy Policy", style = MaterialTheme.typography.bodyMedium)
                Text("4. Confirm and start training", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { PrimaryButton("Get started") { onContinue() } }
    }
}

@Composable
private fun CreateProfileStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    val d = state.draft
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Create your profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Your display name personalises the app. Everything else is optional.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            RepLogCard {
                Text("Display name", fontWeight = FontWeight.Bold)
                Text("Required", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = d.displayName,
                    onValueChange = vm::setDisplayName,
                    singleLine = true,
                    placeholder = { Text("e.g. David") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            RepLogCard {
                Text("Optional details", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OptionalNumberField("Height (cm)", d.heightCm) { vm.setHeight(it) }
                Spacer(Modifier.height(8.dp))
                OptionalNumberField(if (d.useKg) "Weight (kg)" else "Weight (lb)", d.weightKg) { vm.setWeight(it) }
                Spacer(Modifier.height(8.dp))
                OptionalNumberField("Year of birth", d.dateOfBirthEpochDay?.let { (it / 365.2425 + 1970).toInt().toDouble() }) { year ->
                    // Store Jan 1 of the chosen year as an epoch-day approximation.
                    vm.setDob(year?.let { ((it.toInt() - 1970) * 365.2425).toLong() })
                }
                Text("Age is calculated from your year of birth and never stored directly.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            RepLogCard {
                Text("Units", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ChoiceRow {
                    Choice("Kilograms", d.useKg) { vm.setUseKg(true) }
                    Choice("Pounds", !d.useKg) { vm.setUseKg(false) }
                }
            }
        }
        item {
            PrimaryButton("Continue", enabled = d.nameValid) { vm.next() }
            if (!d.nameValid) {
                Spacer(Modifier.height(6.dp))
                Text("Enter a display name to continue.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
        }
        item { SecondaryButton("Back") { vm.back() } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainingPreferencesStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    val d = state.draft
    val preview = remember(d.goal, d.level, d.equipment, d.daysPerWeek, d.style) { vm.preview(d) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Training preferences", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold) }
        item {
            Question("What's your main goal?") {
                Choice("Build muscle", d.goal == TrainingGoal.HYPERTROPHY) { vm.setGoal(TrainingGoal.HYPERTROPHY) }
                Choice("Get stronger", d.goal == TrainingGoal.STRENGTH) { vm.setGoal(TrainingGoal.STRENGTH) }
                Choice("Lose fat", d.goal == TrainingGoal.FAT_LOSS) { vm.setGoal(TrainingGoal.FAT_LOSS) }
                Choice("General fitness", d.goal == TrainingGoal.GENERAL) { vm.setGoal(TrainingGoal.GENERAL) }
            }
        }
        item {
            Question("How long have you trained?") {
                Choice("New (< 1 yr)", d.level == TrainingLevel.BEGINNER) { vm.setLevel(TrainingLevel.BEGINNER) }
                Choice("Intermediate", d.level == TrainingLevel.INTERMEDIATE) { vm.setLevel(TrainingLevel.INTERMEDIATE) }
                Choice("Advanced", d.level == TrainingLevel.ADVANCED) { vm.setLevel(TrainingLevel.ADVANCED) }
            }
        }
        item {
            Question("What equipment do you have?") {
                Choice("Full gym", d.equipment == EquipmentAccess.FULL_GYM) { vm.setEquipment(EquipmentAccess.FULL_GYM) }
                Choice("Dumbbells only", d.equipment == EquipmentAccess.DUMBBELLS_ONLY) { vm.setEquipment(EquipmentAccess.DUMBBELLS_ONLY) }
                Choice("Bodyweight only", d.equipment == EquipmentAccess.BODYWEIGHT_ONLY) { vm.setEquipment(EquipmentAccess.BODYWEIGHT_ONLY) }
            }
        }
        item {
            Question("How many days per week?") {
                (2..6).forEach { day -> Choice("$day days", d.daysPerWeek == day) { vm.setDays(day) } }
            }
        }
        item {
            Question("Preferred workout style?") {
                Choice("No preference", d.style == WorkoutStyle.NO_PREFERENCE) { vm.setStyle(WorkoutStyle.NO_PREFERENCE) }
                Choice("Full body", d.style == WorkoutStyle.FULL_BODY) { vm.setStyle(WorkoutStyle.FULL_BODY) }
                Choice("Upper / Lower", d.style == WorkoutStyle.UPPER_LOWER) { vm.setStyle(WorkoutStyle.UPPER_LOWER) }
                Choice("Push / Pull / Legs", d.style == WorkoutStyle.PUSH_PULL_LEGS) { vm.setStyle(WorkoutStyle.PUSH_PULL_LEGS) }
            }
        }
        item {
            RepLogCard {
                Text("Your starting program", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(preview.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text(preview.rationale, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { PrimaryButton("Continue") { vm.next() } }
        item { SecondaryButton("Back") { vm.back() } }
    }
}

/**
 * A single legal document step: full text that must be scrolled to the bottom
 * before the confirmation checkbox is enabled; the checkbox must be ticked before
 * Continue is enabled. Decline cancels the whole flow.
 */
@Composable
private fun LegalStep(
    doc: com.replog.util.legal.LegalDocument,
    accepted: Boolean,
    showEmergency: Boolean,
    canGoBack: Boolean,
    onAccept: () -> Unit,
    onContinue: () -> Unit,
    onDecline: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    // Scrolled to the bottom when the last item is visible.
    val reachedBottom by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()
            last != null && last.index >= layout.totalItemsCount - 1
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(doc.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                "Version ${doc.version}  -  Effective ${doc.effectiveDate}  -  Updated ${doc.lastUpdated}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            RepLogCard {
                Text(doc.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (showEmergency) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Emergency warning", fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(6.dp))
                        Text(LegalDocuments.EMERGENCY_WARNING, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = { if (reachedBottom) onAccept() },
                    enabled = reachedBottom
                )
                Text(
                    if (reachedBottom) "I have read and accept the ${doc.title}."
                    else "Scroll to the bottom to enable acceptance.",
                    color = if (reachedBottom) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            PrimaryButton("Continue", enabled = accepted) { onContinue() }
        }
        item {
            SecondaryButton("Decline") { onDecline() }
        }
        if (canGoBack) {
            item { SecondaryButton("Back") { onBack() } }
        }
    }
}

@Composable
private fun FinalConfirmationStep(
    expectedName: String,
    allAccepted: Boolean,
    onFinish: (String) -> Unit,
    onBack: () -> Unit
) {
    var typed by remember { mutableStateOf("") }
    val matches = typed == expectedName && expectedName.isNotBlank() && allAccepted

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Final confirmation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                "You have reviewed the Health & Safety Disclaimer, Terms of Use and Privacy Policy. To confirm, type your display name exactly as shown below.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            RepLogCard {
                Text("Type to confirm", fontWeight = FontWeight.Bold)
                Text(expectedName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    placeholder = { Text("Type your display name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            PrimaryButton("Finish", enabled = matches) { onFinish(typed) }
            if (!matches) {
                Spacer(Modifier.height(6.dp))
                Text("Type your display name exactly to enable Finish.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
        item { SecondaryButton("Back") { onBack() } }
    }
}

// --- small shared pieces ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Question(title: String, content: @Composable () -> Unit) = RepLogCard {
    Text(title, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(content: @Composable () -> Unit) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth()
) { content() }

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun OptionalNumberField(label: String, value: Double?, onChange: (Double?) -> Unit) {
    var text by remember(value) { mutableStateOf(value?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it.trim().toDoubleOrNull())
        },
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
