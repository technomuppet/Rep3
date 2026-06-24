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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.coachdash.CoachBriefing
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton

/**
 * Coach Dashboard — the persistent "Good morning" advisor that unifies
 * recommendation + recovery + goals + progression into one card on Home.
 */
@Composable
fun CoachDashboardCard(
    state: CoachUiState,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onTrainAnyway: () -> Unit = {}
) = RepLogCard {
    when {
        state.isLoading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Your coach is reading your training data…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.error != null -> {
            Text("Coach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Try again", onClick = onRefresh)
        }
        state.briefing != null -> BriefingBody(state, onStart, onDismiss, onTrainAnyway)
        else -> Text("Log a workout and your coach will tell you what to train next.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BriefingBody(state: CoachUiState, onStart: () -> Unit, onDismiss: () -> Unit, onTrainAnyway: () -> Unit) {
    val b: CoachBriefing = state.briefing!!
    var showWhy by remember { mutableStateOf(false) }

    // Greeting + recovery chip
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(b.greeting, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        b.recoveryScore?.let { score ->
            Surface(color = recoveryColor(score).copy(alpha = 0.15f), shape = RoundedCornerShape(999.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.HealthAndSafety, null, tint = recoveryColor(score), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$score%", fontWeight = FontWeight.Bold, color = recoveryColor(score))
                }
            }
        }
    }
    b.recoveryDirective?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Spacer(Modifier.height(12.dp))

    // The headline directive
    Text(b.trainLine, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)

    Spacer(Modifier.height(10.dp))

    // Briefing lines
    b.focusLine?.let { BriefingLine(Icons.Default.CenterFocusStrong, it) }
    b.progressionLine?.let { BriefingLine(Icons.Default.TrendingUp, it) }
    b.estimatedMinutes?.let { BriefingLine(Icons.Default.Schedule, "Estimated workout: $it min") }
    b.goalLine?.let { BriefingLine(Icons.Default.Flag, it) }
    b.confidence?.let { BriefingLine(Icons.Default.Bolt, "Confidence: $it%") }

    Spacer(Modifier.height(14.dp))

    // Actions
    if (state.accepted && !b.isRestDay) {
        Text("Workout staged — opening your session…", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
    }
    if (b.isRestDay) {
        // Respect the user's autonomy: rest is recommended, not enforced.
        Text(
            "Recovery is when muscle growth and performance gains happen. A rest day now usually means a stronger next session.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        PrimaryButton("Rest Today", onClick = onStart)
        Spacer(Modifier.height(6.dp))
        SecondaryButton("Train Anyway", onClick = onTrainAnyway)
    } else {
        PrimaryButton("Start Recommended Workout", onClick = onStart)
        Spacer(Modifier.height(6.dp))
        SecondaryButton("Not today", onClick = onDismiss)
    }

    // Why? (reuse existing explanation contributions)
    if (state.contributions.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showWhy = !showWhy }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showWhy) "Hide explanation" else "Why this plan?", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Icon(if (showWhy) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        AnimatedVisibility(visible = showWhy) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.contributions.forEach { c ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(c.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(4.dp))
                            c.lines.forEach { line ->
                                Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun recoveryColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF2E7D32)
    score >= 60 -> MaterialTheme.colorScheme.primary
    score >= 40 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}
