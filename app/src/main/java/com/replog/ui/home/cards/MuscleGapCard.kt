package com.replog.ui.home.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.musclegap.MuscleGapSuggestion
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SectionHeader

// Phase 2 Gap 4: dedicated muscle-gap card on Home with its own visual
// hierarchy. Each row carries a per-muscle "Start focus workout" CTA wired
// to the existing IntelligenceRepository.startMuscleGapWorkout() helper,
// which creates a session with the top-ranked exercises and sets the active
// session id in DataStore — Home then navigates to the workout tab.
@Composable
fun MuscleGapCard(
    suggestions: List<MuscleGapSuggestion>,
    onStartMuscle: (String) -> Unit,
    onOpen: () -> Unit
) = RepLogCard {
    SectionHeader("MUSCLES YOU'VE BEEN SKIPPING", Icons.Default.Healing)
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
fun MuscleGapRow(
    s: MuscleGapSuggestion,
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
