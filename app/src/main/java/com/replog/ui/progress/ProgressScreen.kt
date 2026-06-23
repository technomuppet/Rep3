package com.replog.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.data.model.BodyweightLog
import com.replog.ui.components.EmptyState
import com.replog.ui.components.LoadingState
import com.replog.ui.components.PRBadge
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton
import com.replog.ui.components.StatCard
import com.replog.ui.components.formatWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(
    contentPadding: PaddingValues,
    onOpenTrainingDna: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showBodyweightDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var pendingBodyweightDelete by remember { mutableStateOf<BodyweightLog?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Progress", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Your training signal across volume, PRs, strength and bodyweight.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item { PrimaryButton("View Training DNA") { onOpenTrainingDna() } }

        item {
            BodyweightCard(
                logs = state.bodyweights,
                goal = state.bodyweightGoal,
                useKg = state.useKg,
                onAdd = { showBodyweightDialog = true },
                onSetGoal = { showGoalDialog = true },
                onDelete = { log -> pendingBodyweightDelete = log }
            )
        }

        if (state.isLoading && state.totalWorkouts == 0) {
            item { LoadingState("Loading progress") }
        } else if (state.totalWorkouts == 0) {
            item { EmptyState("No completed workouts yet", "Finish workouts to unlock progress charts and rankings.") }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Workouts", state.totalWorkouts.toString(), Modifier.weight(1f))
                    StatCard("Sets", state.totalSets.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Volume", formatWeight(state.totalVolume, state.useKg), Modifier.weight(1f))
                    StatCard("Reps", state.totalReps.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("PRs", state.totalPRs.toString(), Modifier.weight(1f))
                    StatCard("Tonnes", "%.1f".format(state.totalVolume / 1000.0), Modifier.weight(1f))
                }
            }

            item { StreakCard(streak = state.currentStreakDays) }
            item { NextBestActionsCard(actions = state.nextBestActions) }
            item { TrainingDnaCard(dna = state.trainingDna) }
            item { RecoveryCard(recovery = state.recoveryInsight) }
            if (state.forecasts.isNotEmpty()) {
                item { ForecastCard(forecasts = state.forecasts, useKg = state.useKg) }
            }
            if (state.plateauAlerts.isNotEmpty()) {
                item { PlateauCard(alerts = state.plateauAlerts, useKg = state.useKg) }
            }
            item { BalanceCard(title = "Movement balance", points = state.movementBalance, useKg = state.useKg) }
            item { BalanceCard(title = "Muscle volume distribution", points = state.muscleBalance, useKg = state.useKg) }
            item { MilestonesCard(milestones = state.milestones) }
            item { WeeklyVolumeCard(points = state.weeklyVolume, useKg = state.useKg) }
            item { Text("Estimated 1RM leaderboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(state.rankings.take(10), key = { it.exerciseName }) { ranking ->
                RankingCard(ranking = ranking, useKg = state.useKg)
            }
        }
    }

    pendingBodyweightDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { pendingBodyweightDelete = null },
            title = { Text("Delete bodyweight entry?") },
            text = { Text("This removes ${formatWeight(log.weight, state.useKg)} from ${formatDate(log.timestamp)}.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBodyweight(log.id)
                    pendingBodyweightDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingBodyweightDelete = null }) { Text("Cancel") } }
        )
    }

    if (showGoalDialog) {
        BodyweightGoalDialog(
            currentGoal = state.bodyweightGoal,
            useKg = state.useKg,
            onDismiss = { showGoalDialog = false },
            onSave = { goal ->
                viewModel.setBodyweightGoal(goal)
                showGoalDialog = false
            }
        )
    }

    if (showBodyweightDialog) {
        AddBodyweightDialog(
            useKg = state.useKg,
            onDismiss = { showBodyweightDialog = false },
            onSave = { weight, note ->
                viewModel.addBodyweight(weight, note)
                showBodyweightDialog = false
            }
        )
    }
}

@Composable
private fun NextBestActionsCard(actions: List<ActionRecommendation>) = RepLogCard {
    Text("Next best action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (actions.isEmpty()) {
        Text("Log more workouts to unlock personalised recommendations.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        actions.forEach { action ->
            Text("${action.priority}: ${action.title}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(action.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TrainingDnaCard(dna: TrainingDnaSummary) = RepLogCard {
    Text("Training DNA™", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(dna.profileNote, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("Rep range", dna.preferredRepRange, Modifier.weight(1f))
        StatCard("Hard sets/wk", "%.1f".format(dna.hardSetsPerWeek), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("Top pattern", dna.topMovementPattern, Modifier.weight(1f))
        StatCard("Avg RPE", dna.averageRpe?.let { "%.1f".format(it) } ?: "—", Modifier.weight(1f))
    }
    Text("Top exercise: ${dna.topExercise}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ForecastCard(forecasts: List<StrengthForecast>, useKg: Boolean) = RepLogCard {
    Text("Strength forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text("Projected estimated 1RM if recent trend continues.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    forecasts.forEach { forecast ->
        Text(forecast.exerciseName, fontWeight = FontWeight.Bold)
        Text(
            "${formatWeight(forecast.currentEstimatedOneRm, useKg)} → ${formatWeight(forecast.projectedEstimatedOneRm, useKg)} in ${forecast.weeks} weeks • ${forecast.confidence} confidence",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Likely range ${formatWeight(forecast.lowerBoundEstimatedOneRm, useKg)}–${formatWeight(forecast.upperBoundEstimatedOneRm, useKg)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        forecast.targetDateMillis?.let { date ->
            Text(
                "Next target ${formatWeight(forecast.targetEstimatedOneRm, useKg)} around ${formatDate(date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            "Trend ${if (forecast.trendPerWeek >= 0) "+" else ""}${"%.2f".format(forecast.trendPerWeek)}/week",
            style = MaterialTheme.typography.bodySmall,
            color = if (forecast.trendPerWeek >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RecoveryCard(recovery: RecoveryInsight) = RepLogCard {
    Row {
        Icon(Icons.Default.ShowChart, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 12.dp)) {
            Text("Recovery signal", fontWeight = FontWeight.Bold)
            Text("${recovery.label} • ${recovery.score}/100", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(recovery.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(progress = recovery.score / 100f, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PlateauCard(alerts: List<PlateauAlert>, useKg: Boolean) = RepLogCard {
    Text("Plateau watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text("Lifts that may need programming attention.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    alerts.forEach { alert ->
        Text(alert.exerciseName, fontWeight = FontWeight.Bold)
        Text(
            "No estimated 1RM improvement for ~${alert.weeksStalled} weeks • best ${formatWeight(alert.bestEstimatedOneRm, useKg)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(alert.suggestion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BalanceCard(title: String, points: List<BalancePoint>, useKg: Boolean) = RepLogCard {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (points.isEmpty()) {
        Text("Log more workouts to build this analysis.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        points.take(6).forEach { point ->
            Row {
                Text(point.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("${(point.percentage * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = point.percentage.toFloat(), modifier = Modifier.fillMaxWidth())
            Text(formatWeight(point.volume, useKg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StreakCard(streak: Int) = RepLogCard {
    Row {
        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 12.dp)) {
            Text("Workout streak", fontWeight = FontWeight.Bold)
            Text(
                if (streak > 0) "$streak training day${if (streak == 1) "" else "s"} in a row" else "Finish a workout today to start a streak.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MilestonesCard(milestones: List<Milestone>) = RepLogCard {
    Text("Milestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    milestones.forEach { milestone ->
        Row {
            Icon(
                Icons.Default.EmojiEvents,
                null,
                tint = if (milestone.achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(milestone.title, fontWeight = FontWeight.SemiBold)
                Text(milestone.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(
                    progress = milestone.progress.toFloat(),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun BodyweightCard(
    logs: List<BodyweightLog>,
    goal: Double?,
    useKg: Boolean,
    onAdd: () -> Unit,
    onSetGoal: () -> Unit,
    onDelete: (BodyweightLog) -> Unit
) = RepLogCard {
    Row {
        Icon(Icons.Default.MonitorWeight, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Bodyweight", fontWeight = FontWeight.Bold)
            Text(
                logs.lastOrNull()?.let { "Latest ${formatWeight(it.weight, useKg)} • ${formatDate(it.timestamp)}" } ?: "Track bodyweight for strength ratios.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    if (goal != null) {
        Text("Goal ${formatWeight(goal, useKg)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
    }
    if (logs.size >= 2) BodyweightChart(logs = logs.takeLast(20), goal = goal) else Text("Add at least two entries to show a trend.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton("Log", Modifier.weight(1f), onClick = onAdd)
        SecondaryButton("Goal", Modifier.weight(1f), onClick = onSetGoal)
        logs.lastOrNull()?.let { latest -> SecondaryButton("Delete", Modifier.weight(1f)) { onDelete(latest) } }
    }
}

@Composable
private fun BodyweightChart(logs: List<BodyweightLog>, goal: Double?) {
    val primary = MaterialTheme.colorScheme.tertiary
    val averageColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.error
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    val rolling = logs.mapIndexed { index, log ->
        val window = logs.subList((index - 2).coerceAtLeast(0), index + 1)
        log.timestamp to window.map { it.weight }.average()
    }
    val values = logs.map { it.weight } + rolling.map { it.second } + listOfNotNull(goal)
    val min = values.minOrNull() ?: 0.0
    val max = values.maxOrNull() ?: 1.0
    val range = (max - min).takeIf { it > 0.0 } ?: 1.0
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = "Bodyweight trend chart showing entries, rolling average and optional goal line" }
    ) {
        val left = 8.dp.toPx(); val right = size.width - 8.dp.toPx(); val top = 10.dp.toPx(); val bottom = size.height - 12.dp.toPx()
        val width = right - left; val height = bottom - top
        fun yFor(value: Double): Float = bottom - (((value - min) / range).toFloat() * height)
        repeat(3) { idx ->
            val y = top + height * (idx / 2f)
            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
        }
        goal?.let {
            val y = yFor(it)
            drawLine(goalColor, Offset(left, y), Offset(right, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        val offsets = logs.mapIndexed { index, log ->
            val x = left + width * (index.toFloat() / logs.lastIndex.coerceAtLeast(1))
            Offset(x, yFor(log.weight))
        }
        val avgOffsets = rolling.mapIndexed { index, point ->
            val x = left + width * (index.toFloat() / rolling.lastIndex.coerceAtLeast(1))
            Offset(x, yFor(point.second))
        }
        offsets.zipWithNext().forEach { (a, b) -> drawLine(primary, a, b, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round) }
        avgOffsets.zipWithNext().forEach { (a, b) -> drawLine(averageColor, a, b, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round) }
        offsets.forEach { drawCircle(primary, radius = 4.dp.toPx(), center = it) }
    }
    Text("Thin line: entries • thick line: rolling average", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WeeklyVolumeCard(points: List<WeeklyVolumePoint>, useKg: Boolean) = RepLogCard {
    Row {
        Icon(Icons.Default.ShowChart, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 12.dp)) {
            Text("Weekly volume", fontWeight = FontWeight.Bold)
            Text("Last ${points.size.coerceAtLeast(1)} training weeks", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(12.dp))
    if (points.size < 2) {
        Text("Complete workouts across multiple weeks to see the trend.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        VolumeChart(points = points)
        Spacer(Modifier.height(8.dp))
        Text(
            "${formatWeek(points.first().weekStartMillis)} → ${formatWeek(points.last().weekStartMillis)} • peak ${formatWeight(points.maxOf { it.volume }, useKg)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VolumeChart(points: List<WeeklyVolumePoint>) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    val max = points.maxOfOrNull { it.volume }?.takeIf { it > 0.0 } ?: 1.0

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = "Weekly training volume chart for recent weeks" }
    ) {
        val left = 8.dp.toPx(); val right = size.width - 8.dp.toPx(); val top = 12.dp.toPx(); val bottom = size.height - 18.dp.toPx()
        val width = right - left; val height = bottom - top
        repeat(4) { index ->
            val y = top + height * (index / 3f)
            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
        }
        val offsets = points.mapIndexed { index, point ->
            val x = left + width * (index.toFloat() / points.lastIndex.coerceAtLeast(1))
            val y = bottom - ((point.volume / max).toFloat() * height)
            Offset(x, y)
        }
        offsets.zipWithNext().forEach { (a, b) -> drawLine(primary, a, b, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round) }
        offsets.forEach { drawCircle(primary, radius = 5.dp.toPx(), center = it) }
    }
}

@Composable
private fun RankingCard(ranking: ExerciseRanking, useKg: Boolean) = RepLogCard {
    Row {
        Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(ranking.exerciseName, fontWeight = FontWeight.Bold)
            Text(
                "Best ${formatWeight(ranking.bestWeight, useKg)} • ${ranking.totalSets} sets • ${formatWeight(ranking.totalVolume, useKg)} volume",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (ranking.prCount > 0) PRBadge()
    }
    Spacer(Modifier.height(8.dp))
    Text("Est. 1RM ${formatWeight(ranking.bestEstimatedOneRm, useKg)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    ranking.strengthToBodyweight?.let {
        Text("Strength ratio ${"%.2f".format(it)}× bodyweight", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BodyweightGoalDialog(
    currentGoal: Double?,
    useKg: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit
) {
    var goal by remember(currentGoal) { mutableStateOf(currentGoal?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) }.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bodyweight goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Set an optional target line for your bodyweight trend.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Goal") },
                    suffix = { Text(if (useKg) "kg" else "lb") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(goal.toDoubleOrNull()) }) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(null) }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun AddBodyweightDialog(
    useKg: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, String?) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val valid = (weight.toDoubleOrNull() ?: 0.0) > 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log bodyweight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Weight") },
                    suffix = { Text(if (useKg) "kg" else "lb") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note optional") },
                    singleLine = true
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(weight.toDoubleOrNull() ?: 0.0, note) }) { Text("Save") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

private fun formatWeek(timestamp: Long): String = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
private fun formatDate(timestamp: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
