package com.replog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    // Phase 2 Gap 5: dedicated weekly-volume card data. Empty list is a
    // legitimate UI state (card stays hidden); null means "not yet loaded".
    val weeklyLandmarks by viewModel.weeklyLandmarks.collectAsState()
    // Phase 2 Gap 4: dedicated muscle-gap card data. Same null-vs-empty-list
    // semantics as weekly landmarks; null = not loaded, empty = no DNA yet.
    val muscleGapSuggestions by viewModel.muscleGapSuggestions.collectAsState()
    // Phase 2 Gap 6: dedicated progression-forecast card data. Empty list
    // = no history yet; null = not loaded.
    val progressionForecasts by viewModel.progressionForecasts.collectAsState()
    val recommendedWorkout by viewModel.recommendedWorkout.collectAsState()
    val recoveryCalendarStrip by viewModel.recoveryCalendarStrip.collectAsState()
    val progressionProjection by viewModel.progressionProjection.collectAsState()
    val dnaEvolution by viewModel.dnaEvolution.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    var showTrainAnywayDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refresh(); viewModel.loadIntelligence(); coachViewModel.loadRecommendation(force = false) }

    // Phase 2 Gap 2: refresh the briefing whenever Home returns to the foreground
    // (e.g. user finishes a workout and tabs back to Home). The VM dedupes via
    // its (sessionCount, epochDay) cache; a no-op ON_RESUME (same day, same count)
    // is harmless.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Phase 2 Gap 2: a 60-second heartbeat so an epoch-day rollover (midnight
    // crossing while Home stays mounted) invalidates the briefing cache without
    // requiring the user to leave Home. Cancelled automatically when Home leaves
    // the composition; ticks while Home is in the foreground only.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            viewModel.onMinuteTick()
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(contentPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            val name = displayName?.takeIf { it.isNotBlank() }
            val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
            val header = if (name != null) com.replog.util.profile.Greetings.timeOfDay(name, hour) else "Today"
            val subtitle = if (name != null) com.replog.util.profile.Greetings.possessive(name, "training dashboard") else "Your training dashboard."
            Text(header, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Priority 2: Continue Workout - the top card when a session is in progress.
        continueWorkout?.let { cw ->
            item { ContinueWorkoutCard(cw, onContinue = onStartWorkout) }
        }

        // Priority 1: Today's Briefing - the unified intelligence card.
        briefing?.let { b ->
            item { TodaysBriefingCard(b, onOpenRecovery = onOpenRecoveryCentre) }
            // Phase 2 Gap 3: dedicated recommendation card so the headline
            // recommendation gets its own visual hierarchy + a CTA distinct
            // from the briefing's narrative. The CoachDashboardCard below
            // remains untouched — it pulls from coachState (CoachViewModel)
            // and owns the dismiss / refresh / train-anyway UX.
            item { RecommendationCard(b, onStart = onStartRecommendedWorkout, recommendedWorkout = recommendedWorkout, onStartPlan = { plan -> viewModel.startRecommendedWorkout(plan, b.recommendation); onStartWorkout() }, onSaveTemplate = { viewModel.saveRecommendedWorkoutAsTemplate() }) }
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

        // Phase 2 Gap 5: full weekly volume card with its own visual hierarchy.
        // Empty list is the low-data state (the briefing itself still shows the
        // "Log a few more workouts" fallback); we hide silently in that case so
        // the card never advertises zeros.
        weeklyLandmarks?.takeIf { it.isNotEmpty() }?.let { landmarks ->
            item { WeeklyLandmarksCard(landmarks, onOpen = onOpenMuscleBalance) }
        }

        // Phase 2 Gap 4: muscle-gap card. Each row carries its own per-muscle
        // "Start focus workout" CTA that delegates to the existing
        // `IntelligenceRepository.startMuscleGapWorkout()`, which creates a
        // session + sets active id; Home then navigates to the workout tab.
        muscleGapSuggestions?.takeIf { it.isNotEmpty() }?.let { suggestions ->
            item {
                MuscleGapCard(
                    suggestions = suggestions,
                    onStartMuscle = { muscle ->
                        viewModel.startMuscleGapFocus(muscle)
                        onStartWorkout()
                    },
                    onOpen = onOpenMuscleBalance
                )
            }
        }

        // Phase 2 Gap 6: progression-forecast card. One headline projection
        // (the top result by confidence + weekly gain) plus a compact list
        // of secondary lifts — mirrors what the briefing's "Progress
        // Forecast" explainSection carries, but expanded to a full Home card.
        progressionForecasts?.takeIf { it.isNotEmpty() }?.let { forecasts ->
            item { ProgressionForecastCard(forecasts, onOpen = onOpenDnaEvolution) }
        }

        // Coach Dashboard — the unified "Good morning" advisor (recommendation +
        // recovery + focus + progression + goal + estimated time).
        item {
            com.replog.ui.coach.CoachDashboardCard(
                state = coachState,
                onStart = {
                    coachViewModel.acceptRecommendation(
                        onLaunchWorkout = {
                            recommendedWorkout?.workoutPlan?.let { plan ->
                                viewModel.startRecommendedWorkout(plan, recommendedWorkout!!.title)
                            }
                            onStartWorkout()
                        },
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

        // Phase 3 Gap 4: compact 7-day recovery forecast strip.
        if (recoveryCalendarStrip.isNotEmpty()) {
            item { RecoveryCalendarStrip(recoveryCalendarStrip.take(7)) }
        }

        // Phase 3 Gap 5: top-4 lifts projected 4 weeks ahead.
        if (progressionProjection.isNotEmpty()) {
            item { ProgressionProjectionStrip(progressionProjection) }
        }

        // Phase 3 Gap 6: Training DNA evolution snapshot.
        if (dnaEvolution.hasData) {
            item { DnaEvolutionCard(dnaEvolution) }
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

// Phase 2 Gap 3: dedicated recommendation card with its own visual hierarchy.
// Surfaces the IntelligenceEngine's `recommendation` string at weighty title
// size, gives the matching personalised coach-insight as a "Because: …" reason
// line, and provides a single primary CTA. The "Log a few more workouts" low-
// data fallback branch disables the CTA (it would have nothing concrete to
// start) and shows a soft caption instead, matching the briefing fallback.
@Composable
private fun RecommendationCard(
    b: com.replog.domain.intelligence.TodaysBriefing,
    onStart: () -> Unit,
    recommendedWorkout: com.replog.data.repository.RecommendedWorkoutCardEntry?,
    onStartPlan: (com.replog.domain.recommendation.WorkoutPlan) -> Unit,
    onSaveTemplate: () -> Unit = {}
) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            "RECOMMENDED TODAY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(Modifier.height(4.dp))
    Text(
        b.recommendation,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold
    )
    Spacer(Modifier.height(12.dp))
    Text("Because:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    val insightText = b.coachInsights.firstOrNull()?.text
        ?: "No additional insight today — based on recent training."
    Text(
        insightText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (b.recommendation != "Log a few more workouts") {
        Spacer(Modifier.height(12.dp))
        // Phase 3 Gap 1: if a concrete workout plan exists, use it.
        // Otherwise fall back to the existing onStart callback.
        val hasPlan = recommendedWorkout?.workoutPlan != null &&
            recommendedWorkout.workoutPlan.exercises.isNotEmpty()
        if (hasPlan) {
            Text(
                "${recommendedWorkout!!.exerciseCount} exercises • ~${recommendedWorkout.estimatedDurationMinutes} min • ${recommendedWorkout.split}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                recommendedWorkout.topExercises.joinToString(" \u00b7 "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
        TextButton(onClick = {
            if (hasPlan) onStartPlan(recommendedWorkout!!.workoutPlan!!) else onStart()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Start recommended workout", fontWeight = FontWeight.Bold)
        }
        if (hasPlan) {
            TextButton(onClick = onSaveTemplate, modifier = Modifier.fillMaxWidth()) {
                Text("Save as template", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    } else {
        Spacer(Modifier.height(8.dp))
        Text(
            "Need a couple more sessions before recommendations lock in.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Phase 2 Gap 5: dedicated weekly-volume card with its own visual hierarchy.
// Renders the ten hypertrophy groups from `VolumeLandmarks.analyze(weeks = 1)`
// as compact horizontal bars with a colour-coded status badge (UNDER /
// IN_RANGE / ABOVE / NONE). UNDER groups are surfaced first so the user sees
// the gap signal before the in-range noise; the CTA opens the existing
// Muscle Balance drill-down screen (the same route the IntelligenceNavCard
// "Muscle Balance" tile uses).
@Composable
private fun WeeklyLandmarksCard(
    landmarks: List<com.replog.domain.volume.VolumeLandmark>,
    onOpen: () -> Unit
) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.StackedBarChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "WEEKLY VOLUME",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(Modifier.height(4.dp))
    val underCount = landmarks.count {
        it.status == com.replog.domain.volume.VolumeStatus.UNDER ||
            it.status == com.replog.domain.volume.VolumeStatus.NONE
    }
    val inRangeCount = landmarks.count {
        it.status == com.replog.domain.volume.VolumeStatus.IN_RANGE
    }
    val summary = when {
        underCount == 0 && inRangeCount > 0 ->
            "All $inRangeCount trained muscle groups are in their optimal range."
        underCount > 0 && inRangeCount > 0 ->
            "$inRangeCount in range • $underCount below optimal — tap for suggestions."
        underCount > 0 && inRangeCount == 0 ->
            "$underCount groups need attention — tap for suggestions."
        else -> "Tap for full weekly volume analysis."
    }
    Text(
        summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(10.dp))
    // Priority order: UNDER first (action signal), then ABOVE, then IN_RANGE,
    // then NONE, so the user sees the actionable rows at the top of the card.
    val ordered = landmarks.sortedWith(
        compareBy<com.replog.domain.volume.VolumeLandmark> {
            when (it.status) {
                com.replog.domain.volume.VolumeStatus.UNDER -> 0
                com.replog.domain.volume.VolumeStatus.NONE -> 0
                com.replog.domain.volume.VolumeStatus.ABOVE -> 1
                com.replog.domain.volume.VolumeStatus.IN_RANGE -> 2
            }
        }.thenBy { it.muscleGroup }
    )
    ordered.forEach { lm ->
        WeeklyLandmarkRow(lm)
        Spacer(Modifier.height(4.dp))
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Text("Open Muscle Balance →", fontWeight = FontWeight.Bold)
    }
}

/** One horizontal-bar muscle-group row in the WeeklyLandmarksCard. */
@Composable
private fun WeeklyLandmarkRow(lm: com.replog.domain.volume.VolumeLandmark) {
    val barColor = when (lm.status) {
        com.replog.domain.volume.VolumeStatus.IN_RANGE -> MaterialTheme.colorScheme.primary
        com.replog.domain.volume.VolumeStatus.UNDER -> MaterialTheme.colorScheme.tertiary
        com.replog.domain.volume.VolumeStatus.ABOVE -> MaterialTheme.colorScheme.secondary
        com.replog.domain.volume.VolumeStatus.NONE -> MaterialTheme.colorScheme.outlineVariant
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fraction = if (lm.optimalHigh > 0) {
        (lm.weeklySets / lm.optimalHigh).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            lm.muscleGroup,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(88.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.height(10.dp).weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(trackColor, RoundedCornerShape(5.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(barColor, RoundedCornerShape(5.dp))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            when (lm.status) {
                com.replog.domain.volume.VolumeStatus.NONE -> "—"
                else -> "${lm.weeklySets.toInt()} / ${lm.optimalLow}-${lm.optimalHigh}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Spacer(Modifier.width(8.dp))
        WeeklyStatusBadge(lm.status)
    }
}

/** Compact coloured pill showing the VolumeStatus label for one landmark. */
@Composable
private fun WeeklyStatusBadge(status: com.replog.domain.volume.VolumeStatus) {
    val fg = when (status) {
        com.replog.domain.volume.VolumeStatus.IN_RANGE -> MaterialTheme.colorScheme.primary
        com.replog.domain.volume.VolumeStatus.UNDER -> MaterialTheme.colorScheme.tertiary
        com.replog.domain.volume.VolumeStatus.ABOVE -> MaterialTheme.colorScheme.secondary
        com.replog.domain.volume.VolumeStatus.NONE -> MaterialTheme.colorScheme.outline
    }
    val bg = when (status) {
        com.replog.domain.volume.VolumeStatus.IN_RANGE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        com.replog.domain.volume.VolumeStatus.UNDER -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        com.replog.domain.volume.VolumeStatus.ABOVE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        com.replog.domain.volume.VolumeStatus.NONE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }
    Text(
        status.label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// Phase 2 Gap 4: dedicated muscle-gap card on Home with its own visual
// hierarchy. Each row carries a per-muscle "Start focus workout" CTA wired
// to the existing IntelligenceRepository.startMuscleGapWorkout() helper,
// which creates a session with the top-ranked exercises and sets the active
// session id in DataStore — Home then navigates to the workout tab.
@Composable
private fun MuscleGapCard(
    suggestions: List<com.replog.domain.musclegap.MuscleGapSuggestion>,
    onStartMuscle: (String) -> Unit,
    onOpen: () -> Unit
) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Healing,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "MUSCLES YOU'VE BEEN SKIPPING",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(Modifier.height(4.dp))
    val total = suggestions.sumOf { it.exercises.size }
    val headline = if (suggestions.size == 1) {
        "1 under-trained group with $total exercise suggestion. Tap to start a focus workout."
    } else {
        "$total exercise suggestions across ${suggestions.size} under-trained groups. Tap to start a focus workout."
    }
    Text(
        headline,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(10.dp))
    suggestions.forEach { s ->
        MuscleGapRow(s, onStartMuscle)
        Spacer(Modifier.height(6.dp))
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Text("Open Muscle Balance →", fontWeight = FontWeight.Bold)
    }
}

/** One weak-muscle row in the MuscleGapCard: name, suggestion count, top exercises, focus-workout CTA. */
@Composable
private fun MuscleGapRow(
    s: com.replog.domain.musclegap.MuscleGapSuggestion,
    onStart: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                s.muscle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${s.exercises.size} suggested",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            s.exercises.take(3).joinToString(" \u00b7 ") { it.name },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = { onStart(s.muscle) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Start ${s.muscle} focus workout \u2192",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Phase 2 Gap 6: dedicated progression-forecast card with its own visual hierarchy.
// Surfaces the top progression projections (ranked by confidence desc, weekly gain
// desc) as a dedicated Home card. The headline gets a star + bolder styling; the
// remaining entries are compact rows. The CTA opens the existing DNA Evolution
// drill-down (same route the IntelligenceNavCard "DNA" tile uses), where the
// user sees the full trend timeline + history.
//
// `forecasts.first()` is the headline by construction (sorted by confidence +
// weekly gain in IntelligenceRepository.buildProgressionForecasts), so the user
// always sees the strongest single lift at the top of the card.
@Composable
private fun ProgressionForecastCard(
    forecasts: List<com.replog.data.repository.ForecastCardEntry>,
    onOpen: () -> Unit
) = RepLogCard {
    val headline = forecasts.first()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.TrendingUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "PROGRESS FORECAST",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(Modifier.height(4.dp))
    val more = forecasts.size - 1
    val subtitle = if (more > 0)
        "Top ${forecasts.size} trending lifts over the next few weeks."
    else
        "Your top trending lift over the next few weeks."
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(12.dp))
    ProgressionForecastRow(headline, isHeadline = true)
    if (more > 0) {
        Spacer(Modifier.height(8.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))
        forecasts.drop(1).forEach { entry ->
            ProgressionForecastRow(entry, isHeadline = false)
            Spacer(Modifier.height(6.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Confidence = sets/week trend stability \u00d7 load consistency.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Text("See strength trajectory \u2192", fontWeight = FontWeight.Bold)
    }
}

/** One row in the progression-forecast card. Headline gets a star + bolder style. */
@Composable
private fun ProgressionForecastRow(
    f: com.replog.data.repository.ForecastCardEntry,
    isHeadline: Boolean
) {
    val trendGlyph = when (f.trend) {
        com.replog.domain.forecast.ForecastTrend.RISING -> "\u2191"
        com.replog.domain.forecast.ForecastTrend.FLAT -> "\u2192"
        com.replog.domain.forecast.ForecastTrend.DECLINING -> "\u2193"
    }
    val trendLabel = when (f.trend) {
        com.replog.domain.forecast.ForecastTrend.RISING -> "Rising"
        com.replog.domain.forecast.ForecastTrend.FLAT -> "Flat"
        com.replog.domain.forecast.ForecastTrend.DECLINING -> "Declining"
    }
    val confidenceLabel = when (f.confidence) {
        com.replog.domain.forecast.ForecastConfidence.HIGH -> "High"
        com.replog.domain.forecast.ForecastConfidence.MEDIUM -> "Medium"
        com.replog.domain.forecast.ForecastConfidence.LOW -> "Low"
    }
    val confidenceColor = when (f.confidence) {
        com.replog.domain.forecast.ForecastConfidence.HIGH -> MaterialTheme.colorScheme.primary
        com.replog.domain.forecast.ForecastConfidence.MEDIUM -> MaterialTheme.colorScheme.tertiary
        com.replog.domain.forecast.ForecastConfidence.LOW -> MaterialTheme.colorScheme.outline
    }
    val weeklyGainText = if (f.weeklyGainKg >= 0.0)
        "+${"%.1f".format(f.weeklyGainKg)} kg/wk"
    else
        "${"%.1f".format(f.weeklyGainKg)} kg/wk"
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isHeadline) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                f.exerciseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.width(28.dp))
            Text(
                f.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            f.projectionLabel,
            style = if (isHeadline) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(Modifier.height(2.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 28.dp)) {
        Text(
            "$trendGlyph $trendLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "\u2022",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(weeklyGainText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(
            "\u2022",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$confidenceLabel confidence",
            style = MaterialTheme.typography.bodySmall,
            color = confidenceColor,
            fontWeight = FontWeight.Bold
        )
    }
}

// Phase 3 Gap 4: compact 7-day recovery forecast strip.
@Composable
private fun RecoveryCalendarStrip(days: List<com.replog.domain.recovery.RecoveryCalendarDay>) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Healing, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("7-DAY RECOVERY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(10.dp))
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        days.forEach { day ->
            val (bg, fg) = when (day.state) {
                com.replog.domain.recovery.RecoveryDay.READY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
                com.replog.domain.recovery.RecoveryDay.CAUTION -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f) to MaterialTheme.colorScheme.tertiary
                com.replog.domain.recovery.RecoveryDay.RECOVERING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f) to MaterialTheme.colorScheme.secondary
                com.replog.domain.recovery.RecoveryDay.REST_NO_DATA -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f) to MaterialTheme.colorScheme.outline
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(day.dayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(bg, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (day.trainedToday) FontWeight.ExtraBold else FontWeight.Bold,
                        color = fg
                    )
                }
            }
        }
    }
}

// ───────────────────────────────────────────────
// Phase 3 Gap 5 — Progression Projection Strip
// ───────────────────────────────────────────────

@Composable
private fun ProgressionProjectionStrip(entries: List<ForecastCardEntry>) = RepLogCard {
    Column {
        Text(
            "Next 4 Weeks",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEach { entry ->
                val trendColor = when (entry.trend) {
                    com.replog.domain.forecast.ForecastTrend.RISING -> Color(0xFF4CAF50)
                    com.replog.domain.forecast.ForecastTrend.FLAT -> Color(0xFFFF9800)
                    com.replog.domain.forecast.ForecastTrend.DECLINING -> Color(0xFFE53935)
                }
                val trendArrow = when (entry.trend) {
                    com.replog.domain.forecast.ForecastTrend.RISING -> "▲"
                    com.replog.domain.forecast.ForecastTrend.FLAT -> "—"
                    com.replog.domain.forecast.ForecastTrend.DECLINING -> "▼"
                }
                val confidenceAlpha = when (entry.confidence) {
                    com.replog.domain.forecast.ForecastConfidence.HIGH -> 1.0f
                    com.replog.domain.forecast.ForecastConfidence.MEDIUM -> 0.7f
                    com.replog.domain.forecast.ForecastConfidence.LOW -> 0.45f
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.alpha(confidenceAlpha)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            entry.exerciseName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            entry.projectionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            trendArrow,
                            color = trendColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────
// Phase 3 Gap 6 — Training DNA Evolution Card
// ───────────────────────────────────────────────

@Composable
private fun DnaEvolutionCard(data: DnaEvolutionData) = RepLogCard {
    val latest = data.points.lastOrNull()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Training DNA",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (data.genomeMaturity) {
                    "Mature" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                    "Developing" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                    else -> Color(0xFF2196F3).copy(alpha = 0.15f)
                }
            ) {
                Text(
                    data.genomeMaturity,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when (data.genomeMaturity) {
                        "Mature" -> Color(0xFF4CAF50)
                        "Developing" -> Color(0xFFFF9800)
                        else -> Color(0xFF2196F3)
                    }
                )
            }
        }

        if (latest != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DnaStat(
                    label = "Rep Range",
                    value = latest.preferredRepRange,
                    modifier = Modifier.weight(1f)
                )
                DnaStat(
                    label = "Recovery",
                    value = "${latest.recoveryHours}h",
                    modifier = Modifier.weight(1f)
                )
                DnaStat(
                    label = "Avg Duration",
                    value = "${latest.workoutDurationMinutes}min",
                    modifier = Modifier.weight(1f)
                )
            }

            if (data.points.size >= 2) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DnaStat(
                        label = "Trend",
                        value = data.consistencyTrend,
                        modifier = Modifier.weight(1f)
                    )
                    DnaStat(
                        label = "Vol. Tolerance",
                        value = "${latest.volumeTolerance}/100",
                        modifier = Modifier.weight(1f)
                    )
                    DnaStat(
                        label = "PRs (30d)",
                        value = "${latest.monthlyPrCount}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DnaStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
