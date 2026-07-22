package com.replog.ui.exercise

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.data.model.Exercise
import com.replog.data.model.ExerciseInsight
import com.replog.data.model.ExerciseSetHistory
import com.replog.ui.components.EmptyState
import com.replog.ui.components.ExerciseIcon
import com.replog.ui.components.InlineEmpty
import com.replog.ui.components.LoadingState
import com.replog.ui.components.PRBadge
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.StatCard
import com.replog.ui.components.formatWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryScreen(
    contentPadding: PaddingValues,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedInsight by viewModel.selectedInsight.collectAsState()
    val swaps by viewModel.swapSuggestions.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Exercise Library", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                Text("Browse movements, inspect progress, and add your own.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Search exercises") },
                    singleLine = true
                )
            }

            item {
                FilterSection(
                    state = state,
                    onToggleMuscle = viewModel::toggleMuscle,
                    onToggleEquipment = viewModel::toggleEquipment,
                    onToggleDifficulty = viewModel::toggleDifficulty,
                    onTogglePattern = viewModel::togglePattern,
                    onToggleGoal = viewModel::toggleGoal,
                    onToggleExperience = viewModel::toggleExperience,
                    onToggleEquipmentPreset = viewModel::toggleEquipmentPreset,
                    onClear = viewModel::clearFilters
                )
            }

            if (state.isLoading && state.exercises.isEmpty() && state.filter.isEmpty) {
                item { LoadingState("Loading exercises") }
            } else if (state.exercises.isEmpty()) {
                item { EmptyState("No exercises found", "Try changing your search or add a custom exercise.") }
            } else {
                items(state.exercises, key = { it.id }) { exercise ->
                    ExerciseItem(
                        exercise = exercise,
                        onClick = { viewModel.selectExercise(exercise) },
                        onDelete = { pendingDelete = exercise }
                    )
                }
            }
        }
    }

    pendingDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete custom exercise?") },
            text = { Text("This deletes ${exercise.name}. Existing workouts may still reference this movement and deletion can fail if it is in use.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExercise(exercise)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }

    if (showAdd) {
        AddExerciseDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, category, equipment, muscles ->
                viewModel.addCustomExercise(name, category, equipment, muscles)
                showAdd = false
            }
        )
    }

    val coaching by viewModel.selectedCoaching.collectAsState()
    selectedInsight?.let { insight ->
        ExerciseDetailDialog(
            insight = insight,
            useKg = state.useKg,
            swaps = swaps,
            coaching = coaching,
            onDismiss = viewModel::clearSelectedExercise
        )
    }
}

@Composable
private fun ExerciseItem(
    exercise: Exercise,
    onClick: () -> Unit,
    onDelete: () -> Unit
) = RepLogCard(onClick = onClick) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExerciseIcon()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(exercise.name, fontWeight = FontWeight.Bold)
            Text(
                "${exercise.category} • ${exercise.equipment}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (exercise.movementPattern.isNotBlank()) {
                Text(
                    exercise.movementPattern,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (exercise.primaryMuscles.isNotBlank()) {
                Text(
                    "Primary: ${exercise.primaryMuscles}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (exercise.secondaryMuscles.isNotBlank()) {
                Text(
                    "Secondary: ${exercise.secondaryMuscles}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (exercise.primaryMuscles.isBlank() && exercise.muscles.isNotBlank()) {
                Text(
                    exercise.muscles,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (exercise.isCustom) {
                Text("Custom", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        }
        Icon(Icons.Default.TrendingUp, contentDescription = "Progress", tint = MaterialTheme.colorScheme.primary)
        if (exercise.isCustom) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ExerciseDetailDialog(
    insight: ExerciseInsight,
    useKg: Boolean,
    swaps: List<com.replog.domain.swap.ExerciseSwap>,
    coaching: ExerciseCoaching?,
    onDismiss: () -> Unit
) {
    val scroll = rememberScrollState()
    val ex = insight.exercise
    val anatomySpec = remember(ex) {
        com.replog.domain.visual.resolver.ExerciseVisualResolver.resolve(ex).anatomy
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(ex.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${ex.equipment} \u2022 ${ex.difficulty}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (ex.primaryMuscles.isNotBlank()) {
                    Text("Primary: ${ex.primaryMuscles}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (ex.secondaryMuscles.isNotBlank()) {
                    Text("Secondary: ${ex.secondaryMuscles}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 640.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. EXERCISE HEADER — Premium educational layout
                RepLogCard {
                    Text("EXERCISE HEADER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.padding(end = 12.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Text(" ${ex.movementPattern.ifBlank { "No pattern" }} ", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)) {
                            Text(" ${ex.category.ifBlank { "No category" }} ", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Equipment", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ex.equipment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Difficulty", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ex.difficulty, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // 2. HOW TO PERFORM — Always visible, never collapsible, above coaching cues
                RepLogCard {
                    Text("HOW TO PERFORM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    coaching?.let { c ->
                        Text("Starting position", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.steps.firstOrNull() ?: "Prepare posture, brace core, and stand stable.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Step-by-step execution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Breathing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.breathing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Tempo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("${c.coaching.tempo} — ${c.coaching.tempoExplanation}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Range of motion", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.rangeOfMotion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Finish position", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.steps.getOrNull(2) ?: "Pause briefly at peak contraction to maximize tension.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Reset position", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Return to the starting posture with control, maintaining bracing and posture.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } ?: run {
                        Text("Move the weight smoothly through its full range of motion under control, squeezing the target muscle.", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // 3. MEDICAL MUSCLE ACTIVATION DIAGRAM — Data-driven, no stick figure, no block fill
                RepLogCard {
                    Text("MUSCLE ACTIVATION DIAGRAM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("Medical-grade vector anatomy showing primary, secondary, and stabiliser activation for this movement.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    com.replog.domain.visual.anatomy.AnatomicalMuscleDiagram(
                        anatomySpec = anatomySpec,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. COACHING CUES — Premium redesign (Setup, Execution, Lockout, Breathing, Bracing, Grip, Foot Position)
                coaching?.let { c ->
                    CoachingCueSections(coaching = c.coaching)
                }

                // 5. COMMON MISTAKES — Problem / Why it matters / How to correct (already in CoachingCueSections with Common Errors)
                // 6. EQUIPMENT — Primary, Alternative, Machine equivalent, Home gym alternative, Resistance band alternative
                RepLogCard {
                    Text("EQUIPMENT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Primary", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ex.equipment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Alternative", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(alternativeEquipmentFor(ex), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Machine equivalent", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(machineEquivalentFor(ex), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Home / Band", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(homeAlternativeFor(ex) + " / " + bandAlternativeFor(ex), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // 7. EXERCISE INFORMATION — Premium cards
                RepLogCard {
                    Text("EXERCISE INFORMATION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Movement pattern", ex.movementPattern)
                    InfoRow("Joint actions", jointActionsFor(ex))
                    InfoRow("Plane of movement", planeOfMovementFor(ex))
                    InfoRow("Primary joints", primaryJointsFor(ex))
                    InfoRow("Stabilising joints", stabilisingJointsFor(ex))
                    InfoRow("Exercise type", exerciseTypeFor(ex))
                    InfoRow("Skill level", ex.difficulty)
                    InfoRow("Force type", forceTypeFor(ex))
                    InfoRow("Compound / Isolation", compoundIsolationFor(ex))
                    InfoRow("Open / Closed chain", chainTypeFor(ex))
                    InfoRow("Unilateral / Bilateral", unilateralBilateralFor(ex))
                }

                // 8. MUSCLE BREAKDOWN — Cards with activation %, role
                RepLogCard {
                    Text("MUSCLE BREAKDOWN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    val muscles = parseMuscleBreakdown(ex)
                    muscles.forEach { muscle ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(muscle.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Row {
                                    Text("${muscle.activation}% • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${muscle.role} • ${muscle.subRole}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            val activationColor = when {
                                muscle.activation > 70 -> Color(0xFF4CAF50)
                                muscle.activation > 40 -> Color(0xFFFF9800)
                                muscle.activation > 15 -> Color(0xFF2196F3)
                                else -> Color(0xFF9E9E9E)
                            }
                            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = activationColor.copy(alpha = 0.15f), modifier = Modifier.padding(start = 8.dp)) {
                                Text(" ${muscle.activation}% ", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = activationColor)
                            }
                        }
                    }
                }

                // 9. ANALYTICS — Keep existing functionality
                RepLogCard {
                    Text("ANALYTICS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Best weight", formatWeight(insight.bestWeight, useKg), Modifier.weight(1f))
                        StatCard("Est. 1RM", formatWeight(insight.bestEstimatedOneRm, useKg), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Total sets", insight.totalSets.toString(), Modifier.weight(1f))
                        StatCard("Volume", formatWeight(insight.totalVolume, useKg), Modifier.weight(1f))
                    }
                }

                // 10. PERSONAL RECORDS — Keep functionality
                RepLogCard {
                    Text("PERSONAL RECORDS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Estimated 1RM: ${formatWeight(insight.bestEstimatedOneRm, useKg)} (best recorded)", style = MaterialTheme.typography.bodyMedium)
                    Text("Best weight: ${formatWeight(insight.bestWeight, useKg)} for ${insight.bestReps} reps", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 11. VOLUME — Keep functionality
                RepLogCard {
                    Text("VOLUME", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Total volume: ${formatWeight(insight.totalVolume, useKg)}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("Sets completed: ${insight.totalSets}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 12. PROGRESS CHARTS — Keep functionality, improve styling
                RepLogCard {
                    Text("PROGRESS CHARTS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    ProgressChart(history = insight.history, useKg = useKg)
                }

                // 13. HISTORY — Keep functionality
                RepLogCard {
                    Text("HISTORY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    if (insight.history.isEmpty()) {
                        InlineEmpty("No completed workout data yet. Log this exercise and finish a workout to build analytics.")
                    } else {
                        insight.history.takeLast(8).reversed().forEach { set ->
                            HistorySetRow(set, useKg)
                        }
                    }
                }

                // Swap alternatives
                if (swaps.isNotEmpty()) {
                    RepLogCard {
                        Text("SWAP ALTERNATIVES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Equipment busy or unavailable? Try one of these.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        swaps.forEach { s ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text(s.exercise.name, fontWeight = FontWeight.SemiBold)
                                Text(s.matchReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

// Helper data for premium sections
private data class MuscleBreakdownRow(
    val name: String,
    val activation: Int,
    val role: String,
    val subRole: String
)

private fun parseMuscleBreakdown(ex: Exercise): List<MuscleBreakdownRow> {
    val primary = ex.primaryMuscles.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val secondary = ex.secondaryMuscles.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val out = mutableListOf<MuscleBreakdownRow>()
    primary.forEach { name ->
        out.add(MuscleBreakdownRow(name = name, activation = 85, role = "Primary", subRole = "Main mover"))
    }
    secondary.forEach { name ->
        out.add(MuscleBreakdownRow(name = name, activation = 45, role = "Secondary", subRole = "Supporting mover"))
    }
    out.add(MuscleBreakdownRow(name = "Stabilisers", activation = 25, role = "Stabiliser", subRole = "Joint control"))
    out.add(MuscleBreakdownRow(name = "Antagonist", activation = 15, role = "Antagonist", subRole = "Opposite action"))
    out.add(MuscleBreakdownRow(name = "Synergist", activation = 35, role = "Synergist", subRole = "Assists main mover"))
    return out.distinctBy { it.name }
}

@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun alternativeEquipmentFor(ex: Exercise): String = when {
    ex.equipment.contains("machine", true) || ex.equipment.contains("cable", true) -> "Dumbbell / Barbell equivalent"
    ex.equipment.contains("barbell", true) -> "Dumbbell / Machine equivalent"
    ex.equipment.contains("dumbbell", true) -> "Barbell / Machine equivalent"
    ex.equipment.contains("kettlebell", true) -> "Dumbbell / Barbell equivalent"
    else -> "Similar movement with different load"
}

private fun machineEquivalentFor(ex: Exercise): String = when {
    ex.name.contains("bench", true) -> "Machine Chest Press"
    ex.name.contains("squat", true) -> "Leg Press / Hack Squat"
    ex.name.contains("deadlift", true) -> "Machine Hip Hinge / RDL"
    ex.name.contains("curl", true) -> "Preacher Curl Machine"
    ex.name.contains("row", true) || ex.name.contains("pull", true) || ex.name.contains("pulldown", true) -> "Seated Cable Row / Lat Pulldown"
    ex.name.contains("raise", true) || ex.name.contains("fly", true) -> "Machine Shoulder / Chest Fly"
    else -> "Machine equivalent of ${ex.name}"
}

private fun homeAlternativeFor(ex: Exercise): String = when {
    ex.equipment.contains("machine", true) || ex.equipment.contains("cable", true) || ex.equipment.contains("barbell", true) ->
        "Bodyweight or resistance band version"
    ex.equipment.contains("dumbbell", true) || ex.equipment.contains("kettlebell", true) ->
        "Bodyweight or household item version"
    else -> "Bodyweight adaptation"
}

private fun bandAlternativeFor(ex: Exercise): String = when {
    ex.name.contains("squat", true) || ex.name.contains("lunge", true) -> "Resistance band squats / lunges"
    ex.name.contains("press", true) || ex.name.contains("bench", true) -> "Band push-ups / band press"
    ex.name.contains("row", true) || ex.name.contains("pull", true) -> "Band rows / band pull-aparts"
    ex.name.contains("curl", true) || ex.name.contains("extension", true) -> "Band curls / band triceps extensions"
    else -> "Resistance band equivalent"
}

private fun jointActionsFor(ex: Exercise): String {
    val lower = ex.name.lowercase()
    return when {
        lower.contains("bench") || lower.contains("press") || lower.contains("push") -> "Shoulder flexion / horizontal adduction; elbow extension"
        lower.contains("row") || lower.contains("pull") || lower.contains("pulldown") -> "Shoulder extension / adduction; elbow flexion"
        lower.contains("squat") || lower.contains("lunge") || lower.contains("leg") -> "Hip extension / knee extension; ankle plantarflexion"
        lower.contains("deadlift") || lower.contains("hinge") || lower.contains("rdl") -> "Hip extension; knee flexion; ankle neutral"
        lower.contains("curl") || lower.contains("bicep") -> "Elbow flexion; shoulder flexion (minor)"
        lower.contains("triceps") || lower.contains("extension") -> "Elbow extension; shoulder extension (minor)"
        lower.contains("raise") || lower.contains("fly") -> "Shoulder abduction / horizontal adduction; elbow static"
        lower.contains("calf") -> "Ankle plantarflexion"
        else -> "Primary joint actions for this movement pattern"
    }
}

private fun planeOfMovementFor(ex: Exercise): String {
    val lower = ex.name.lowercase()
    return when {
        lower.contains("squat") || lower.contains("lunge") || lower.contains("deadlift") || lower.contains("hinge") -> "Sagittal"
        lower.contains("lateral") || lower.contains("abduct") || lower.contains("side") -> "Frontal"
        lower.contains("rotation") || lower.contains("twist") || lower.contains("woodchop") -> "Transverse"
        else -> "Primarily sagittal with secondary frontal/transverse components"
    }
}

private fun primaryJointsFor(ex: Exercise): String {
    val lower = ex.name.lowercase()
    return when {
        lower.contains("bench") || lower.contains("press") || lower.contains("push") -> "Shoulder, Elbow"
        lower.contains("row") || lower.contains("pull") || lower.contains("pulldown") -> "Shoulder, Elbow, Scapula"
        lower.contains("squat") || lower.contains("lunge") || lower.contains("leg") -> "Hip, Knee, Ankle"
        lower.contains("deadlift") || lower.contains("hinge") || lower.contains("rdl") -> "Hip, Knee, Spine"
        lower.contains("curl") || lower.contains("bicep") -> "Elbow"
        lower.contains("calf") -> "Ankle"
        else -> "Primary joints for this movement"
    }
}

private fun stabilisingJointsFor(ex: Exercise): String {
    val lower = ex.name.lowercase()
    return when {
        lower.contains("bench") || lower.contains("press") || lower.contains("squat") -> "Spine (core stabilisation)"
        lower.contains("row") || lower.contains("pull") || lower.contains("deadlift") || lower.contains("hinge") -> "Spine, Scapula, Core"
        lower.contains("lunge") -> "Hip (contralateral), Spine"
        else -> "Spine, core, and adjacent stabilising joints"
    }
}

private fun exerciseTypeFor(ex: Exercise): String {
    val lower = ex.name.lowercase()
    return when {
        lower.contains("plank") || lower.contains("carry") || lower.contains("hold") || lower.contains("static") -> "Isometric"
        lower.contains("olympic") || lower.contains("clean") || lower.contains("snatch") -> "Dynamic / Power"
        else -> "Dynamic (concentric + eccentric)"
    }
}

private fun forceTypeFor(ex: Exercise): String {
    val lower = ex.name.lowercase()
    return when {
        lower.contains("olympic") || lower.contains("clean") || lower.contains("snatch") || lower.contains("jump") -> "Explosive / Power"
        lower.contains("plank") || lower.contains("hold") || lower.contains("carry") || lower.contains("static") -> "Isometric"
        else -> "Concentric / Eccentric (controlled)"
    }
}

private fun compoundIsolationFor(ex: Exercise): String {
    val lower = ex.name.lowercase()
    return when {
        lower.contains("bench") || lower.contains("squat") || lower.contains("deadlift") || lower.contains("row") || lower.contains("pull") -> "Compound (multi-joint)"
        lower.contains("curl") || lower.contains("extension") || lower.contains("raise") || lower.contains("fly") || lower.contains("calf") -> "Isolation (single-joint)"
        else -> "Primarily compound with isolation elements"
    }
}

private fun chainTypeFor(ex: Exercise): String {
    val lower = ex.name.lowercase()
    return when {
        lower.contains("machine") || lower.contains("leg extension") || lower.contains("leg curl") || lower.contains("calf") -> "Open chain"
        else -> "Closed chain (feet/hands fixed)"
    }
}

private fun unilateralBilateralFor(ex: Exercise): String {
    val lower = ex.name.lowercase()
    return when {
        lower.contains("single") || lower.contains("unilateral") || lower.contains("bulgarian") || lower.contains("split") || lower.contains("lunge") || lower.contains("step") -> "Unilateral"
        else -> "Bilateral"
    }
}

@Composable
private fun ProgressChart(history: List<com.replog.data.model.ExerciseSetHistory>, useKg: Boolean) {
    val points = history
        .groupBy { it.workoutStartTime }
        .toSortedMap()
        .map { (time, sets) -> time to (sets.maxOfOrNull { it.estimatedOneRm } ?: 0.0) }
        .filter { it.second > 0.0 }

    if (points.size < 2) {
        RepLogCard {
            Text("Finish at least two workouts with this exercise to see a trend line.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val min = points.minOf { it.second }
    val max = points.maxOf { it.second }
    val range = (max - min).takeIf { it > 0.0 } ?: 1.0

    RepLogCard {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .semantics { contentDescription = "Estimated one rep max trend chart for this exercise" }
        ) {
            val left = 8.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            val width = right - left
            val height = bottom - top

            repeat(4) { index ->
                val y = top + height * (index / 3f)
                drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            }

            val offsets = points.mapIndexed { index, point ->
                val x = left + width * (index.toFloat() / (points.lastIndex).coerceAtLeast(1))
                val y = bottom - (((point.second - min) / range).toFloat() * height)
                Offset(x, y)
            }

            offsets.zipWithNext().forEach { (a, b) ->
                drawLine(primary, a, b, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            }
            offsets.forEach { point ->
                drawCircle(primary, radius = 5.dp.toPx(), center = point)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${formatWeight(min, useKg)} → ${formatWeight(max, useKg)} estimated 1RM",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistorySetRow(set: com.replog.data.model.ExerciseSetHistory, useKg: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${formatWeight(set.weight, useKg)} × ${set.reps}", fontWeight = FontWeight.SemiBold)
            Text(
                formatDate(set.workoutStartTime) + " • est. 1RM ${formatWeight(set.estimatedOneRm, useKg)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (set.isPR) PRBadge()
    }
}

@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var muscles by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Exercise name") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(equipment, { equipment = it }, label = { Text("Equipment") }, singleLine = true)
                OutlinedTextField(muscles, { muscles = it }, label = { Text("Muscles") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onAdd(name, category, equipment, muscles) }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    state: ExerciseUiState,
    onToggleMuscle: (String) -> Unit,
    onToggleEquipment: (String) -> Unit,
    onToggleDifficulty: (String) -> Unit,
    onTogglePattern: (String) -> Unit,
    onToggleGoal: (String) -> Unit,
    onToggleExperience: (String) -> Unit,
    onToggleEquipmentPreset: (String) -> Unit,
    onClear: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val f = state.filter
    val activeCount = f.muscles.size + f.equipment.size + f.difficulties.size + f.patterns.size +
        f.goals.size + f.experiences.size + f.equipmentPresets.size

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Default.Search, null, modifier = Modifier.width(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (activeCount > 0) "Filters ($activeCount)" else "Filters", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text("${state.resultCount} results", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (activeCount > 0) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }

        FilterChipGroup("Muscles", state.muscleGroups, f.muscles, onToggleMuscle)
        if (expanded) {
            FilterChipGroup("Experience", state.experienceOptions, f.experiences, onToggleExperience)
            FilterChipGroup("Goal", state.goalOptions, f.goals, onToggleGoal)
            FilterChipGroup("Quick equipment", state.equipmentPresetOptions, f.equipmentPresets, onToggleEquipmentPreset)
            FilterChipGroup("Equipment", state.equipmentOptions, f.equipment, onToggleEquipment)
            FilterChipGroup("Difficulty", state.difficultyOptions, f.difficulties, onToggleDifficulty)
            FilterChipGroup("Movement", state.patternOptions, f.patterns, onTogglePattern)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipGroup(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    if (options.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onToggle(option) },
                label = { Text(option) }
            )
        }
    }
}
