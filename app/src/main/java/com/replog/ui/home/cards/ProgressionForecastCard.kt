package com.replog.ui.home.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.data.repository.ForecastCardEntry
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SectionHeader

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
fun ProgressionForecastCard(
    forecasts: List<ForecastCardEntry>,
    onOpen: () -> Unit
) = RepLogCard {
    val headline = forecasts.first()
    SectionHeader("PROGRESS FORECAST", Icons.Default.TrendingUp)
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
fun ProgressionForecastRow(
    f: ForecastCardEntry,
    isHeadline: Boolean
) {
    val trendGlyph = f.trend.glyph
    val trendLabel = f.trend.label
    val confidenceLabel = f.confidence.label
    val confidenceColor = f.confidence.accent
    val weeklyGainText = if (f.weeklyGainKg >= 0.0)
        "+${\"%.1f\".format(f.weeklyGainKg)} kg/wk"
    else
        "${\"%.1f\".format(f.weeklyGainKg)} kg/wk"
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
