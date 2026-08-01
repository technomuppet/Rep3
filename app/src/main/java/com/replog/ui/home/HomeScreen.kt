package com.replog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
import com.replog.data.repository.ForecastCardEntry
import com.replog.data.repository.RecommendedWorkoutCardEntry
import com.replog.domain.intelligence.RepLogScoreResult
import com.replog.domain.intelligence.TodaysBriefing
import com.replog.domain.musclegap.MuscleGapSuggestion
import com.replog.domain.recommendation.WorkoutPlan
import com.replog.domain.recovery.RecoveryCalendarDay
import com.replog.domain.volume.VolumeLandmark
import com.replog.ui.coach.CoachDashboardCard
import com.replog.ui.coach.CoachViewModel
import com.replog.ui.components.*
import com.replog.ui.home.cards.DnaEvolutionCard
import com.replog.ui.home.cards.MuscleGapCard
import com.replog.ui.home.cards.ProgressionForecastCard
import com.replog.ui.home.cards.ProgressionProjectionStrip
import com.replog.ui.home.cards.RecommendationCard
import com.replog.ui.home.cards.RecoveryCalendarStrip
import com.replog.ui.home.cards.TodaysBriefingCard
import com.replog.ui.home.cards.WeeklyLandmarksCard
import com.replog.util.ShareCardRenderer
import com.replog.util.ShareStat
import com.replog.util.profile.Greetings
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    coachViewModel: CoachViewModel = hiltViewModel()
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
            val hour = remember { LocalTime.now().hour }
            val header = if (name != null) Greetings.timeOfDay(name, hour) else "Today"
            val subtitle = if (name != null) Greetings.possessive(name, "training dashboard") else "Your training dashboard."
            Text(header, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Priority 2: Continue Workout - the top card when a session is in progress.
        renderIfNotNull(continueWorkout) { cw ->
            ContinueWorkoutCard(cw, onContinue = onStartWorkout)
        }

        // Priority 1: Today's Briefing - the unified intelligence card.
        renderIfNotNull(briefing) { b ->
            TodaysBriefingCard(b, onOpenRecovery = onOpenRecoveryCentre)
        }
        // Phase 2 Gap 3: dedicated recommendation card so the headline
        // recommendation gets its own visual hierarchy + a CTA distinct
        // from the briefing's narrative. The CoachDashboardCard below
        // remains untouched — it pulls from coachState (CoachViewModel)
        // and owns the dismiss / refresh / train-anyway UX.
        renderIfNotNull(briefing) { b ->
            RecommendationCard(b, onStart = onStartRecommendedWorkout, recommendedWorkout = recommendedWorkout, onStartPlan = { plan -> viewModel.startRecommendedWorkout(plan, b.recommendation); onStartWorkout() }, onSaveTemplate = { viewModel.saveRecommendedWorkoutAsTemplate() })
        }

        // Sprint 8 P5: RepLog Score with explainable component breakdown.
        renderIfNotNull(repLogScore) { s ->
            RepLogScoreCard(s)
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
        renderIfNotNull(weeklyLandmarks?.takeIf { it.isNotEmpty() }) { landmarks ->
            WeeklyLandmarksCard(landmarks, onOpen = onOpenMuscleBalance)
        }

        // Phase 2 Gap 4: muscle-gap card. Each row carries its own per-muscle
        // "Start focus workout" CTA that delegates to the existing
        // `IntelligenceRepository.startMuscleGapWorkout()`, which creates a
        // session + sets active id; Home then navigates to the workout tab.
        renderIfNotNull(muscleGapSuggestions?.takeIf { it.isNotEmpty() }) { suggestions ->
            MuscleGapCard(
                suggestions = suggestions,
                onStartMuscle = { muscle ->
                    viewModel.startMuscleGapFocus(muscle)
                    onStartWorkout()
                },
                onOpen = onOpenMuscleBalance
            )
        }

        // Phase 2 Gap 6: progression-forecast card. One headline projection
        // (the top result by confidence + weekly gain) plus a compact list
        // of secondary lifts — mirrors what the briefing's "Progress
        // Forecast" explainSection carries, but expanded to a full Home card.
        renderIfNotNull(progressionForecasts?.takeIf { it.isNotEmpty() }) { forecasts ->
            ProgressionForecastCard(forecasts, onOpen = onOpenDnaEvolution)
        }

        // Coach Dashboard — the unified "Good morning" advisor (recommendation +
        // recovery + focus + progression + goal + estimated time).
        item {
            CoachDashboardCard(
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
        renderIfNotNull(state.topGoal) { goal ->
            HomeGoalCard(goal, onClick = onOpenGoals)
        }

        // Training Genome headline (P2) — tap to open Training DNA.
        renderIfNotNull(state.genomeHeadline) { headline ->
            HomeGenomeCard(headline, onClick = onOpenTrainingDna)
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
                ShareCardRenderer.renderAndShare(
                    context = shareContext,
                    headline = "${state.sessionCount} workouts logged",
                    subtitle = "My RepLog training so far",
                    stats = listOf(
                        ShareStat("Workouts", state.sessionCount.toString()),
                        ShareStat("Total volume", formatWeight(state.totalVolume)),
                        ShareStat("This week", "${state.sessionsThisWeek} sessions"),
                        ShareStat("Day streak", if (state.dayStreak > 0) "${state.dayStreak} days" else "—")
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
        Column(Modifier.weight(1f)) { Text(session.session.templateName ?: "Workout", fontWeight = FontWeight.Bold); Text(Instant.ofEpochMilli(session.session.startTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.getDefault())), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); Text("${session.exercises.size} exercises • ${session.exercises.sumOf { it.sets.size }} sets", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
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

@Composable
private fun IntelligenceNavCard(label: String, modifier: Modifier, onClick: () -> Unit) = RepLogCard(modifier, onClick = onClick) {
    Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun RepLogScoreCard(s: RepLogScoreResult) = RepLogCard {
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

/**
 * Emits a single LazyColumn item only when [value] is non-null — the shared
 * null-check render pattern used by the Home cards.
 */
private fun <T> LazyListScope.renderIfNotNull(value: T?, itemContent: @Composable (T) -> Unit) {
    if (value != null) item { itemContent(value) }
}
