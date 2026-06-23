package com.replog.ui.trainingdna

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.ui.components.EmptyState
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.StatCard
import kotlin.math.roundToInt

@Composable
fun TrainingDnaInsightScreen(
    contentPadding: PaddingValues,
    viewModel: TrainingDnaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ){
        item {
            Text("Training DNA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Your personal training signature, built from every workout.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Priority 3 (#13) — Recovery dashboard (shown even before full DNA exists).
        state.recovery?.let { rec ->
            item { RecoveryDashboardCard(rec) }
        }

        if (!state.hasData) {
            item { EmptyState("Not enough data yet", "Finish a few more workouts and RepLog will calculate your training DNA.") }
        } else {
            item {
                DnaSummaryCard(
                    repRange = state.preferredRepRange,
                    volumeRange = state.preferredVolumeRange,
                    frequency = state.preferredFrequency
                )
            }

            // Phase 5 — plain-English interpretation of the DNA snapshot.
            state.interpretation?.let { interpretation ->
                item { DnaMeaningCard(interpretation) }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Monthly PRs", state.monthlyPRCount.toString(), Modifier.weight(1f))
                    StatCard("Volume tolerance", "${state.volumeToleranceScore.roundToInt()}%", Modifier.weight(1f))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Avg duration", "${state.averageWorkoutDuration.roundToInt()} min", Modifier.weight(1f))
                    StatCard("Avg recovery", "${state.averageRecoveryHours.roundToInt()} h", Modifier.weight(1f))
                }
            }

            item {
                MuscleCard(
                    title = "Strongest muscles",
                    muscles = state.strongestMuscles,
                    icon = Icons.Default.FitnessCenter
                )
            }

            item {
                MuscleCard(
                    title = "Weakest muscles",
                    muscles = state.weakestMuscles,
                    icon = Icons.Default.Accessibility
                )
            }

            item {
                Text("Best progressing lifts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            if (state.fastestProgressing.isEmpty()) {
                item { Text("No clear progression detected yet. Keep logging consistently.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.fastestProgressing) { exercise ->
                    RepLogCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoGraph, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(exercise, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Text("Plateau warnings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            if (state.stalledExercises.isEmpty()) {
                item { Text("No plateaus detected. All tracked lifts are moving forward.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.stalledExercises) { exercise ->
                    RepLogCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(exercise, fontWeight = FontWeight.SemiBold)
                                Text("No load, rep or volume increase in recent weeks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Priority 3 (#14) — Adaptive template swaps for stalled lifts.
            if (state.adaptiveSwaps.isNotEmpty()) {
                item {
                    Text("Suggested swaps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }
                items(state.adaptiveSwaps) { swap -> AdaptiveSwapCard(swap) }
            }

            // Priority 3 (#11) — Progression forecasts.
            if (state.forecasts.isNotEmpty()) {
                item {
                    Text("Progression forecast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }
                items(state.forecasts) { nf -> ForecastCard(nf) }
            }

            item {
                Text("Recovery trends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                RecoveryCard(
                    averageRecoveryHours = state.averageRecoveryHours,
                    volumeToleranceScore = state.volumeToleranceScore
                )
            }

            item {
                Text("Monthly PR rate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                RepLogCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("${state.monthlyPRCount} PRs in the last 30 days", fontWeight = FontWeight.SemiBold)
                            Text("PRs are personal-record sets logged in your workouts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DnaSummaryCard(
    repRange: String,
    volumeRange: String,
    frequency: String
) = RepLogCard {
    Text("Training style summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    SummaryRow(icon = Icons.Default.FitnessCenter, label = "Preferred rep range", value = repRange)
    SummaryRow(icon = Icons.Default.Speed, label = "Preferred volume", value = volumeRange)
    SummaryRow(icon = Icons.Default.CalendarToday, label = "Preferred frequency", value = frequency)
}

@Composable
private fun SummaryRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MuscleCard(
    title: String,
    muscles: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(8.dp))
    if (muscles.isEmpty()) {
        Text("No data available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Text(muscles.joinToString(" • "), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecoveryCard(
    averageRecoveryHours: Double,
    volumeToleranceScore: Double
) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Average recovery time", fontWeight = FontWeight.SemiBold)
            Text("${averageRecoveryHours.roundToInt()} hours between sessions", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DirectionsRun, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Volume tolerance", fontWeight = FontWeight.SemiBold)
            Text("${volumeToleranceScore.roundToInt()} / 100 — higher means more consistent weekly volume", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DnaMeaningCard(interpretation: com.replog.domain.trainingdna.DnaInterpretation) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(interpretation.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(8.dp))
    interpretation.statements.forEach { statement ->
        Spacer(Modifier.height(10.dp))
        Text(statement.prompt, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(statement.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        statement.detail?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecoveryDashboardCard(state: com.replog.domain.recovery.RecoveryDashboardState) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text("Recovery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("${state.score}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(progress = { (state.score / 100f) }, modifier = Modifier.fillMaxWidth().height(8.dp))
    Spacer(Modifier.height(10.dp))
    Text(state.directive, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    Text(state.directiveDetail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    if (state.factors.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        state.factors.take(4).forEach { factor ->
            Text("• $factor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ForecastCard(nf: NamedForecast) = RepLogCard {
    val f = nf.forecast
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(nf.exerciseName, fontWeight = FontWeight.Bold)
            Text(
                "Now ~${if (f.currentE1rm % 1.0 == 0.0) f.currentE1rm.toInt().toString() else "%.1f".format(f.currentE1rm)} kg 1RM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Text("Projected: ${f.projectionLabel}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    Text(f.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AdaptiveSwapCard(swap: com.replog.domain.adaptive.AdaptiveSwap) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${swap.fromName} → ${swap.toName}", fontWeight = FontWeight.Bold)
            Text("for ${swap.durationWeeks} weeks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(swap.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
