package com.replog.domain.visual.anatomy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.replog.domain.visual.spec.AnatomySpec

/**
 * Preview showcases for standard movement archetypes verifying visual hierarchy,
 * bilateral symmetry, and accessibility contrast across anterior and posterior views.
 */
@Composable
fun AnatomyExerciseCard(title: String, spec: AnatomySpec, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AnatomicalMuscleDiagram(anatomySpec = spec)
        }
    }
}

@Preview(name = "1. Bench Press Anatomy", showBackground = true)
@Composable
fun BenchPressAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Barbell Bench Press",
        spec = AnatomySpec(
            primaryMuscles = setOf(Muscles.CHEST, Muscles.ANTERIOR_DELTOID),
            secondaryMuscles = setOf(Muscles.TRICEPS)
        )
    )
}

@Preview(name = "2. Back Squat Anatomy", showBackground = true)
@Composable
fun BackSquatAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Barbell Back Squat",
        spec = AnatomySpec(
            primaryMuscles = setOf(Muscles.QUADRICEPS, Muscles.GLUTE_MAXIMUS),
            secondaryMuscles = setOf(Muscles.HAMSTRINGS, Muscles.SPINAL_ERECTORS, Muscles.CALVES)
        )
    )
}

@Preview(name = "3. Deadlift Anatomy", showBackground = true)
@Composable
fun DeadliftAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Barbell Deadlift",
        spec = AnatomySpec(
            primaryMuscles = setOf(Muscles.GLUTE_MAXIMUS, Muscles.HAMSTRINGS, Muscles.SPINAL_ERECTORS),
            secondaryMuscles = setOf(Muscles.QUADRICEPS, Muscles.UPPER_TRAPEZIUS, Muscles.FOREARMS)
        )
    )
}

@Preview(name = "4. Pull-up Anatomy", showBackground = true)
@Composable
fun PullUpAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Pull Up",
        spec = AnatomySpec(
            primaryMuscles = setOf(Muscles.LATISSIMUS_DORSI, Muscles.TERES_MAJOR),
            secondaryMuscles = setOf(Muscles.BICEPS, Muscles.RHOMBOIDS, Muscles.MIDDLE_TRAPEZIUS)
        )
    )
}

@Preview(name = "5. Overhead Press Anatomy", showBackground = true)
@Composable
fun OverheadPressAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Barbell Overhead Press",
        spec = AnatomySpec(
            primaryMuscles = setOf(Muscles.ANTERIOR_DELTOID, Muscles.LATERAL_DELTOID),
            secondaryMuscles = setOf(Muscles.TRICEPS, Muscles.UPPER_CHEST)
        )
    )
}

@Preview(name = "6. Biceps Curl Anatomy", showBackground = true)
@Composable
fun BicepsCurlAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Barbell Biceps Curl",
        spec = AnatomySpec(
            primaryMuscles = setOf(Muscles.BICEPS),
            secondaryMuscles = setOf(Muscles.FOREARMS)
        )
    )
}
