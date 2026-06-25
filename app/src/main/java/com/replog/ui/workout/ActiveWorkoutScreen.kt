package com.replog.ui.workout

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.FileProvider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.replog.util.timer.RestTimerState

@Composable
fun ActiveWorkoutScreen(
    contentPadding: PaddingValues,
    startFromRecommendation: Boolean = false,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    // Phase 3 — Smart Start: if arriving from the Coach card, build the session
    // from the staged recommendation exactly once.
    LaunchedEffect(startFromRecommendation) {
        if (startFromRecommendation) viewModel.consumePendingRecommendation()
    }
    val state by viewModel.uiState.collectAsState()
    val templateMessage by viewModel.templateMessage.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val importTemplateLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (json != null) viewModel.importTemplateJson(json)
        }
    }
    var addExercise by remember { mutableStateOf(false) }
    var showCreateTemplate by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TemplateWithExercises?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var templatePendingDelete by remember { mutableStateOf<TemplateWithExercises?>(null) }
    var setPendingDelete by remember { mutableStateOf<Int?>(null) }
    var editSessionNotes by remember { mutableStateOf(false) }

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
                item { AdaptivePlanCard(plan = plan, useKg = state.useKg) { viewModel.startAdaptiveWorkout(plan) } }
            }
            item { SecondaryButton("Create Template") { showCreateTemplate = true } }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Add Built-in", Modifier.weight(1f)) { viewModel.installAllBuiltInTemplates() }
                    SecondaryButton("Import Template", Modifier.weight(1f)) {
                        importTemplateLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*", "*/*"))
                    }
                }
            }
            templateMessage?.let { msg ->
                item {
                    RepLogCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(msg, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { viewModel.clearTemplateMessage() }) { Text("Dismiss") }
                        }
                    }
                }
            }
            item { SectionTitle("Quick start templates") }
            if (state.templates.isEmpty()) {
                item { EmptyState("Templates loading", "Built-in templates will appear after first launch setup.") }
            } else {
                itemsIndexed(state.templates, key = { _, t -> t.template.id }) { _, template ->
                    TemplateCard(
                        template = template,
                        onStart = { viewModel.startWorkoutFromTemplate(template) },
                        onToggleFavorite = { viewModel.toggleTemplateFavorite(template) },
                        onEdit = { editingTemplate = template },
                        onDelete = { templatePendingDelete = template },
                        onShare = {
                            val (fileName, json) = viewModel.buildShareableTemplate(template)
                            shareTemplateFile(ctx, fileName, json)
                        }
                    )
                }
            }
        } else {
            // Rest timer card – always visible when active
            item {
                RestTimerCardV2(
                    timer = state.restTimer,
                    autoStart = state.restAutoStart,
                    onAdd = { viewModel.addRestSeconds(it) },
                    onSkip = viewModel::skipRestTimer,
                    onRestart = viewModel::restartRestTimer,
                    onToggleAuto = { viewModel.setRestAutoStart(!state.restAutoStart) },
                    onQuickStart = { viewModel.startRestTimer(it) }
                )
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

                // Session notes
                item {
                    SessionNotesCard(
                        notes = state.session?.session?.notes ?: "",
                        onEdit = { editSessionNotes = true }
                    )
                }

                itemsIndexed(entries, key = { _, e -> e.sessionExercise.id }) { idx, entry ->
                    WorkoutExerciseCard(
                        entry = entry,
                        index = idx,
                        total = entries.size,
                        supersetLabel = supersetLabel(entry, entries),
                        adaptiveTarget = state.targetsByExerciseId[entry.exercise.id],
                        previousSets = state.lastSetsByExerciseId[entry.exercise.id].orEmpty(),
                        progression = state.progressionByExerciseId[entry.exercise.id],
                        useKg = state.useKg,
                        autoFocusField = state.autoFocusField,
                        onAddSet = { w, r, type, rpe, tempo -> viewModel.addSet(entry, w, r, type, rpe, tempo) },
                        onRepeatLastSet = { viewModel.repeatLastSet(entry) },
                        onQuickComplete = { w, r -> viewModel.quickCompleteSet(entry, w, r) },
                        onEditSet = { set, w, r, type, rpe, tempo -> viewModel.editSet(entry, set, w, r, type, rpe, tempo) },
                        onDeleteSet = { setPendingDelete = it },
                        onSetSuperset = { group -> viewModel.setSupersetGroup(entry, group) },
                        onRemoveExercise = { viewModel.removeExercise(entry.sessionExercise.id) },
                        onMoveUp = if (idx > 0) { { state.activeSessionId?.let { viewModel.moveExercise(it, idx, idx - 1) } } } else null,
                        onMoveDown = if (idx < entries.lastIndex) { { state.activeSessionId?.let { viewModel.moveExercise(it, idx, idx + 1) } } } else null,
                        onSaveNotes = { notes -> viewModel.updateExerciseNotes(entry, notes) }
                    )
                }
            }

            item {
                SecondaryButton("Discard Workout") { confirmDiscard = true }
            }
        }
    }

    if (editSessionNotes && state.activeSessionId != null) {
        var notes by remember(state.session?.session?.notes) { mutableStateOf(state.session?.session?.notes ?: "") }
        AlertDialog(
            onDismissRequest = { editSessionNotes = false },
            title = { Text("Session notes") },
            text = {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(2000) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Bad sleep / Felt strong / Shoulder sore…") },
                    minLines = 3
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.updateSessionNotes(notes); editSessionNotes = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editSessionNotes = false }) { Text("Cancel") } }
        )
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

    // Sprint 3 – Workout Completion Screen
    state.summary?.let { summary ->
        WorkoutCompletionDialog(summary = summary, useKg = state.useKg, onRate = viewModel::rateWorkout, onDismiss = viewModel::dismissSummary)
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
                if (active) "Active • $elapsed" else "Templates, quick logging, personal bests and recovery.",
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
private fun AdaptivePlanCard(plan: AdaptiveWorkoutPlan, useKg: Boolean, onStart: () -> Unit) = RepLogCard {
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
            "${target.exerciseName}: ${target.adjustment} • ${formatWeight(target.suggestedWeight, useKg)} × ${target.suggestedReps} for ${target.suggestedSets} sets",
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
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit
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
        // P5: star to pin this template to the Home "Quick Start" row.
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (template.template.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = if (template.template.isFavorite) "Unpin from Quick Start" else "Pin to Quick Start",
                tint = if (template.template.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share template") }
        if (!template.template.isBuiltIn) {
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit template") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete template", tint = MaterialTheme.colorScheme.error) }
        }
        AssistChip(onClick = onStart, label = { Text("Start") })
    }
}

@Composable
private fun WorkoutStatsRow(entries: List<SessionExerciseWithSets>, elapsed: String, useKg: Boolean) {
    val setCount by remember(entries) { derivedStateOf { entries.sumOf { it.sets.size } } }
    val volume by remember(entries) { derivedStateOf { entries.sumOf { e -> e.sets.sumOf { it.weight * it.reps } } } }
    val prs by remember(entries) { derivedStateOf { entries.sumOf { e -> e.sets.count { it.isPR } } } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RepLogCard {
            Text("Session Volume", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(formatWeight(volume, useKg), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Updates live every set", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Time", elapsed, Modifier.weight(1f))
            StatCard("Sets", setCount.toString(), Modifier.weight(1f))
            StatCard("PBs", prs.toString(), Modifier.weight(1f))
        }
    }
}

// Sprint 3 – Rest Timer Overhaul
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestTimerCardV2(
    timer: RestTimerState,
    autoStart: Boolean,
    onAdd: (Int) -> Unit,
    onSkip: () -> Unit,
    onRestart: () -> Unit,
    onToggleAuto: () -> Unit,
    onQuickStart: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    RepLogCard {
        if (timer.ready) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("READY FOR NEXT SET", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("Time to lift!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onRestart() }) {
                    Icon(Icons.Default.Timer, null); Spacer(Modifier.width(8.dp)); Text("Restart Timer")
                }
            }
            return@RepLogCard
        }
        if (!timer.active) {
            // Idle – quick start buttons
            Text("Rest timer", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 90, 120, 180).forEach { s ->
                    AssistChip(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onQuickStart(s) }, label = { Text("${s}s") })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-start after set", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onToggleAuto) { Text(if (autoStart) "ON" else "OFF", fontWeight = FontWeight.Bold) }
            }
            return@RepLogCard
        }
        val progress = if (timer.totalSeconds > 0) timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat() else 0f
        val animatedProgress by animateFloatAsState(progress, label = "rest")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxSize(), strokeWidth = 6.dp)
                Text("${timer.remainingSeconds}s", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Rest", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "%d:%02d".format(timer.remainingSeconds / 60, timer.remainingSeconds % 60),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("Next set ready soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onSkip() }) {
                Icon(Icons.Default.SkipNext, "Skip", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(30 to "+30s", 60 to "+60s").forEach { (sec, label) ->
                OutlinedButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdd(sec) }, modifier = Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.Bold) }
            }
            Button(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onSkip() }, modifier = Modifier.weight(1f)) { Text("Skip", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SessionNotesCard(notes: String, onEdit: () -> Unit) {
    RepLogCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Session notes", fontWeight = FontWeight.Bold)
                Text(if (notes.isBlank()) "Tap to add notes – Bad sleep / Felt strong / Sore…" else notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.EditNote, "Edit notes") }
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    entry: SessionExerciseWithSets,
    index: Int,
    total: Int,
    supersetLabel: String?,
    adaptiveTarget: WorkoutTargetUi?,
    previousSets: List<SetLog>,
    progression: ProgressionSuggestionUi?,
    useKg: Boolean,
    autoFocusField: String = "weight",
    onAddSet: (Double, Int, String, Double?, String?) -> Unit,
    onRepeatLastSet: () -> Unit,
    onQuickComplete: (Double, Int) -> Unit,
    onEditSet: (SetLog, Double, Int, String, Double?, String?) -> Unit,
    onDeleteSet: (Int) -> Unit,
    onSetSuperset: (String?) -> Unit,
    onRemoveExercise: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onSaveNotes: (String) -> Unit
) {
    var showSetDialog by remember { mutableStateOf(false) }
    var showSupersetDialog by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<SetLog?>(null) }
    var showNotesDialog by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val sortedSets = remember(entry.sets) { entry.sets.sortedBy { it.setNumber } }
    val suggestedSet = remember(sortedSets, previousSets, progression) {
        sortedSets.lastOrNull()
            ?: previousSets.lastOrNull()
            ?: progression?.let { com.replog.data.model.SetLog(sessionExerciseId = entry.sessionExercise.id, setNumber = 1, weight = it.weight, reps = it.reps, rpe = it.rpe, isPR = false, prType = null, completed = true) }
    }
    val canRepeat = suggestedSet != null

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
            if (onMoveUp != null) IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, "Move up") }
            if (onMoveDown != null) IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, "Move down") }
            IconButton(onClick = { showNotesDialog = true }) { Icon(Icons.Default.EditNote, "Notes", tint = if (entry.sessionExercise.notes.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = { showSupersetDialog = true }) { Text("Superset") }
            IconButton(onClick = onRemoveExercise) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }

        if (entry.sessionExercise.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("📝 ${entry.sessionExercise.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Progression suggestion – FEATURE 4
        progression?.let { p ->
            Spacer(Modifier.height(8.dp))
            RepLogCard {
                Text("Suggested: ${formatWeight(p.weight, useKg)} × ${p.reps}" + (p.rpe?.let { " @ RPE $it" } ?: ""), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(p.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onQuickComplete(p.weight, p.reps)
                }, modifier = Modifier.fillMaxWidth()) { Text("✓ Complete ${formatWeight(p.weight, useKg)} × ${p.reps}", fontWeight = FontWeight.Bold) }
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
                // Phase 1: tap the planned set to log it instantly (no dialog) when a target load is known.
                target.targetWeight?.let { tw ->
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onQuickComplete(tw, target.targetReps)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("✓ Complete ${formatWeight(tw, useKg)} × ${target.targetReps}", fontWeight = FontWeight.Bold) }
                }
            }
        }

        // FEATURE 3 — Smart Previous Performance
        if (previousSets.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Last session", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            previousSets.forEachIndexed { index, set ->
                Text(
                    "${index + 1}. ${formatWeight(set.weight, useKg)} × ${set.reps}" +
                        (set.rpe?.let { " @ RPE $it" } ?: "") +
                        if (set.setType != SetType.WORKING) " • ${set.setType}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        if (sortedSets.isEmpty()) {
            Text("No sets logged yet. Tap Repeat to copy last session, or Add Set.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        val meta = listOfNotNull(
                            set.setType.takeIf { it != SetType.WORKING },
                            set.rpe?.let { "RPE ${"%.1f".format(it)}" },
                            set.tempo?.takeIf { it.isNotBlank() }?.let { "Tempo $it" },
                            set.prType?.let { "🏆 PB ${it}" }
                        ).joinToString(" • ")
                        if (meta.isNotBlank()) {
                            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (set.isPR) PRBadge()
                    IconButton(onClick = { editingSet = set }) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton(onClick = { onDeleteSet(set.id) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        // FEATURE 2 — One Tap Set Completion
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (canRepeat) {
                PrimaryButton(
                    text = "Repeat ${formatWeight(suggestedSet!!.weight, useKg)}×${suggestedSet.reps}",
                    modifier = Modifier.weight(1.35f)
                ) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onRepeatLastSet() }
                SecondaryButton("Add Set", Modifier.weight(0.85f)) { showSetDialog = true }
            } else {
                PrimaryButton("Add Set", Modifier.weight(1f)) { showSetDialog = true }
            }
        }
    }

    if (showNotesDialog) {
        var notes by remember { mutableStateOf(entry.sessionExercise.notes) }
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text(entry.exercise.name + " notes") },
            text = {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Elbows tucked / slow eccentric …") },
                    minLines = 2
                )
            },
            confirmButton = { TextButton(onClick = { onSaveNotes(notes); showNotesDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showNotesDialog = false }) { Text("Cancel") } }
        )
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
            title = "Log Set",
            initialSet = suggestedSet,
            useKg = useKg,
            autoFocusField = autoFocusField,
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

// ... rest of file: SupersetDialog, AddOrEditSetDialog, ExercisePickerDialog, CreateTemplateDialog, TemplateTargetDialog, SectionTitle, supersetLabel, elapsed, toCleanString, plus new WorkoutCompletionDialog

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
    autoFocusField: String = "",
    onDismiss: () -> Unit,
    onSave: (Double, Int, String, Double?, String?) -> Unit
) {
    var weight by remember(initialSet?.id) { mutableStateOf(initialSet?.weight?.toCleanString().orEmpty()) }
    var reps by remember(initialSet?.id) { mutableStateOf(initialSet?.reps?.toString().orEmpty()) }
    // Phase 1: open the chosen field focused with the numeric keyboard up so the
    // user can begin typing immediately (no extra tap).
    val weightFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val repsFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        when (autoFocusField) {
            "weight" -> { weightFocus.requestFocus(); keyboard?.show() }
            "reps" -> { repsFocus.requestFocus(); keyboard?.show() }
            else -> Unit
        }
    }
    var setType by remember(initialSet?.id) { mutableStateOf(initialSet?.setType ?: SetType.WORKING) }
    var rpeValueState by remember(initialSet?.id) { mutableStateOf(initialSet?.rpe) }
    var tempoOption by remember(initialSet?.id) { mutableStateOf(com.replog.domain.logging.TempoPresets.optionForNotation(initialSet?.tempo)) }
    var customTempo by remember(initialSet?.id) { mutableStateOf(if (tempoOption.isCustom) initialSet?.tempo.orEmpty() else "") }
    var showAdvanced by remember { mutableStateOf(initialSet?.rpe != null || !initialSet?.tempo.isNullOrBlank() || initialSet?.setType != SetType.WORKING) }

    val weightValue = weight.toDoubleOrNull() ?: 0.0
    val repsValue = reps.toIntOrNull() ?: 0
    val valid = weightValue >= 0 && repsValue > 0
    val rpeValue = rpeValueState?.coerceIn(1.0, 10.0)
    val tempoToSave = com.replog.domain.logging.TempoPresets.resolveNotation(tempoOption, customTempo)

    fun bumpWeight(delta: Double) {
        val current = weight.toDoubleOrNull() ?: 0.0
        val next = (current + delta).coerceAtLeast(0.0)
        weight = if (next % 1.0 == 0.0) next.toInt().toString() else "%.2f".format(next).trimEnd('0').trimEnd('.')
    }

    fun bumpReps(delta: Int) {
        val current = reps.toIntOrNull() ?: 0
        reps = (current + delta).coerceAtLeast(0).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    NumberInputField(
                        weight, "Weight", Modifier.weight(1.15f), if (useKg) "kg" else "lb",
                        focusRequester = weightFocus,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { repsFocus.requestFocus() })
                    ) { v -> weight = v.filter { it.isDigit() || it == '.' } }
                    NumberInputField(
                        reps, "Reps", Modifier.weight(0.85f),
                        focusRequester = repsFocus,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                            if (weightValue >= 0 && repsValue > 0) { onSave(weightValue, repsValue, setType, rpeValue, tempoToSave) }
                        })
                    ) { v -> reps = v.filter { it.isDigit() } }
                }
                // Weight quick-add
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-2.5 to "-2.5", 1.25 to "+1.25", 2.5 to "+2.5", 5.0 to "+5").forEach { (delta, label) ->
                        TextButton(onClick = { bumpWeight(delta) }) { Text(label, fontWeight = FontWeight.Bold) }
                    }
                }
                // Reps quick-add (#5)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-1 to "-1", 1 to "+1", 2 to "+2", 5 to "+5").forEach { (delta, label) ->
                        TextButton(onClick = { bumpReps(delta) }) { Text("$label rep", fontWeight = FontWeight.Bold) }
                    }
                }

                if (!showAdvanced) {
                    TextButton(onClick = { showAdvanced = true }) { Text("Set type / RPE / Tempo…") }
                } else {
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

                    // RPE preset chips (#7)
                    Text("Effort (RPE)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.replog.domain.logging.RpePresets.OPTIONS.forEach { option ->
                            FilterChip(
                                selected = rpeValueState == option.value,
                                onClick = { rpeValueState = option.value },
                                label = { Text(option.label) }
                            )
                        }
                    }

                    // Tempo preset chips (#6) + custom field
                    Text("Tempo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.replog.domain.logging.TempoPresets.OPTIONS.forEach { option ->
                            val suffix = option.notation?.let { " ($it)" } ?: ""
                            FilterChip(
                                selected = tempoOption.key == option.key,
                                onClick = { tempoOption = option },
                                label = { Text(option.label + suffix) }
                            )
                        }
                    }
                    if (tempoOption.isCustom) {
                        OutlinedTextField(
                            value = customTempo,
                            onValueChange = { customTempo = it.take(12) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Custom tempo") },
                            placeholder = { Text("e.g. 3-1-1") },
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(weightValue, repsValue, setType, rpeValue, tempoToSave) }
            ) { Text("Complete", fontWeight = FontWeight.Bold) }
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
    val filtered = remember(q, exercises) {
        exercises.filter {
            q.isBlank() || it.name.contains(q, true) || it.category.contains(q, true) || it.equipment.contains(q, true) || it.movementPattern.contains(q, true)
        }
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
                    itemsIndexed(filtered, key = { _, ex -> ex.id }) { _, ex ->
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutCompletionDialog(summary: WorkoutSummary, useKg: Boolean, onRate: (Int) -> Unit, onDismiss: () -> Unit) {
    // Session Rating (#10): 5 = Amazing ... 1 = Terrible.
    var rating by remember(summary.sessionId) { mutableStateOf(0) }
    val ratingLabels = listOf(5 to "Amazing", 4 to "Good", 3 to "Average", 2 to "Poor", 1 to "Terrible")
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Workout Complete", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(summary.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("How was this workout?", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ratingLabels.forEach { (value, label) ->
                        FilterChip(
                            selected = rating == value,
                            onClick = { rating = value; onRate(value) },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Duration", formatDuration(summary.durationMillis), Modifier.weight(1f))
                    StatCard("Sets", summary.setCount.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Reps", summary.repCount.toString(), Modifier.weight(1f))
                    StatCard("Volume", formatWeight(summary.volume, useKg), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("PBs", summary.prCount.toString(), Modifier.weight(1f))
                    StatCard("Score", "${summary.qualityScore}/100", Modifier.weight(1f))
                }
                if (summary.prCount > 0) {
                    Text("🏆 New Personal Best${if(summary.prCount>1) "s" else ""} – strong session!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text("Exercises: ${summary.exerciseCount} • Targets hit: ${summary.targetHitCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            val shareContext = androidx.compose.ui.platform.LocalContext.current
            TextButton(onClick = {
                com.replog.util.ShareCardRenderer.renderAndShare(
                    context = shareContext,
                    headline = "Workout Complete",
                    subtitle = summary.name,
                    stats = listOf(
                        com.replog.util.ShareStat("Volume", formatWeight(summary.volume, useKg)),
                        com.replog.util.ShareStat("Sets", summary.setCount.toString()),
                        com.replog.util.ShareStat("Duration", formatDuration(summary.durationMillis)),
                        com.replog.util.ShareStat("Score", "${summary.qualityScore}/100")
                    ),
                    footnote = if (summary.prCount > 0) "🏆 ${summary.prCount} new PB${if (summary.prCount > 1) "s" else ""}!" else null
                )
            }) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share Progress")
            }
        }
    )
}

private fun formatDuration(durationMillis: Long): String {
    val minutes = (durationMillis / 60_000).coerceAtLeast(0)
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}

// Template dialog – compact version kept
@Composable private fun CreateTemplateDialog(exercises: List<Exercise>,initialTemplate: TemplateWithExercises?,onDismiss: () -> Unit,onSave: (String, List<TemplateExerciseDraft>) -> Unit) { var name by remember(initialTemplate?.template?.id) { mutableStateOf(initialTemplate?.template?.name.orEmpty()) }; var query by remember { mutableStateOf("") }; var drafts by remember(initialTemplate?.template?.id) { mutableStateOf(initialTemplate?.exercises?.sortedBy { it.templateExercise.orderIndex }?.map { TemplateExerciseDraft(it.exercise, it.templateExercise.defaultSets, it.templateExercise.targetReps, it.templateExercise.targetWeight) }.orEmpty()) }; var editingDraft by remember { mutableStateOf<TemplateExerciseDraft?>(null) }; val filtered = remember(query, exercises, drafts) { exercises.filter { query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) || it.equipment.contains(query, true) || it.movementPattern.contains(query, true) } }; fun move(id: Int, direction: Int) { val index = drafts.indexOfFirst { it.exercise.id == id }; val newIndex = (index + direction).coerceIn(0, drafts.lastIndex); if (index >= 0 && newIndex != index) { drafts = drafts.toMutableList().also { list -> val item = list.removeAt(index); list.add(newIndex, item) } } }; fun upsertDraft(draft: TemplateExerciseDraft) { drafts = if (drafts.any { it.exercise.id == draft.exercise.id }) { drafts.map { if (it.exercise.id == draft.exercise.id) draft else it } } else { drafts + draft } }; AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initialTemplate == null) "Create template" else "Edit template") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Template name") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(query, { query = it }, label = { Text("Search exercises") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth()); LazyColumn(Modifier.heightIn(max = 120.dp)) { items(drafts.size) { index -> val draft = drafts[index]; ListItem(headlineContent = { Text(draft.exercise.name) }, supportingContent = { Text("${draft.defaultSets} sets × ${draft.targetReps} reps" + (draft.targetWeight?.let { " @ ${formatWeight(it, true)}" } ?: "")) }, trailingContent = { Row { TextButton(onClick = { move(draft.exercise.id, -1) }, enabled = index > 0) { Text("Up") }; TextButton(onClick = { move(draft.exercise.id, 1) }, enabled = index < drafts.lastIndex) { Text("Down") } } }, modifier = Modifier.clickable { editingDraft = draft }); HorizontalDivider() } }; Spacer(Modifier.height(8.dp)); LazyColumn(Modifier.heightIn(max = 230.dp)) { itemsIndexed(filtered, key = { _, ex -> ex.id }) { _, exercise -> val selected = drafts.any { it.exercise.id == exercise.id }; ListItem(headlineContent = { Text(exercise.name) }, supportingContent = { Text("${exercise.category} • ${exercise.equipment}") }, leadingContent = { Checkbox(checked = selected, onCheckedChange = null) }, modifier = Modifier.fillMaxWidth().clickable { drafts = if (selected) drafts.filterNot { it.exercise.id == exercise.id } else drafts + TemplateExerciseDraft(exercise) }); HorizontalDivider() } } } }, confirmButton = { TextButton(enabled = name.isNotBlank() && drafts.isNotEmpty(), onClick = { onSave(name, drafts) }) { Text(if (initialTemplate == null) "Create" else "Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }); editingDraft?.let { draft -> TemplateTargetDialog(draft = draft, onDismiss = { editingDraft = null }, onSave = { upsertDraft(it); editingDraft = null }) } }
@Composable private fun TemplateTargetDialog(draft: TemplateExerciseDraft, onDismiss: () -> Unit, onSave: (TemplateExerciseDraft) -> Unit) { var sets by remember(draft.exercise.id) { mutableStateOf(draft.defaultSets.toString()) }; var reps by remember(draft.exercise.id) { mutableStateOf(draft.targetReps.toString()) }; var weight by remember(draft.exercise.id) { mutableStateOf(draft.targetWeight?.toCleanString().orEmpty()) }; AlertDialog(onDismissRequest = onDismiss, title = { Text(draft.exercise.name) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Programmed template target", color = MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { NumberInputField(sets, "Sets", Modifier.weight(1f)) { sets = it.filter { char -> char.isDigit() } }; NumberInputField(reps, "Reps", Modifier.weight(1f)) { reps = it.filter { char -> char.isDigit() } } }; NumberInputField(weight, "Target weight", Modifier.fillMaxWidth(), "optional") { value -> weight = value.filter { it.isDigit() || it == '.' } } } }, confirmButton = { TextButton(enabled = (sets.toIntOrNull() ?: 0) > 0 && (reps.toIntOrNull() ?: 0) > 0, onClick = { onSave(draft.copy(defaultSets = sets.toIntOrNull() ?: draft.defaultSets, targetReps = reps.toIntOrNull() ?: draft.targetReps, targetWeight = weight.toDoubleOrNull())) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }

@Composable private fun SectionTitle(title: String) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
private fun supersetLabel(entry: SessionExerciseWithSets, entries: List<SessionExerciseWithSets>): String? { val group = entry.sessionExercise.supersetGroup ?: return null; val grouped = entries.filter { it.sessionExercise.supersetGroup == group }.sortedBy { it.sessionExercise.orderIndex }; val index = grouped.indexOfFirst { it.sessionExercise.id == entry.sessionExercise.id }.takeIf { it >= 0 } ?: 0; return "$group${index + 1}" }
private fun elapsed(start: Long?): String { if (start == null) return "00:00"; val sec = ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(0); return "%02d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60) }
private fun Double.toCleanString(): String = if (this % 1.0 == 0.0) toInt().toString() else "%.2f".format(this).trimEnd('0').trimEnd('.')

/** Write a .replogtemplate file to the cache and open the OS share sheet. */
private fun shareTemplateFile(context: android.content.Context, fileName: String, json: String) {
    val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
    val file = java.io.File(dir, fileName)
    file.writeText(json)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share workout template"))
}
