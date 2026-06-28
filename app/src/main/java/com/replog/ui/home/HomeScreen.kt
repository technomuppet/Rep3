package com.replog.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
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
    onOpenGoals: () -> Unit = {},
    onOpenTrainingDna: () -> Unit = {},
    onOpenQuickWorkouts: () -> Unit = {},
    onOpenRecoveryCentre: () -> Unit = {},
    onOpenMuscleBalance: () -> Unit = {},
    onOpenDnaEvolution: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    coachViewModel: com.replog.ui.coach.CoachViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val coachState by coachViewModel.state.collectAsState()
    val briefing by viewModel.briefing.collectAsState()
    val continueWorkout by viewModel.continueWorkout.collectAsState()
    val repLogScore by viewModel.repLogScore.collectAsState()
    var showTrainAnywayDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refresh(); viewModel.loadIntelligence(); coachViewModel.loadRecommendation(force = false) }
    LazyColumn(Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Today", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold); Text("Your training dashboard.", color = MaterialTheme.colorScheme.onSurfaceVariant) }

        // Priority 2: Continue Workout - the top card when a session is in progress.
        continueWorkout?.let { cw ->
            item { ContinueWorkoutCard(cw, onContinue = onStartWorkout) }
        }

        // Priority 1: Today's Briefing - the unified intelligence card.
        briefing?.let { b ->
            item { TodaysBriefingCard(b, onOpenRecovery = onOpenRecoveryCentre) }
        }

        // Sprint 8 P5: RepLog Score with explainable component breakdown.
        repLogScore?.let { s ->
            item { RepLogScoreCard(s) }
        }

        // Sprint 9: intelligence hubs (Recovery Centre / Muscle Balance / DNA Evolution).
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntelligenceNavCard("Recovery", Modifier.weight(1f), onOpenRecoveryCentre)
                IntelligenceNavCard("Muscle Balance", Modifier.weight(1f), onOpenMuscleBalance)
                IntelligenceNavCard("DNA", Modifier.weight(1f), onOpenDnaEvolution)
            }
        }

        // Coach Dashboard — the unified "Good morning" advisor (recommendation +
        // recovery + focus + progression + goal + estimated time).
        item {
            com.replog.ui.coach.CoachDashboardCard(
                state = coachState,
                onStart = {
                    coachViewModel.acceptRecommendation(
                        onLaunchWorkout = onStartRecommendedWorkout,
                        onRestDay = onViewRecoveryGuidance
                    )
                },
                onDismiss = { coachViewModel.dismissRecommendation() },
                onRefresh = { coachViewModel.loadRecommendation(force = true) },
                // Rest day override: confirm intent, then respect the user's choice.
                onTrainAnyway = { showTrainAnywayDialog = true }
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

        // Browse the curated Quick Workout library (P2/P3).
        item {
            RepLogCard(onClick = onOpenQuickWorkouts) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Quick Workouts", fontWeight = FontWeight.Bold)
                        Text("Start a proven workout instantly — 5x5, PPL, full body and more.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Quick Start: pinned/starred templates (P5) — one tap launches.
        if (state.favoriteTemplates.isNotEmpty()) {
            item { Text("Quick Start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(state.favoriteTemplates, key = { "fav-${it.template.id}" }) { template ->
                RepLogCard(onClick = { viewModel.startTemplate(template); onStartWorkout() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(template.template.name, fontWeight = FontWeight.Bold)
                            Text("${template.exercises.size} exercises", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Start", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Recent workouts (P6) — one tap repeats them.
        val recentCompleted = state.recentSessions.filter { it.session.endTime != null }
        if (recentCompleted.isNotEmpty()) {
            item { Text("Recent workouts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(recentCompleted.take(5), key = { "recent-${it.session.id}" }) { session ->
                RepLogCard(onClick = { viewModel.repeatSession(session); onStartWorkout() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(session.session.templateName ?: "Workout", fontWeight = FontWeight.Bold)
                            Text(
                                "${session.exercises.size} exercises • ${session.exercises.sumOf { it.sets.size }} sets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("Repeat", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Top goal progress (P6) — tap to open Goals.
        state.topGoal?.let { goal ->
            item { HomeGoalCard(goal, onClick = onOpenGoals) }
        }

        // Training Genome headline (P2) — tap to open Training DNA.
        state.genomeHeadline?.let { headline ->
            item { HomeGenomeCard(headline, onClick = onOpenTrainingDna) }
        }

        // Coaching insight + last PR (single, not a full feed — that lives in History/Progress)
        item { HomeInsightCard(state.insight) }
        item { Text("Last PB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            val lastPr = state.recentPRs.firstOrNull()
            if (lastPr == null) {
                RepLogCard { InlineEmpty("Personal bests appear here when you beat previous bests.") }
            } else {
                RepLogCard { Row(verticalAlignment = Alignment.CenterVertically) { PRBadge(); Spacer(Modifier.width(10.dp)); Text("${formatWeight(lastPr.weight)} × ${lastPr.reps} reps", fontWeight = FontWeight.Bold) } }
            }
        }

        // Lightweight navigation to the detail areas (their full job lives elsewhere).
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { SecondaryButton("Goals", Modifier.weight(1f), onClick = onOpenGoals); SecondaryButton("History", Modifier.weight(1f), onClick = onViewHistory) } }
        item { SecondaryButton("Coach History", onClick = onOpenCoachHistory) }
        item {
            val shareContext = androidx.compose.ui.platform.LocalContext.current
            SecondaryButton("Share my progress") {
                com.replog.util.ShareCardRenderer.renderAndShare(
                    context = shareContext,
                    headline = "${state.sessionCount} workouts logged",
                    subtitle = "My RepLog training so far",
                    stats = listOf(
                        com.replog.util.ShareStat("Workouts", state.sessionCount.toString()),
                        com.replog.util.ShareStat("Total volume", formatWeight(state.totalVolume)),
                        com.replog.util.ShareStat("This week", "${state.sessionsThisWeek} sessions"),
                        com.replog.util.ShareStat("Day streak", if (state.dayStreak > 0) "${state.dayStreak} days" else "—")
                    ),
                    footnote = state.recentPRs.firstOrNull()?.let { "Latest PB: ${formatWeight(it.weight)} × ${it.reps}" }
                )
            }
        }
    }

    if (showTrainAnywayDialog) {
        AlertDialog(
            onDismissRequest = { showTrainAnywayDialog = false },
            title = { Text("Train anyway?") },
            text = {
                Text(
                    "Recovery data suggests rest today. Training while fatigued may reduce performance and recovery. Continue?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showTrainAnywayDialog = false
                    coachViewModel.recordTrainAnywayOverride()
                    onStartWorkout()
                }) { Text("Train Anyway") }
            },
            dismissButton = {
                TextButton(onClick = { showTrainAnywayDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HomeGoalCard(goal: HomeGoal, onClick: () -> Unit) = RepLogCard(onClick = onClick) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(goal.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(goal.etaText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(progress = { goal.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
    Spacer(Modifier.height(6.dp))
    Text("${goal.progressPercent}% • ${goal.summaryLine}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun HomeGenomeCard(headline: String, onClick: () -> Unit) = RepLogCard(onClick = onClick) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Your Training Genome", fontWeight = FontWeight.Bold)
            Text("You grow best with $headline. Tap for the full picture.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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

@Composable
private fun ContinueWorkoutCard(cw: ContinueWorkout, onContinue: () -> Unit) = RepLogCard(onClick = onContinue) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Continue Workout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(cw.templateName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Resume", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(8.dp))
    val elapsedMin = ((System.currentTimeMillis() - cw.startTime) / 60000L).toInt().coerceAtLeast(0)
    Text(
        "$elapsedMin min • ${cw.exerciseCount} exercises • ${cw.setCount} sets logged",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun confidenceText(c: com.replog.domain.intelligence.BriefingConfidence): String = when (c) {
    com.replog.domain.intelligence.BriefingConfidence.HIGH -> "High"
    com.replog.domain.intelligence.BriefingConfidence.MEDIUM -> "Medium"
    com.replog.domain.intelligence.BriefingConfidence.LOW -> "Low"
}

@Composable
private fun IntelligenceNavCard(label: String, modifier: Modifier, onClick: () -> Unit) = RepLogCard(modifier, onClick = onClick) {
    Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun TodaysBriefingCard(
    b: com.replog.domain.intelligence.TodaysBriefing,
    onOpenRecovery: () -> Unit
) = RepLogCard(onClick = onOpenRecovery) {
    var showWhy by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text("Today's Briefing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        b.recoveryScore?.let {
            Text("Recovery $it%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(Modifier.height(10.dp))
    // P4: conversational, deterministic narrative (one sentence per signal).
    val narrative = b.narrative.ifEmpty { b.reasons }
    narrative.forEach { line ->
        Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
    }

    // P3: every recommendation is explainable.
    if (b.explainSections.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showWhy = !showWhy }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showWhy) "Hide why" else "Why?", fontWeight = FontWeight.Bold)
        }
        if (showWhy) {
            b.explainSections.forEach { section ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(section.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Confidence: ${confidenceText(section.confidence)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                section.lines.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (b.coachInsights.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Text("Your coach noticed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        b.coachInsights.forEach { insight ->
            Text("• ${insight.text}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RepLogScoreCard(s: com.replog.domain.intelligence.RepLogScoreResult) = RepLogCard {
    var showBreakdown by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("RepLog Score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            val trend = buildString {
                s.weeklyTrend?.let { append("Week ${if (it >= 0) "+$it" else "$it"}") }
                s.monthlyTrend?.let { if (isNotEmpty()) append("  •  "); append("Month ${if (it >= 0) "+$it" else "$it"}") }
            }
            if (trend.isNotBlank()) {
                Text(trend, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("${s.score}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(progress = { s.score / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = { showBreakdown = !showBreakdown }, modifier = Modifier.fillMaxWidth()) {
        Text(if (showBreakdown) "Hide breakdown" else "How is this calculated?", fontWeight = FontWeight.Bold)
    }
    if (showBreakdown) {
        s.components.forEach { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text("${c.value}/100 • ${c.weightPercent}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(c.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
        }
    }
}
