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
            primaryMuscles = setOf("Chest", "Anterior Deltoid"),
            secondaryMuscles = setOf("Triceps")
        )
    )
}

@Preview(name = "2. Back Squat Anatomy", showBackground = true)
@Composable
fun BackSquatAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Barbell Back Squat",
        spec = AnatomySpec(
            primaryMuscles = setOf("Quadriceps", "Glute Maximus"),
            secondaryMuscles = setOf("Hamstrings", "Spinal Erectors", "Calves")
        )
    )
}

@Preview(name = "3. Deadlift Anatomy", showBackground = true)
@Composable
fun DeadliftAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Barbell Deadlift",
        spec = AnatomySpec(
            primaryMuscles = setOf("Glute Maximus", "Hamstrings", "Spinal Erectors"),
            secondaryMuscles = setOf("Quadriceps", "Upper Trapezius", "Forearms")
        )
    )
}

@Preview(name = "4. Pull-up Anatomy", showBackground = true)
@Composable
fun PullUpAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Pull Up",
        spec = AnatomySpec(
            primaryMuscles = setOf("Latissimus Dorsi", "Teres Major"),
            secondaryMuscles = setOf("Biceps", "Rhomboids", "Middle Trapezius")
        )
    )
}

@Preview(name = "5. Overhead Press Anatomy", showBackground = true)
@Composable
fun OverheadPressAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Barbell Overhead Press",
        spec = AnatomySpec(
            primaryMuscles = setOf("Anterior Deltoid", "Lateral Deltoid"),
            secondaryMuscles = setOf("Triceps", "Upper Chest")
        )
    )
}

@Preview(name = "6. Biceps Curl Anatomy", showBackground = true)
@Composable
fun BicepsCurlAnatomyPreview() {
    AnatomyExerciseCard(
        title = "Barbell Biceps Curl",
        spec = AnatomySpec(
            primaryMuscles = setOf("Biceps"),
            secondaryMuscles = setOf("Anterior Forearms")
        )
    )
}
