package com.replog.ui.coach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.recommendation.ExplanationContribution
import com.replog.domain.recommendation.Recommendation
import com.replog.domain.recommendation.RecommendationType
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton

/**
 * Phase 1 + 2 + 3 — Smart Coach Home card with expandable explanation and Smart Start.
 * Renders only data the existing RecommendationEngine produced.
 */
@Composable
fun SmartCoachCard(
    state: CoachUiState,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text("Today's Recommendation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(12.dp))

    when {
        state.isLoading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Your coach is reading your training data…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.error != null -> {
            Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Try again", onClick = onRefresh)
        }
        state.recommendation != null -> RecommendationBody(state.recommendation, state.contributions, state.accepted, onStart, onDismiss)
        else -> Text("Log a workout and your coach will tell you what to train next.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecommendationBody(
    rec: Recommendation,
    contributions: List<ExplanationContribution>,
    accepted: Boolean,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    var showWhy by remember { mutableStateOf(false) }

    // Headline (e.g. "Train Push")
    Text(rec.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(4.dp))
    Text(rec.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)

    Spacer(Modifier.height(12.dp))

    // Recovery + plateau + progression chips derived from the existing recommendation
    val recoveryLine = rec.dataUsed.firstOrNull { it.startsWith("Recovery score") }
    if (recoveryLine != null) CoachLine("Recovery", recoveryLine.substringAfter(":").trim())

    val plateauLine = rec.dataUsed.firstOrNull { it.startsWith("Stalled lifts") }
    if (plateauLine != null) CoachLine("Plateau detected", plateauLine.substringAfter(":").trim())

    // Progression suggestion (first planned exercise with an explicit target weight)
    val planned = rec.workoutPlan?.exercises?.firstOrNull { it.targetWeight != null }
        ?: rec.workoutPlan?.exercises?.firstOrNull()
    if (planned != null) {
        val weight = planned.targetWeight?.let { w -> (if (w % 1.0 == 0.0) w.toInt().toString() else "%.1f".format(w)) + " kg" } ?: "bodyweight"
        CoachLine("Suggested progression", "${planned.exerciseName}: $weight × ${planned.targetReps}")
    }

    Spacer(Modifier.height(10.dp))

    // Confidence score
    Text("Confidence: ${rec.confidenceScore.toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { (rec.confidenceScore / 100.0).toFloat() },
        modifier = Modifier.fillMaxWidth().height(8.dp),
    )

    Spacer(Modifier.height(14.dp))

    // Phase 3 — Smart Start
    val startLabel = if (rec.type == RecommendationType.REST) "View recovery guidance" else "Start Recommended Workout"
    if (accepted && rec.type != RecommendationType.REST) {
        Text("Workout staged — opening your session…", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
    PrimaryButton(startLabel, onClick = onStart)
    Spacer(Modifier.height(6.dp))
    SecondaryButton("Not today", onClick = onDismiss)

    Spacer(Modifier.height(8.dp))

    // Phase 2 — "Why this recommendation?" expandable, no black box
    TextButton(onClick = { showWhy = !showWhy }, modifier = Modifier.fillMaxWidth()) {
        Text(if (showWhy) "Hide explanation" else "Why this recommendation?", fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Icon(if (showWhy) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    }
    AnimatedVisibility(visible = showWhy) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            contributions.forEach { c ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(c.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        c.lines.forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text(
                "Expected outcome: ${rec.expectedOutcome}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun CoachLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
