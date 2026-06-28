package com.replog.ui.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.data.repository.MuscleBalanceRow
import com.replog.ui.components.EmptyState
import com.replog.ui.components.LoadingState
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton
import com.replog.ui.components.StatCard

@Composable
fun MuscleBalanceScreen(
    contentPadding: PaddingValues,
    onWorkoutStarted: () -> Unit,
    viewModel: MuscleBalanceViewModel = hiltViewModel()
) {
    val data by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    val dismissed by viewModel.dismissed.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Muscle Balance", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Where your weekly volume is balanced, and what to bring up.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        message?.let { msg ->
            item {
                RepLogCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, modifier = Modifier.weight(1f))
                        SecondaryButton("OK", Modifier.width(90.dp)) { viewModel.clearMessage() }
                    }
                }
            }
        }
        val d = data
        when {
            d == null -> item { LoadingState("Analysing your volume") }
            !d.hasData -> item { EmptyState("Not enough data yet", "Finish a few more workouts to see your muscle balance.") }
            else -> {
                item {
                    RepLogCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Muscle balance", fontWeight = FontWeight.Bold)
                                Text("Higher is more balanced volume across groups.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${d.balanceScore}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { d.balanceScore / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    }
                }
                if (d.weakest.isNotEmpty() || d.strongest.isNotEmpty()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("Weakest", d.weakest.firstOrNull() ?: "—", Modifier.weight(1f))
                            StatCard("Strongest", d.strongest.firstOrNull() ?: "—", Modifier.weight(1f))
                        }
                    }
                }
                if (d.estimatedWeeksToBalance > 0) {
                    item {
                        RepLogCard {
                            Text("Estimated time to rebalance", fontWeight = FontWeight.Bold)
                            Text("~${d.estimatedWeeksToBalance} week${if (d.estimatedWeeksToBalance == 1) "" else "s"} of consistent training on the groups below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Text("By muscle group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(d.rows.filter { it.muscleGroup !in dismissed }, key = { it.muscleGroup }) { row ->
                    MuscleBalanceCard(
                        row = row,
                        onStart = { viewModel.startWorkout(row.muscleGroup) { onWorkoutStarted() } },
                        onAddTemplate = { viewModel.addToTemplate(row.muscleGroup) },
                        onDismiss = { viewModel.dismiss(row.muscleGroup) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleBalanceCard(
    row: MuscleBalanceRow,
    onStart: () -> Unit,
    onAddTemplate: () -> Unit,
    onDismiss: () -> Unit
) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(row.muscleGroup, fontWeight = FontWeight.Bold)
            Text(
                "${row.weeklySets} sets/week • optimal ${row.optimalLow}-${row.optimalHigh}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(statusLabel(row.status), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = statusColor(row.status))
    }
    val needsWork = row.status == "UNDER" || row.status == "NONE"
    if (needsWork) {
        Spacer(Modifier.height(6.dp))
        Text("Gap severity: ${row.severity}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        if (row.recommendedExercises.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Recommended: ${row.recommendedExercises.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Start", Modifier.weight(1f), onClick = onStart)
            SecondaryButton("Add to Template", Modifier.weight(1.2f), onClick = onAddTemplate)
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Dismiss recommendation") }
    }
}

@Composable
private fun statusLabel(status: String): String = when (status) {
    "UNDER" -> "Below optimal"
    "IN_RANGE" -> "In range"
    "ABOVE" -> "Above optimal"
    else -> "Not trained"
}

@Composable
private fun statusColor(status: String) = when (status) {
    "IN_RANGE" -> MaterialTheme.colorScheme.primary
    "ABOVE" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}
