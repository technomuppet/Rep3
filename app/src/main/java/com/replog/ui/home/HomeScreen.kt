package com.replog.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.data.model.SessionWithExercises
import com.replog.ui.components.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onStartWorkout: () -> Unit,
    onViewHistory: () -> Unit,
    onStartRecommendedWorkout: () -> Unit = {},
    onViewRecoveryGuidance: () -> Unit = {},
    onOpenCoachHistory: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    coachViewModel: com.replog.ui.coach.CoachViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val coachState by coachViewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh(); coachViewModel.loadRecommendation(force = false) }
    LazyColumn(Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Today", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold); Text("Your training dashboard.", color = MaterialTheme.colorScheme.onSurfaceVariant) }

        // What to train today (recommendation + recovery) — the core of the dashboard.
        item {
            com.replog.ui.coach.SmartCoachCard(
                state = coachState,
                onStart = {
                    coachViewModel.acceptRecommendation(
                        onLaunchWorkout = onStartRecommendedWorkout,
                        onRestDay = onViewRecoveryGuidance
                    )
                },
                onDismiss = { coachViewModel.dismissRecommendation() },
                onRefresh = { coachViewModel.loadRecommendation(force = true) }
            )
        }

        // Weekly progress + streak
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("This week", "${state.sessionsThisWeek} workouts", Modifier.weight(1f))
                StatCard("Week volume", formatWeight(state.volumeThisWeek), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Day streak", if (state.dayStreak > 0) "🔥 ${state.dayStreak}" else "—", Modifier.weight(1f))
                StatCard("Total workouts", state.sessionCount.toString(), Modifier.weight(1f))
            }
        }

        // Coaching insight + last PR (single, not a full feed — that lives in History/Progress)
        item { HomeInsightCard(state.insight) }
        item { Text("Last PR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            val lastPr = state.recentPRs.firstOrNull()
            if (lastPr == null) {
                RepLogCard { Text("PRs will appear here when you beat previous bests.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                RepLogCard { Row(verticalAlignment = Alignment.CenterVertically) { PRBadge(); Spacer(Modifier.width(10.dp)); Text("${formatWeight(lastPr.weight)} × ${lastPr.reps} reps", fontWeight = FontWeight.Bold) } }
            }
        }

        // Lightweight navigation to the detail areas (their full job lives elsewhere).
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { SecondaryButton("History", Modifier.weight(1f), onClick = onViewHistory); SecondaryButton("Coach History", Modifier.weight(1f), onClick = onOpenCoachHistory) } }
    }
}

@Composable
private fun HomeInsightCard(insight: HomeInsight) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(insight.title, fontWeight = FontWeight.Bold)
            Text(insight.message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RecentWorkoutCard(session: SessionWithExercises) = RepLogCard {
    val duration = session.session.endTime?.let { ((it - session.session.startTime) / 60000.0).roundToInt().toString() + " min" } ?: "In progress"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(session.session.templateName ?: "Workout", fontWeight = FontWeight.Bold); Text(SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(session.session.startTime)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); Text("${session.exercises.size} exercises • ${session.exercises.sumOf { it.sets.size }} sets", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp)); Text(duration, style = MaterialTheme.typography.bodySmall)
    }
}
