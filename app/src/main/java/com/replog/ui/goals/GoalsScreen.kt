package com.replog.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.data.model.Exercise
import com.replog.data.repository.GoalWithForecast
import com.replog.domain.goals.GoalStatus
import com.replog.ui.components.EmptyState
import com.replog.ui.components.LoadingState
import com.replog.ui.components.NumberInputField
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard

@Composable
fun GoalsScreen(
    contentPadding: PaddingValues,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Goals", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Set a target and RepLog forecasts when you'll hit it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { PrimaryButton("Add a goal") { showAdd = true } }

        if (state.isLoading && state.goals.isEmpty()) {
            item { LoadingState("Loading goals") }
        } else if (state.goals.isEmpty()) {
            item { EmptyState("No goals yet", "Add a strength, rep or bodyweight goal to see your forecast and next milestone.") }
        } else {
            items(state.goals) { gwf ->
                GoalCard(gwf, useKg = state.useKg, onDelete = { viewModel.deleteGoal(gwf.goal) })
            }
        }
    }

    if (showAdd) {
        AddGoalDialog(
            exercises = state.exercises,
            useKg = state.useKg,
            onDismiss = { showAdd = false },
            onCreateStrength = { ex, t -> viewModel.createStrengthGoal(ex, t); showAdd = false },
            onCreateReps = { ex, r -> viewModel.createRepGoal(ex, r); showAdd = false },
            onCreateBodyweight = { t -> viewModel.createBodyweightGoal(t); showAdd = false }
        )
    }
}

@Composable
private fun GoalCard(gwf: GoalWithForecast, useKg: Boolean, onDelete: () -> Unit) = RepLogCard {
    val f = gwf.forecast
    val statusColor = when (f.status) {
        GoalStatus.ACHIEVED -> com.replog.ui.theme.RepLogSuccess
        GoalStatus.AHEAD -> com.replog.ui.theme.RepLogSuccess
        GoalStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
        GoalStatus.STALLED -> MaterialTheme.colorScheme.error
        GoalStatus.NO_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(gwf.goal.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(progress = { f.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = statusColor)
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${f.progressPercent}%", fontWeight = FontWeight.Bold)
        Text(f.etaText, fontWeight = FontWeight.SemiBold, color = statusColor)
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Now ${fmtNum(f.current)} → target ${fmtNum(f.target)}" +
            (f.nextMilestone?.let { " • next ${fmtNum(it)}" } ?: ""),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(f.summaryLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AddGoalDialog(
    exercises: List<Exercise>,
    useKg: Boolean,
    onDismiss: () -> Unit,
    onCreateStrength: (Exercise, Double) -> Unit,
    onCreateReps: (Exercise, Int) -> Unit,
    onCreateBodyweight: (Double) -> Unit
) {
    var kind by remember { mutableStateOf("Strength") } // Strength | Reps | Bodyweight
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Exercise?>(null) }
    var targetText by remember { mutableStateOf("") }

    val filtered = remember(query, exercises) {
        if (query.isBlank()) exercises.take(20)
        else exercises.filter { it.name.contains(query, true) }.take(20)
    }
    val unit = if (useKg) "kg" else "lb"
    val needsExercise = kind == "Strength" || kind == "Reps"
    val target = targetText.toDoubleOrNull()
    val valid = target != null && target > 0 && (!needsExercise || selected != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Strength", "Reps", "Bodyweight").forEach { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k; selected = null; targetText = "" }, label = { Text(k) })
                    }
                }

                if (needsExercise) {
                    OutlinedTextField(
                        value = selected?.name ?: query,
                        onValueChange = { query = it; selected = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Exercise") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                    if (selected == null) {
                        LazyColumn(Modifier.heightIn(max = 180.dp)) {
                            itemsIndexed(filtered, key = { _, e -> e.id }) { _, ex ->
                                ListItem(
                                    headlineContent = { Text(ex.name) },
                                    supportingContent = { Text("${ex.category} • ${ex.equipment}") },
                                    modifier = Modifier.fillMaxWidth().clickable { selected = ex; query = ex.name }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                val label = when (kind) {
                    "Reps" -> "Target reps"
                    "Bodyweight" -> "Target bodyweight"
                    else -> "Target 1RM"
                }
                val suffix = if (kind == "Reps") "reps" else unit
                NumberInputField(targetText, label, Modifier.fillMaxWidth(), suffix) { v ->
                    targetText = v.filter { it.isDigit() || it == '.' }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val t = target ?: return@TextButton
                    when (kind) {
                        "Strength" -> selected?.let { onCreateStrength(it, t) }
                        "Reps" -> selected?.let { onCreateReps(it, t.toInt()) }
                        "Bodyweight" -> onCreateBodyweight(t)
                    }
                }
            ) { Text("Create", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun fmtNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
