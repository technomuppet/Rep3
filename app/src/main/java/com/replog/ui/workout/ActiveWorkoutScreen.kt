package com.replog.ui.workout

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.data.model.Exercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SetLog
import com.replog.data.model.SetType
import com.replog.data.model.TemplateWithExercises
import com.replog.ui.components.EmptyState
import com.replog.ui.components.ExerciseIcon
import com.replog.ui.components.NumberInputField
import com.replog.ui.components.PRBadge
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton
import com.replog.ui.components.StatCard
import com.replog.ui.components.formatWeight
import com.replog.util.AdaptiveWorkoutPlan
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    contentPadding: PaddingValues,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var addExercise by remember { mutableStateOf(false) }
    var showCreateTemplate by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TemplateWithExercises?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var templatePendingDelete by remember { mutableStateOf<TemplateWithExercises?>(null) }
    var setPendingDelete by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.activeSessionId, state.restRemainingSeconds) {
        while (state.activeSessionId != null || state.restRemainingSeconds > 0) {
            delay(1_000)
            viewModel.refresh()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WorkoutHeader(
                active = state.activeSessionId != null,
                elapsed = elapsed(state.startTime)
            )
        }

        if (state.restoredWorkout) {
            item {
                RepLogCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Workout recovered", fontWeight = FontWeight.Bold)
                            Text(
                                "Your active session was restored automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = viewModel::dismissRestoredBanner) { Text("OK") }
                    }
                }
            }
        }

        if (state.activeSessionId == null) {
            item { PrimaryButton("Start Empty Workout") { viewModel.startWorkout() } }
            state.adaptivePlan?.let { plan ->
                item { AdaptivePlanCard(plan = plan) { viewModel.startAdaptiveWorkout(plan) } }
            }
            item { SecondaryButton("Create Template") { showCreateTemplate = true } }
            item { SectionTitle("Quick start templates") }
            if (state.templates.isEmpty()) {
                item { EmptyState("Templates loading", "Built-in templates will appear after first launch setup.") }
            } else {
                items(state.templates, key = { it.template.id }) { template ->
                    TemplateCard(
                        template = template,
                        onStart = { viewModel.startWorkoutFromTemplate(template) },
                        onEdit = { editingTemplate = template },
                        onDelete = { templatePendingDelete = template }
                    )
                }
            }
        } else {
            if (state.restRemainingSeconds > 0) {
                item {
                    RestTimerCard(
                        remaining = state.restRemainingSeconds,
                        total = state.restSeconds,
                        onSkip = viewModel::skipRestTimer
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton("Add Exercise", Modifier.weight(1f)) { addExercise = true }
                    SecondaryButton("Finish", Modifier.weight(1f), enabled = state.session?.exercises?.isNotEmpty() == true) {
                        viewModel.finishWorkout()
                    }
                }
            }

            val entries = state.session?.exercises.orEmpty().sortedBy { it.sessionExercise.orderIndex }
            if (entries.isEmpty()) {
                item { EmptyState("No exercises yet", "Add an exercise, then log your working sets.") }
            } else {
                item {
                    WorkoutStatsRow(entries = entries, elapsed = elapsed(state.startTime), useKg = state.useKg)
                }
                val blocks = workoutBlocks(entries)
                items(blocks, key = { it.key }) { block ->
                    when (block) {
                        is WorkoutDisplayBlock.Single -> {
                            val entry = block.entry
                            WorkoutExerciseCard(
                                entry = entry,
                                supersetLabel = supersetLabel(entry, entries),
                                adaptiveTarget = state.targetsByExerciseId[entry.exercise.id],
                                previousSets = state.lastSetsByExerciseId[entry.exercise.id].orEmpty(),
                                useKg = state.useKg,
                                onAddSet = { w, r, type, rpe, tempo -> viewModel.addSet(entry, w, r, type, rpe, tempo) },
                                onEditSet = { set, w, r, type, rpe, tempo -> viewModel.editSet(entry, set, w, r, type, rpe, tempo) },
                                onDeleteSet = { setPendingDelete = it },
                                onSetSuperset = { group -> viewModel.setSupersetGroup(entry, group) },
                                onRemoveExercise = { viewModel.removeExercise(entry.sessionExercise.id) }
                            )
                        }
                        is WorkoutDisplayBlock.Superset -> {
                            SupersetGroupBlock(
                                group = block.group,
                                entries = block.entries,
                                allEntries = entries,
                                targetsByExerciseId = state.targetsByExerciseId,
                                lastSetsByExerciseId = state.lastSetsByExerciseId,
                                useKg = state.useKg,
                                onAddSet = { entry, w, r, type, rpe, tempo -> viewModel.addSet(entry, w, r, type, rpe, tempo) },
                                onEditSet = { entry, set, w, r, type, rpe, tempo -> viewModel.editSet(entry, set, w, r, type, rpe, tempo) },
                                onDeleteSet = { setPendingDelete = it },
                                onSetSuperset = { entry, group -> viewModel.setSupersetGroup(entry, group) },
                                onRemoveExercise = { viewModel.removeExercise(it.sessionExercise.id) }
                            )
                        }
                    }
                }
            }

            item {
                SecondaryButton("Discard Workout") { confirmDiscard = true }
            }
        }
    }

    if (showCreateTemplate) {
        CreateTemplateDialog(
            exercises = state.allExercises,
            initialTemplate = null,
            onDismiss = { showCreateTemplate = false },
            onSave = { name, selected ->
                viewModel.createTemplate(name, selected)
                showCreateTemplate = false
            }
        )
    }

    editingTemplate?.let { template ->
        CreateTemplateDialog(
            exercises = state.allExercises,
            initialTemplate = template,
            onDismiss = { editingTemplate = null },
            onSave = { name, selected ->
                viewModel.updateTemplate(template, name, selected)
                editingTemplate = null
            }
        )
    }

    if (addExercise) {
        ExercisePickerDialog(
            exercises = state.allExercises,
            onDismiss = { addExercise = false },
            onPick = {
                viewModel.addExercise(it)
                addExercise = false
            }
        )
    }

    if (confirmDiscard) {
        ConfirmActionDialog(
            title = "Discard workout?",
            message = "This will permanently delete the active workout and all sets logged in it.",
            confirmLabel = "Discard",
            onCancel = { confirmDiscard = false },
            onConfirm = {
                viewModel.discardWorkout()
                confirmDiscard = false
            }
        )
    }

    templatePendingDelete?.let { template ->
        ConfirmActionDialog(
            title = "Delete template?",
            message = "This deletes '${template.template.name}'. Completed workouts are not affected.",
            confirmLabel = "Delete",
            onCancel = { templatePendingDelete = null },
            onConfirm = {
                viewModel.deleteTemplate(template)
                templatePendingDelete = null
            }
        )
    }

    setPendingDelete?.let { setId ->
        ConfirmActionDialog(
            title = "Delete set?",
            message = "This set will be removed from the workout.",
            confirmLabel = "Delete",
            onCancel = { setPendingDelete = null },
            onConfirm = {
                viewModel.deleteSet(setId)
                setPendingDelete = null
            }
        )
    }

    state.summary?.let { summary ->
        WorkoutSummaryDialog(
            summary = summary,
            useKg = state.useKg,
            onDismiss = viewModel::dismissSummary
        )
    }
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

@Composable
private fun WorkoutHeader(active: Boolean, elapsed: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Workout", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                if (active) "Active • $elapsed" else "Templates, quick logging, PRs and recovery.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (active) Icons.Default.Timer else Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AdaptivePlanCard(plan: AdaptiveWorkoutPlan, onStart: () -> Unit) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(plan.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Fatigue modifier: ${plan.fatigueModifier}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        AssistChip(onClick = onStart, label = { Text("Start") })
    }
    Spacer(Modifier.height(8.dp))
    plan.targets.take(4).forEach { target ->
        Text(
            "${target.exerciseName}: ${target.adjustment} • ${formatWeight(target.suggestedWeight)} × ${target.suggestedReps} for ${target.suggestedSets} sets",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TemplateCard(
    template: TemplateWithExercises,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) = RepLogCard(onClick = onStart) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(template.template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                template.exercises.sortedBy { it.templateExercise.orderIndex }.joinToString(" • ") { it.exercise.name },
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!template.template.isBuiltIn) {
                Text("Custom template", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        if (!template.template.isBuiltIn) {
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit template") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete template", tint = MaterialTheme.colorScheme.error) }
        }
        AssistChip(onClick = onStart, label = { Text("Start") })
    }
}

@Composable
private fun WorkoutStatsRow(entries: List<SessionExerciseWithSets>, elapsed: String, useKg: Boolean) {
    val setCount = entries.sumOf { it.sets.size }
    val volume = entries.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } }
    val prs = entries.sumOf { entry -> entry.sets.count { it.isPR } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Time", elapsed, Modifier.weight(1f))
            StatCard("Sets", setCount.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Volume", formatWeight(volume, useKg), Modifier.weight(1f))
            StatCard("PRs", prs.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun RestTimerCard(remaining: Int, total: Int, onSkip: () -> Unit) = RepLogCard {
    val progress = if (total <= 0) 0f else (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Rest timer", fontWeight = FontWeight.Bold)
            Text("${remaining}s remaining", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onSkip) { Text("Skip") }
    }
    Spacer(Modifier.height(10.dp))
    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
}

private sealed class WorkoutDisplayBlock(val key: String) {
    data class Single(val entry: SessionExerciseWithSets) : WorkoutDisplayBlock("single-${entry.sessionExercise.id}")
    data class Superset(val group: String, val entries: List<SessionExerciseWithSets>) : WorkoutDisplayBlock("superset-$group-${entries.joinToString("-") { it.sessionExercise.id.toString() }}")
}

private fun workoutBlocks(entries: List<SessionExerciseWithSets>): List<WorkoutDisplayBlock> {
    val seenGroups = mutableSetOf<String>()
    return entries.mapNotNull { entry ->
        val group = entry.sessionExercise.supersetGroup
        if (group.isNullOrBlank()) {
            WorkoutDisplayBlock.Single(entry)
        } else if (group !in seenGroups) {
            seenGroups += group
            val grouped = entries.filter { it.sessionExercise.supersetGroup == group }.sortedBy { it.sessionExercise.orderIndex }
            if (grouped.size > 1) WorkoutDisplayBlock.Superset(group, grouped) else WorkoutDisplayBlock.Single(entry)
        } else {
            null
        }
    }
}

@Composable
private fun SupersetGroupBlock(
    group: String,
    entries: List<SessionExerciseWithSets>,
    allEntries: List<SessionExerciseWithSets>,
    targetsByExerciseId: Map<Int, WorkoutTargetUi>,
    lastSetsByExerciseId: Map<Int, List<SetLog>>,
    useKg: Boolean,
    onAddSet: (SessionExerciseWithSets, Double, Int, String, Double?, String?) -> Unit,
    onEditSet: (SessionExerciseWithSets, SetLog, Double, Int, String, Double?, String?) -> Unit,
    onDeleteSet: (Int) -> Unit,
    onSetSuperset: (SessionExerciseWithSets, String?) -> Unit,
    onRemoveExercise: (SessionExerciseWithSets) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RepLogCard {
            Text("Superset $group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Perform these exercises as a linked block before resting.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        entries.forEach { entry ->
            WorkoutExerciseCard(
                entry = entry,
                supersetLabel = supersetLabel(entry, allEntries),
                adaptiveTarget = targetsByExerciseId[entry.exercise.id],
                previousSets = lastSetsByExerciseId[entry.exercise.id].orEmpty(),
                useKg = useKg,
                onAddSet = { w, r, type, rpe, tempo -> onAddSet(entry, w, r, type, rpe, tempo) },
                onEditSet = { set, w, r, type, rpe, tempo -> onEditSet(entry, set, w, r, type, rpe, tempo) },
                onDeleteSet = onDeleteSet,
                onSetSuperset = { groupValue -> onSetSuperset(entry, groupValue) },
                onRemoveExercise = { onRemoveExercise(entry) }
            )
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    entry: SessionExerciseWithSets,
    supersetLabel: String?,
    adaptiveTarget: WorkoutTargetUi?,
    previousSets: List<SetLog>,
    useKg: Boolean,
    onAddSet: (Double, Int, String, Double?, String?) -> Unit,
    onEditSet: (SetLog, Double, Int, String, Double?, String?) -> Unit,
    onDeleteSet: (Int) -> Unit,
    onSetSuperset: (String?) -> Unit,
    onRemoveExercise: () -> Unit
) {
    var showSetDialog by remember { mutableStateOf(false) }
    var showSupersetDialog by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<SetLog?>(null) }
    val sortedSets = entry.sets.sortedBy { it.setNumber }
    val suggestedSet = sortedSets.lastOrNull() ?: previousSets.lastOrNull()

    RepLogCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${entry.exercise.category} • ${entry.exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                supersetLabel?.let { label ->
                    Text("Superset $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = { showSupersetDialog = true }) { Text("Superset") }
            IconButton(onClick = onRemoveExercise) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }

        adaptiveTarget?.let { target ->
            Spacer(Modifier.height(8.dp))
            RepLogCard {
                Text("Today's target", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    "${target.adjustment}: ${target.targetWeight?.let { formatWeight(it, useKg) } ?: "Any load"} × ${target.targetReps} for ${target.targetSets} sets",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(target.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (previousSets.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Last completed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            previousSets.forEachIndexed { index, set ->
                Text(
                    "${index + 1}. ${formatWeight(set.weight, useKg)} × ${set.reps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        if (sortedSets.isEmpty()) {
            Text("No sets logged yet. Use the suggestion or add your first working set.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            sortedSets.forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingSet = set },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Set ${set.setNumber}: ${formatWeight(set.weight, useKg)} × ${set.reps}",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            listOf(set.setType, set.rpe?.let { "RPE ${"%.1f".format(it)}" }, set.tempo?.takeIf { it.isNotBlank() }?.let { "Tempo $it" }).filterNotNull().joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (set.isPR) PRBadge()
                    IconButton(onClick = { editingSet = set }) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton(onClick = { onDeleteSet(set.id) }) { Icon(Icons.Default.Delete, "Delete") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Add Set", Modifier.weight(1f)) { showSetDialog = true }
            suggestedSet?.let { suggestion ->
                PrimaryButton(
                    text = "Repeat ${formatWeight(suggestion.weight, useKg)}×${suggestion.reps}",
                    modifier = Modifier.weight(1f)
                ) { onAddSet(suggestion.weight, suggestion.reps, suggestion.setType, suggestion.rpe, suggestion.tempo) }
            }
        }
    }

    if (showSupersetDialog) {
        SupersetDialog(
            current = entry.sessionExercise.supersetGroup,
            onDismiss = { showSupersetDialog = false },
            onSelect = { group ->
                onSetSuperset(group)
                showSupersetDialog = false
            }
        )
    }

    if (showSetDialog) {
        AddOrEditSetDialog(
            title = "Add Set",
            initialSet = suggestedSet,
            useKg = useKg,
            onDismiss = { showSetDialog = false },
            onSave = { w, r, type, rpe, tempo ->
                onAddSet(w, r, type, rpe, tempo)
                showSetDialog = false
            }
        )
    }

    editingSet?.let { set ->
        AddOrEditSetDialog(
            title = "Edit Set ${set.setNumber}",
            initialSet = set,
            useKg = useKg,
            onDismiss = { editingSet = null },
            onSave = { w, r, type, rpe, tempo ->
                onEditSet(set, w, r, type, rpe, tempo)
                editingSet = null
            }
        )
    }
}

@Composable
private fun SupersetDialog(
    current: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    val groups = listOf("A", "B", "C", "D")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Superset group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assign matching letters to exercises that should be performed together, e.g. A1/A2.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                groups.forEach { group ->
                    TextButton(onClick = { onSelect(group) }) {
                        Text(if (current == group) "✓ Group $group" else "Group $group")
                    }
                }
                TextButton(onClick = { onSelect(null) }) { Text("No superset") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddOrEditSetDialog(
    title: String,
    initialSet: SetLog?,
    useKg: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, Int, String, Double?, String?) -> Unit
) {
    var weight by remember(initialSet?.id) { mutableStateOf(initialSet?.weight?.toCleanString().orEmpty()) }
    var reps by remember(initialSet?.id) { mutableStateOf(initialSet?.reps?.toString().orEmpty()) }
    var setType by remember(initialSet?.id) { mutableStateOf(initialSet?.setType ?: SetType.WORKING) }
    var rpe by remember(initialSet?.id) { mutableStateOf(initialSet?.rpe?.toCleanString().orEmpty()) }
    var tempo by remember(initialSet?.id) { mutableStateOf(initialSet?.tempo.orEmpty()) }
    val valid = (weight.toDoubleOrNull() ?: -1.0) >= 0 && (reps.toIntOrNull() ?: 0) > 0
    val rpeValue = rpe.toDoubleOrNull()?.coerceIn(1.0, 10.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Log advanced set types now; these fuel future Training DNA analytics.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(weight, "Weight", Modifier.weight(1f), if (useKg) "kg" else "lb") { weight = it }
                    NumberInputField(reps, "Reps", Modifier.weight(1f)) { reps = it.filter { char -> char.isDigit() } }
                }
                Text("Set type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SetType.all.forEach { type ->
                        FilterChip(
                            selected = setType == type,
                            onClick = { setType = type },
                            label = { Text(type) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(rpe, "RPE", Modifier.weight(1f)) { value ->
                        rpe = value.filter { it.isDigit() || it == '.' }
                    }
                    OutlinedTextField(
                        value = tempo,
                        onValueChange = { tempo = it.take(12) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Tempo") },
                        placeholder = { Text("3-1-1") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(weight.toDoubleOrNull() ?: 0.0, reps.toIntOrNull() ?: 0, setType, rpeValue, tempo) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateTemplateDialog(
    exercises: List<Exercise>,
    initialTemplate: TemplateWithExercises?,
    onDismiss: () -> Unit,
    onSave: (String, List<TemplateExerciseDraft>) -> Unit
) {
    var name by remember(initialTemplate?.template?.id) { mutableStateOf(initialTemplate?.template?.name.orEmpty()) }
    var query by remember { mutableStateOf("") }
    var drafts by remember(initialTemplate?.template?.id) {
        mutableStateOf(
            initialTemplate?.exercises
                ?.sortedBy { it.templateExercise.orderIndex }
                ?.map {
                    TemplateExerciseDraft(
                        exercise = it.exercise,
                        defaultSets = it.templateExercise.defaultSets,
                        targetReps = it.templateExercise.targetReps,
                        targetWeight = it.templateExercise.targetWeight
                    )
                }
                .orEmpty()
        )
    }
    var editingDraft by remember { mutableStateOf<TemplateExerciseDraft?>(null) }
    val filtered = exercises.filter {
        query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) || it.equipment.contains(query, true) || it.movementPattern.contains(query, true)
    }

    fun move(id: Int, direction: Int) {
        val index = drafts.indexOfFirst { it.exercise.id == id }
        val newIndex = (index + direction).coerceIn(0, drafts.lastIndex)
        if (index >= 0 && newIndex != index) {
            drafts = drafts.toMutableList().also { list ->
                val item = list.removeAt(index)
                list.add(newIndex, item)
            }
        }
    }

    fun upsertDraft(draft: TemplateExerciseDraft) {
        drafts = if (drafts.any { it.exercise.id == draft.exercise.id }) {
            drafts.map { if (it.exercise.id == draft.exercise.id) draft else it }
        } else {
            drafts + draft
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTemplate == null) "Create Template" else "Edit Template") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Template name") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search exercises") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${drafts.size} selected • tap selected exercise to edit targets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (drafts.isNotEmpty()) {
                    LazyColumn(Modifier.heightIn(max = 175.dp)) {
                        items(drafts, key = { it.exercise.id }) { draft ->
                            val index = drafts.indexOfFirst { it.exercise.id == draft.exercise.id }
                            ListItem(
                                headlineContent = { Text("${index + 1}. ${draft.exercise.name}") },
                                supportingContent = {
                                    Text("${draft.defaultSets} sets × ${draft.targetReps} reps" + (draft.targetWeight?.let { " @ ${formatWeight(it)}" } ?: ""))
                                },
                                trailingContent = {
                                    Row {
                                        TextButton(onClick = { move(draft.exercise.id, -1) }, enabled = index > 0) { Text("Up") }
                                        TextButton(onClick = { move(draft.exercise.id, 1) }, enabled = index < drafts.lastIndex) { Text("Down") }
                                    }
                                },
                                modifier = Modifier.clickable { editingDraft = draft }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 230.dp)) {
                    items(filtered, key = { it.id }) { exercise ->
                        val selected = drafts.any { it.exercise.id == exercise.id }
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = { Text("${exercise.category} • ${exercise.equipment}") },
                            leadingContent = { Checkbox(checked = selected, onCheckedChange = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    drafts = if (selected) drafts.filterNot { it.exercise.id == exercise.id } else drafts + TemplateExerciseDraft(exercise)
                                }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && drafts.isNotEmpty(),
                onClick = { onSave(name, drafts) }
            ) { Text(if (initialTemplate == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    editingDraft?.let { draft ->
        TemplateTargetDialog(
            draft = draft,
            onDismiss = { editingDraft = null },
            onSave = {
                upsertDraft(it)
                editingDraft = null
            }
        )
    }
}

@Composable
private fun TemplateTargetDialog(
    draft: TemplateExerciseDraft,
    onDismiss: () -> Unit,
    onSave: (TemplateExerciseDraft) -> Unit
) {
    var sets by remember(draft.exercise.id) { mutableStateOf(draft.defaultSets.toString()) }
    var reps by remember(draft.exercise.id) { mutableStateOf(draft.targetReps.toString()) }
    var weight by remember(draft.exercise.id) { mutableStateOf(draft.targetWeight?.toCleanString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(draft.exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Programmed template target", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField(sets, "Sets", Modifier.weight(1f)) { sets = it.filter { char -> char.isDigit() } }
                    NumberInputField(reps, "Reps", Modifier.weight(1f)) { reps = it.filter { char -> char.isDigit() } }
                }
                NumberInputField(weight, "Target weight", Modifier.fillMaxWidth(), "optional") { value ->
                    weight = value.filter { it.isDigit() || it == '.' }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = (sets.toIntOrNull() ?: 0) > 0 && (reps.toIntOrNull() ?: 0) > 0,
                onClick = {
                    onSave(
                        draft.copy(
                            defaultSets = sets.toIntOrNull() ?: draft.defaultSets,
                            targetReps = reps.toIntOrNull() ?: draft.targetReps,
                            targetWeight = weight.toDoubleOrNull()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit
) {
    var q by remember { mutableStateOf("") }
    val filtered = exercises.filter {
        q.isBlank() || it.name.contains(q, true) || it.category.contains(q, true) || it.equipment.contains(q, true) || it.movementPattern.contains(q, true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search name, category or equipment") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(filtered, key = { it.id }) { ex ->
                        ListItem(
                            headlineContent = { Text(ex.name) },
                            supportingContent = { Text("${ex.category} • ${ex.equipment}") },
                            leadingContent = { ExerciseIcon(Modifier.size(40.dp)) },
                            trailingContent = { Icon(Icons.Default.Add, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(ex) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun WorkoutSummaryDialog(summary: WorkoutSummary, useKg: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Workout complete") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(summary.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Duration", formatDuration(summary.durationMillis), Modifier.weight(1f))
                    StatCard("Sets", summary.setCount.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Volume", formatWeight(summary.volume, useKg), Modifier.weight(1f))
                    StatCard("PRs", summary.prCount.toString(), Modifier.weight(1f))
                }
                if (summary.targetHitCount + summary.targetMissCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Targets hit", summary.targetHitCount.toString(), Modifier.weight(1f))
                        StatCard("Missed", summary.targetMissCount.toString(), Modifier.weight(1f))
                    }
                    summary.adaptiveNotes.forEach { note ->
                        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (summary.progressionNotes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Next progression", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        summary.progressionNotes.forEach { note ->
                            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(
                    if (summary.prCount > 0) "Strong session — you set ${summary.prCount} PR${if (summary.prCount == 1) "" else "s"}." else "Session saved. Keep stacking consistent work.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

private fun supersetLabel(entry: SessionExerciseWithSets, entries: List<SessionExerciseWithSets>): String? {
    val group = entry.sessionExercise.supersetGroup ?: return null
    val grouped = entries.filter { it.sessionExercise.supersetGroup == group }.sortedBy { it.sessionExercise.orderIndex }
    val index = grouped.indexOfFirst { it.sessionExercise.id == entry.sessionExercise.id }.takeIf { it >= 0 } ?: 0
    return "$group${index + 1}"
}

private fun elapsed(start: Long?): String {
    if (start == null) return "00:00"
    val sec = ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60)
}

private fun formatDuration(durationMillis: Long): String {
    val minutes = (durationMillis / 60_000).coerceAtLeast(0)
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}

private fun Double.toCleanString(): String = if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
