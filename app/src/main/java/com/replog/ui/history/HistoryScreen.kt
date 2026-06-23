package com.replog.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.data.model.SessionWithExercises
import com.replog.ui.components.EmptyState
import com.replog.ui.components.LoadingState
import com.replog.ui.components.PRBadge
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.StatCard
import com.replog.ui.components.formatWeight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val completed = state.sessions.filter { it.session.endTime != null }
    var pendingDelete by remember { mutableStateOf<SessionWithExercises?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("History", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Calendar view, session details and PRs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.isLoading && state.sessions.isEmpty()) {
            item { LoadingState("Loading history") }
        } else if (state.sessions.isEmpty()) {
            item { EmptyState("No history yet", "Finished workouts will appear here.") }
        } else {
            item { CalendarSummaryCard(sessions = completed) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("This month", sessionsThisMonth(completed).toString(), Modifier.weight(1f))
                    StatCard("Month volume", formatWeight(monthVolume(completed)), Modifier.weight(1f))
                }
            }

            val grouped = state.sessions.groupBy { dayStart(it.session.startTime) }.toSortedMap(compareByDescending { it })
            grouped.forEach { (day, sessions) ->
                item {
                    Text(formatDayHeader(day), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(sessions, key = { it.session.id }) { session ->
                    SessionHistoryCard(session) { pendingDelete = session }
                }
            }
        }
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete workout?") },
            text = { Text("This permanently deletes the selected workout and all logged sets.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(session.session.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CalendarSummaryCard(sessions: List<SessionWithExercises>) = RepLogCard {
    val calendar = Calendar.getInstance()
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    val sessionDays = sessions.map { dayOfMonth(it.session.startTime) }.toSet()
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text("Training days are highlighted.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..daysInMonth).chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEach { day ->
                    val trained = day in sessionDays
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (trained) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (trained) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (trained) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(session: SessionWithExercises, onDelete: () -> Unit) = RepLogCard {
    val duration = session.session.endTime?.let { ((it - session.session.startTime) / 60000.0).roundToInt().toString() + " min" } ?: "In progress"
    val volume = session.exercises.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(session.session.templateName ?: "Workout", fontWeight = FontWeight.Bold)
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.session.startTime)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$duration • ${session.exercises.size} exercises • ${session.exercises.sumOf { it.sets.size }} sets • ${formatWeight(volume)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
    }

    session.exercises.forEach { ex ->
        Spacer(Modifier.height(8.dp))
        Text(ex.exercise.name, fontWeight = FontWeight.SemiBold)
        ex.sets.forEach { set ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Set ${set.setNumber}: ${formatWeight(set.weight)} × ${set.reps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        listOf(set.setType, set.rpe?.let { "RPE ${"%.1f".format(it)}" }, set.tempo?.takeIf { it.isNotBlank() }?.let { "Tempo $it" }).filterNotNull().joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (set.isPR) PRBadge()
            }
        }
    }
}

private fun dayStart(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun dayOfMonth(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
private fun formatDayHeader(timestamp: Long): String = SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(timestamp))

private fun sessionsThisMonth(sessions: List<SessionWithExercises>): Int {
    val now = Calendar.getInstance()
    return sessions.count {
        val cal = Calendar.getInstance().apply { timeInMillis = it.session.startTime }
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }
}

private fun monthVolume(sessions: List<SessionWithExercises>): Double {
    val now = Calendar.getInstance()
    return sessions.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.session.startTime }
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }.sumOf { session -> session.exercises.sumOf { entry -> entry.sets.sumOf { it.weight * it.reps } } }
}
