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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
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

            // Multi-select, combinable filters (muscle / equipment / difficulty / pattern).
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
                    .heightIn(max = 580.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CARD 1: HOW TO PERFORM
                RepLogCard {
                    Text("HOW TO PERFORM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    coaching?.let { c ->
                        Text("Starting position:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.steps.firstOrNull() ?: "Prepare posture, brace core, and stand stable.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Movement:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Breathing:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.breathing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Finish position:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.steps.getOrNull(2) ?: "Pause briefly at peak contraction to maximize tension.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Range of motion:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(c.coaching.rangeOfMotion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } ?: run {
                        Text("Move the weight smoothly through its full range of motion under control, squeezing the target muscle.", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // CARD 2: COACHING CUES
                coaching?.let { c ->
                    if (c.coaching.cues.isNotEmpty()) {
                        RepLogCard {
                            Text("COACHING CUES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            c.coaching.cues.forEach { cue ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(cue, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // CARD 3: TARGET MUSCLES
                RepLogCard {
                    Text("TARGET MUSCLES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    com.replog.domain.visual.anatomy.AnatomicalMuscleDiagram(
                        anatomySpec = anatomySpec,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // CARD 4: COMMON MISTAKES
                coaching?.let { c ->
                    if (c.coaching.mistakes.isNotEmpty()) {
                        RepLogCard {
                            Text("COMMON MISTAKES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            c.coaching.mistakes.forEach { mistake ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("✗ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text(mistake, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // CARD 5: SAFETY
                coaching?.let { c ->
                    if (c.coaching.safety.isNotEmpty()) {
                        RepLogCard {
                            Text("SAFETY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            c.coaching.safety.forEach { advice ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("⚠ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text(advice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // CARD 6: EXERCISE DETAILS
                RepLogCard {
                    Text("EXERCISE DETAILS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Difficulty: ${ex.difficulty}", style = MaterialTheme.typography.bodyMedium)
                    Text("Equipment: ${ex.equipment}", style = MaterialTheme.typography.bodyMedium)
                    if (ex.movementPattern.isNotBlank()) {
                        Text("Movement Pattern: ${ex.movementPattern}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Exercise Family: ${ex.category}", style = MaterialTheme.typography.bodyMedium)
                }

                if (swaps.isNotEmpty()) {
                    ExpandableSection("Swap / alternatives") {
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Best weight", formatWeight(insight.bestWeight, useKg), Modifier.weight(1f))
                    StatCard("Est. 1RM", formatWeight(insight.bestEstimatedOneRm, useKg), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Total sets", insight.totalSets.toString(), Modifier.weight(1f))
                    StatCard("Volume", formatWeight(insight.totalVolume, useKg), Modifier.weight(1f))
                }

                Text("Estimated 1RM trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ProgressChart(history = insight.history, useKg = useKg)

                Text("Recent sets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (insight.history.isEmpty()) {
                    InlineEmpty("No completed workout data yet. Log this exercise and finish a workout to build analytics.")
                } else {
                    insight.history.takeLast(8).reversed().forEach { set ->
                        HistorySetRow(set, useKg)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Best weight", formatWeight(insight.bestWeight, useKg), Modifier.weight(1f))
                    StatCard("Est. 1RM", formatWeight(insight.bestEstimatedOneRm, useKg), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Total sets", insight.totalSets.toString(), Modifier.weight(1f))
                    StatCard("Volume", formatWeight(insight.totalVolume, useKg), Modifier.weight(1f))
                }

                Text("Estimated 1RM trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ProgressChart(history = insight.history, useKg = useKg)

                Text("Recent sets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (insight.history.isEmpty()) {
                    InlineEmpty("No completed workout data yet. Log this exercise and finish a workout to build analytics.")
                } else {
                    insight.history.takeLast(8).reversed().forEach { set ->
                        HistorySetRow(set, useKg)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun ProgressChart(history: List<ExerciseSetHistory>, useKg: Boolean) {
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
private fun HistorySetRow(set: ExerciseSetHistory, useKg: Boolean) {
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

        // Always show muscle chips (the primary filter); other dimensions when expanded.
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
