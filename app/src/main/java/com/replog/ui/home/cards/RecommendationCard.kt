package com.replog.ui.home.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.data.repository.RecommendedWorkoutCardEntry
import com.replog.domain.intelligence.TodaysBriefing
import com.replog.domain.recommendation.WorkoutPlan
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SectionHeader

// Phase 2 Gap 3: dedicated recommendation card with its own visual hierarchy.
// Surfaces the IntelligenceEngine's `recommendation` string at weighty title
// size, gives the matching personalised coach-insight as a "Because: …" reason
// line, and provides a single primary CTA. The "Log a few more workouts" low-
// data fallback branch disables the CTA (it would have nothing concrete to
// start) and shows a soft caption instead, matching the briefing fallback.
@Composable
fun RecommendationCard(
    b: TodaysBriefing,
    onStart: () -> Unit,
    recommendedWorkout: RecommendedWorkoutCardEntry?,
    onStartPlan: (WorkoutPlan) -> Unit,
    onSaveTemplate: () -> Unit = {}
) = RepLogCard {
    SectionHeader("RECOMMENDED TODAY", Icons.Default.PlayArrow)
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
