package com.replog.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.templates.EquipmentAccess
import com.replog.domain.templates.ProgramGenerator
import com.replog.domain.templates.ProgramRequest
import com.replog.domain.templates.TrainingGoal
import com.replog.domain.templates.TrainingLevel
import com.replog.domain.templates.WorkoutStyle
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton

@Composable
fun OnboardingScreen(
    onComplete: (useKg: Boolean) -> Unit,
    onPersonalize: (OnboardingAnswers) -> Unit = {}
) {
    var goal by remember { mutableStateOf(TrainingGoal.HYPERTROPHY) }
    var level by remember { mutableStateOf(TrainingLevel.BEGINNER) }
    var equipment by remember { mutableStateOf(EquipmentAccess.FULL_GYM) }
    var days by remember { mutableStateOf(3) }
    var style by remember { mutableStateOf(WorkoutStyle.NO_PREFERENCE) }
    var useKg by remember { mutableStateOf(true) }

    val answers = OnboardingAnswers(useKg, goal, level, equipment, days, style)
    val preview = remember(goal, level, equipment, days, style) {
        ProgramGenerator.generate(ProgramRequest(goal, days, equipment, level, style))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Welcome to RepLog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Answer a few questions and we'll build your starting program — fully offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Question("What's your main goal?") {
                Choice("Build muscle", goal == TrainingGoal.HYPERTROPHY) { goal = TrainingGoal.HYPERTROPHY }
                Choice("Get stronger", goal == TrainingGoal.STRENGTH) { goal = TrainingGoal.STRENGTH }
                Choice("Lose fat", goal == TrainingGoal.FAT_LOSS) { goal = TrainingGoal.FAT_LOSS }
                Choice("General fitness", goal == TrainingGoal.GENERAL) { goal = TrainingGoal.GENERAL }
            }
        }

        item {
            Question("How long have you trained?") {
                Choice("New (< 1 yr)", level == TrainingLevel.BEGINNER) { level = TrainingLevel.BEGINNER }
                Choice("Intermediate (1–3 yr)", level == TrainingLevel.INTERMEDIATE) { level = TrainingLevel.INTERMEDIATE }
                Choice("Advanced (3+ yr)", level == TrainingLevel.ADVANCED) { level = TrainingLevel.ADVANCED }
            }
        }

        item {
            Question("What equipment do you have?") {
                Choice("Full gym", equipment == EquipmentAccess.FULL_GYM) { equipment = EquipmentAccess.FULL_GYM }
                Choice("Dumbbells only", equipment == EquipmentAccess.DUMBBELLS_ONLY) { equipment = EquipmentAccess.DUMBBELLS_ONLY }
                Choice("Bodyweight only", equipment == EquipmentAccess.BODYWEIGHT_ONLY) { equipment = EquipmentAccess.BODYWEIGHT_ONLY }
            }
        }

        item {
            Question("How many days per week?") {
                (2..6).forEach { d -> Choice("$d days", days == d) { days = d } }
            }
        }

        item {
            Question("Preferred workout style?") {
                Choice("No preference", style == WorkoutStyle.NO_PREFERENCE) { style = WorkoutStyle.NO_PREFERENCE }
                Choice("Full body", style == WorkoutStyle.FULL_BODY) { style = WorkoutStyle.FULL_BODY }
                Choice("Upper / Lower", style == WorkoutStyle.UPPER_LOWER) { style = WorkoutStyle.UPPER_LOWER }
                Choice("Push / Pull / Legs", style == WorkoutStyle.PUSH_PULL_LEGS) { style = WorkoutStyle.PUSH_PULL_LEGS }
            }
        }

        item {
            Question("Preferred units") {
                Choice("Kilograms", useKg) { useKg = true }
                Choice("Pounds", !useKg) { useKg = false }
            }
        }

        item {
            RepLogCard {
                Text("Your starting program", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(preview.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text(preview.rationale, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                preview.templateNames.distinct().forEach { name ->
                    Text("• $name", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            PrimaryButton("Create my program") { onPersonalize(answers) }
        }
        item {
            SecondaryButton("Skip — just set up logging") { onComplete(useKg) }
        }
    }
}

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

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
