package com.replog.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.domain.library.QuickWorkout
import com.replog.domain.library.WorkoutCategory
import com.replog.domain.library.WorkoutEquipment
import com.replog.domain.library.WorkoutGoal
import com.replog.ui.components.InlineEmpty
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickWorkoutsScreen(
    contentPadding: PaddingValues,
    onWorkoutStarted: () -> Unit,
    viewModel: QuickWorkoutsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Quick Workouts", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Start a proven workout in one tap, or save it to your templates.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        state.message?.let { msg ->
            item {
                RepLogCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, modifier = Modifier.weight(1f))
                        SecondaryButton("OK", Modifier.width(90.dp)) { viewModel.clearMessage() }
                    }
                }
            }
        }

        item {
            ChipGroup("Category", WorkoutCategory.entries.map { it.label }, state.selectedCategory?.label) { label ->
                viewModel.toggleCategory(WorkoutCategory.entries.first { it.label == label })
            }
        }
        item {
            ChipGroup("Goal", WorkoutGoal.entries.map { it.label }, state.selectedGoal?.label) { label ->
                viewModel.toggleGoal(WorkoutGoal.entries.first { it.label == label })
            }
        }
        item {
            ChipGroup("Equipment", WorkoutEquipment.entries.map { it.label }, state.selectedEquipment?.label) { label ->
                viewModel.toggleEquipment(WorkoutEquipment.entries.first { it.label == label })
            }
        }
        item {
            ChipGroup("Max duration", state.durationOptions.map { "$it min" }, state.maxMinutes?.let { "$it min" }) { label ->
                viewModel.toggleDuration(label.removeSuffix(" min").trim().toInt())
            }
        }

        item {
            Text("${state.results.size} workouts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.results.isEmpty()) {
            item { InlineEmpty("No workouts match those filters. Try removing one.") }
        } else {
            items(state.results, key = { it.id }) { workout ->
                QuickWorkoutCard(
                    workout = workout,
                    onStart = { viewModel.startWorkout(workout); onWorkoutStarted() },
                    onSave = { viewModel.saveToTemplates(workout) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(title: String, options: List<String>, selected: String?, onToggle: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onToggle(option) }, label = { Text(option) })
        }
    }
}

@Composable
private fun QuickWorkoutCard(workout: QuickWorkout, onStart: () -> Unit, onSave: () -> Unit) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(Icons.Default.Schedule, null, Modifier.width(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text("${workout.estimatedMinutes} min", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    workout.programmeName?.let {
        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(4.dp))
    Text(workout.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Text(
        workout.exercises.joinToString("  •  ") { "${it.exerciseName} ${it.sets}×${it.reps}" },
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(6.dp))
    Text("Progression: ${workout.progressionNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton("Start Workout", Modifier.weight(1.3f), onClick = onStart)
        SecondaryButton("Save", Modifier.weight(0.7f), onClick = onSave)
    }
}
