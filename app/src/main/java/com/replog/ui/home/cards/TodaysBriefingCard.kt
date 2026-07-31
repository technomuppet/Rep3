package com.replog.ui.home.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.intelligence.BriefingConfidence
import com.replog.domain.intelligence.TodaysBriefing
import com.replog.ui.components.RepLogCard

fun confidenceText(c: BriefingConfidence): String = when (c) {
    BriefingConfidence.HIGH -> "High"
    BriefingConfidence.MEDIUM -> "Medium"
    BriefingConfidence.LOW -> "Low"
}

@Composable
fun TodaysBriefingCard(
    b: TodaysBriefing,
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
