package com.replog.ui.coach

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.data.model.RecommendationHistory
import com.replog.ui.components.EmptyState
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.StatCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 4 — Coach History. Reads the RecommendationHistory table via CoachViewModel
 * so users can see coaching consistency and outcomes over time.
 */
@Composable
fun CoachHistoryScreen(
    contentPadding: PaddingValues,
    viewModel: CoachViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsState()
    val (accepted, rejected) = viewModel.successRate.collectAsState().value

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Coach History", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Every recommendation your coach has made, and what you did with it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Followed", accepted.toString(), Modifier.weight(1f))
                StatCard("Skipped", rejected.toString(), Modifier.weight(1f))
                StatCard("Total", history.size.toString(), Modifier.weight(1f))
            }
        }
        if (history.isEmpty()) {
            item { EmptyState("No coaching yet", "Open the home screen and follow (or skip) a recommendation to start your coaching log.") }
        } else {
            items(history) { entry -> CoachHistoryCard(entry) }
        }
    }
}

@Composable
private fun CoachHistoryCard(entry: RecommendationHistory) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutcomeBadge(entry.outcome)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, fontWeight = FontWeight.Bold)
            Text(
                SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("${entry.confidenceScore.toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(8.dp))
    Text(entry.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    entry.workoutSplit?.let {
        Spacer(Modifier.height(4.dp))
        Text("Split: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    entry.rejectedReason?.takeIf { it.isNotBlank() }?.let {
        Spacer(Modifier.height(4.dp))
        Text("Skipped: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(6.dp))
    Text(outcomeLabel(entry.outcome), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = outcomeColor(entry.outcome))
}

@Composable
private fun OutcomeBadge(outcome: String?) {
    val (icon, tint) = when (outcome) {
        "Completed" -> Icons.Default.CheckCircle to com.replog.ui.theme.RepLogSuccess
        "Accepted" -> Icons.Default.HourglassEmpty to MaterialTheme.colorScheme.primary
        "Rejected" -> Icons.Default.Cancel to MaterialTheme.colorScheme.error
        else -> Icons.Default.HourglassEmpty to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(999.dp), color = tint.copy(alpha = 0.15f)) {
        Icon(icon, null, tint = tint, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun outcomeColor(outcome: String?): Color = when (outcome) {
    "Completed" -> com.replog.ui.theme.RepLogSuccess
    "Accepted" -> MaterialTheme.colorScheme.primary
    "Rejected" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun outcomeLabel(outcome: String?): String = when (outcome) {
    "Completed" -> "✓ Workout completed"
    "Accepted" -> "Accepted — workout started"
    "Rejected" -> "Skipped"
    else -> "Pending"
}
