package com.replog.ui.exercise

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.data.model.Exercise
import com.replog.domain.library.CoachingInfo
import com.replog.domain.library.ConfidenceCard
import com.replog.domain.library.EasierAlternative
import com.replog.ui.components.RepLogCard

/**
 * Reusable progressive-disclosure section (Phase 6). Header is always visible and
 * tappable; body expands on demand. Uses the app's existing ExpandMore/ExpandLess
 * idiom. Accessible: the whole header row carries a state-describing
 * contentDescription for TalkBack.
 */
@Composable
fun ExpandableSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    RepLogCard(onClick = { expanded = !expanded }, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = if (expanded) "$title, expanded. Tap to collapse." else "$title, collapsed. Tap to expand." }
        ) {
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content()
            }
        }
    }
}

/** The always-visible confidence card (Phase 2). */
@Composable
fun ConfidenceCardView(card: ConfidenceCard, modifier: Modifier = Modifier) {
    RepLogCard(modifier = modifier) {
        Text("Beginner confidence", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        LabeledRow("Difficulty", "${difficultyMarker(card.difficulty)} ${card.difficulty}")
        LabeledRow("Equipment", card.equipment)
        LabeledRow("Estimated learning time", "${card.estimatedLearningMinutes} minutes")
        LabeledRow("Ideal experience", card.idealExperience)
    }
}

/** Collapsed-by-default coaching sections (Phase 6). */
@Composable
fun CoachingSections(
    exercise: Exercise,
    coaching: CoachingInfo,
    why: String,
    easier: EasierAlternative?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExpandableSection("How to Perform") {
            coaching.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(4.dp))
            Text("How far to move", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(coaching.rangeOfMotion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ExpandableSection("What You Should Feel") {
            Text("You should mainly feel:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            coaching.feel.forEach { Text("\u2022 $it", style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(6.dp))
            Text("You should NOT feel:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            coaching.notFeel.forEach { Text("\u2022 $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(6.dp))
            Text("If something hurts, stop and try the easier variation below.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ExpandableSection("Common Mistakes") {
            coaching.mistakes.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
        }
        ExpandableSection("Breathing") {
            Text(coaching.breathing, style = MaterialTheme.typography.bodyMedium)
        }
        ExpandableSection("Tempo") {
            Text(coaching.tempo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(coaching.tempoExplanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ExpandableSection("Safety") {
            coaching.safety.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
        }
        ExpandableSection("Why RepLog Recommends This") {
            Text(why, style = MaterialTheme.typography.bodyMedium)
        }
        // Phase 3: the Alternatives section is ALWAYS shown - either a recommended
        // easier variation, or an explanation of why none is suggested.
        ExpandableSection("Alternatives", initiallyExpanded = false) {
            if (easier != null) {
                Text("Recommended first", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text(easier.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(easier.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val msg = when (exercise.difficulty.lowercase()) {
                    "beginner" -> "This is already a beginner-friendly exercise, so there is no easier version to learn first."
                    "custom" -> "This is a custom exercise, so RepLog does not suggest an easier variation."
                    else -> "This is already one of the most accessible options for this movement, so there is no easier variation to recommend first."
                }
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A text marker that does not rely on colour alone (Phase 8). */
private fun difficultyMarker(difficulty: String): String = when (difficulty.lowercase()) {
    "beginner" -> "[Easy]"
    "intermediate" -> "[Moderate]"
    "advanced" -> "[Hard]"
    else -> "[Custom]"
}
