mport com.replog.domain.visual.anatomy.Muscles
package com.replog.domain.visual.anatomy

mport com.replog.domain.visual.anatomy.Muscles

mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.foundation.layout.Arrangement
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.foundation.layout.Column
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.foundation.layout.Spacer
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.foundation.layout.fillMaxWidth
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.foundation.layout.height
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.foundation.layout.padding
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.material3.Card
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.material3.MaterialTheme
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.material3.Text
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.runtime.Composable
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.ui.Modifier
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.ui.text.font.FontWeight
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.ui.tooling.preview.Preview
mport com.replog.domain.visual.anatomy.Muscles
import androidx.compose.ui.unit.dp
mport com.replog.domain.visual.anatomy.Muscles
import com.replog.domain.visual.spec.AnatomySpec
mport com.replog.domain.visual.anatomy.Muscles

mport com.replog.domain.visual.anatomy.Muscles
/**
mport com.replog.domain.visual.anatomy.Muscles
 * Preview showcases for standard movement archetypes verifying visual hierarchy,
mport com.replog.domain.visual.anatomy.Muscles
 * bilateral symmetry, and accessibility contrast across anterior and posterior views.
mport com.replog.domain.visual.anatomy.Muscles
 */
mport com.replog.domain.visual.anatomy.Muscles
@Composable
mport com.replog.domain.visual.anatomy.Muscles
fun AnatomyExerciseCard(title: String, spec: AnatomySpec, modifier: Modifier = Modifier) {
mport com.replog.domain.visual.anatomy.Muscles
    Card(modifier = modifier.fillMaxWidth().padding(8.dp)) {
mport com.replog.domain.visual.anatomy.Muscles
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
mport com.replog.domain.visual.anatomy.Muscles
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
mport com.replog.domain.visual.anatomy.Muscles
            AnatomicalMuscleDiagram(anatomySpec = spec)
mport com.replog.domain.visual.anatomy.Muscles
        }
mport com.replog.domain.visual.anatomy.Muscles
    }
mport com.replog.domain.visual.anatomy.Muscles
}
mport com.replog.domain.visual.anatomy.Muscles

mport com.replog.domain.visual.anatomy.Muscles
@Preview(name = "1. Bench Press Anatomy", showBackground = true)
mport com.replog.domain.visual.anatomy.Muscles
@Composable
mport com.replog.domain.visual.anatomy.Muscles
fun BenchPressAnatomyPreview() {
mport com.replog.domain.visual.anatomy.Muscles
    AnatomyExerciseCard(
mport com.replog.domain.visual.anatomy.Muscles
        title = "Barbell Bench Press",
mport com.replog.domain.visual.anatomy.Muscles
        spec = AnatomySpec(
mport com.replog.domain.visual.anatomy.Muscles
            primaryMuscles = setOf(Muscles.CHEST, Muscles.ANTERIOR_DELTOID),
mport com.replog.domain.visual.anatomy.Muscles
            secondaryMuscles = setOf(Muscles.TRICEPS)
mport com.replog.domain.visual.anatomy.Muscles
        )
mport com.replog.domain.visual.anatomy.Muscles
    )
mport com.replog.domain.visual.anatomy.Muscles
}
mport com.replog.domain.visual.anatomy.Muscles

mport com.replog.domain.visual.anatomy.Muscles
@Preview(name = "2. Back Squat Anatomy", showBackground = true)
mport com.replog.domain.visual.anatomy.Muscles
@Composable
mport com.replog.domain.visual.anatomy.Muscles
fun BackSquatAnatomyPreview() {
mport com.replog.domain.visual.anatomy.Muscles
    AnatomyExerciseCard(
mport com.replog.domain.visual.anatomy.Muscles
        title = "Barbell Back Squat",
mport com.replog.domain.visual.anatomy.Muscles
        spec = AnatomySpec(
mport com.replog.domain.visual.anatomy.Muscles
            primaryMuscles = setOf(Muscles.QUADRICEPS, Muscles.GLUTE_MAXIMUS),
mport com.replog.domain.visual.anatomy.Muscles
            secondaryMuscles = setOf(Muscles.HAMSTRINGS, Muscles.SPINAL_ERECTORS, Muscles.CALVES)
mport com.replog.domain.visual.anatomy.Muscles
        )
mport com.replog.domain.visual.anatomy.Muscles
    )
mport com.replog.domain.visual.anatomy.Muscles
}
mport com.replog.domain.visual.anatomy.Muscles

mport com.replog.domain.visual.anatomy.Muscles
@Preview(name = "3. Deadlift Anatomy", showBackground = true)
mport com.replog.domain.visual.anatomy.Muscles
@Composable
mport com.replog.domain.visual.anatomy.Muscles
fun DeadliftAnatomyPreview() {
mport com.replog.domain.visual.anatomy.Muscles
    AnatomyExerciseCard(
mport com.replog.domain.visual.anatomy.Muscles
        title = "Barbell Deadlift",
mport com.replog.domain.visual.anatomy.Muscles
        spec = AnatomySpec(
mport com.replog.domain.visual.anatomy.Muscles
            primaryMuscles = setOf(Muscles.GLUTE_MAXIMUS, Muscles.HAMSTRINGS, Muscles.SPINAL_ERECTORS),
mport com.replog.domain.visual.anatomy.Muscles
            secondaryMuscles = setOf(Muscles.QUADRICEPS, Muscles.UPPER_TRAPEZIUS, Muscles.FOREARMS)
mport com.replog.domain.visual.anatomy.Muscles
        )
mport com.replog.domain.visual.anatomy.Muscles
    )
mport com.replog.domain.visual.anatomy.Muscles
}
mport com.replog.domain.visual.anatomy.Muscles

mport com.replog.domain.visual.anatomy.Muscles
@Preview(name = "4. Pull-up Anatomy", showBackground = true)
mport com.replog.domain.visual.anatomy.Muscles
@Composable
mport com.replog.domain.visual.anatomy.Muscles
fun PullUpAnatomyPreview() {
mport com.replog.domain.visual.anatomy.Muscles
    AnatomyExerciseCard(
mport com.replog.domain.visual.anatomy.Muscles
        title = "Pull Up",
mport com.replog.domain.visual.anatomy.Muscles
        spec = AnatomySpec(
mport com.replog.domain.visual.anatomy.Muscles
            primaryMuscles = setOf(Muscles.LATISSIMUS_DORSI, Muscles.TERES_MAJOR),
mport com.replog.domain.visual.anatomy.Muscles
            secondaryMuscles = setOf(Muscles.BICEPS, Muscles.RHOMBOIDS, Muscles.MIDDLE_TRAPEZIUS)
mport com.replog.domain.visual.anatomy.Muscles
        )
mport com.replog.domain.visual.anatomy.Muscles
    )
mport com.replog.domain.visual.anatomy.Muscles
}
mport com.replog.domain.visual.anatomy.Muscles

mport com.replog.domain.visual.anatomy.Muscles
@Preview(name = "5. Overhead Press Anatomy", showBackground = true)
mport com.replog.domain.visual.anatomy.Muscles
@Composable
mport com.replog.domain.visual.anatomy.Muscles
fun OverheadPressAnatomyPreview() {
mport com.replog.domain.visual.anatomy.Muscles
    AnatomyExerciseCard(
mport com.replog.domain.visual.anatomy.Muscles
        title = "Barbell Overhead Press",
mport com.replog.domain.visual.anatomy.Muscles
        spec = AnatomySpec(
mport com.replog.domain.visual.anatomy.Muscles
            primaryMuscles = setOf(Muscles.ANTERIOR_DELTOID, Muscles.LATERAL_DELTOID),
mport com.replog.domain.visual.anatomy.Muscles
            secondaryMuscles = setOf(Muscles.TRICEPS, Muscles.UPPER_CHEST)
mport com.replog.domain.visual.anatomy.Muscles
        )
mport com.replog.domain.visual.anatomy.Muscles
    )
mport com.replog.domain.visual.anatomy.Muscles
}
mport com.replog.domain.visual.anatomy.Muscles

mport com.replog.domain.visual.anatomy.Muscles
@Preview(name = "6. Biceps Curl Anatomy", showBackground = true)
mport com.replog.domain.visual.anatomy.Muscles
@Composable
mport com.replog.domain.visual.anatomy.Muscles
fun BicepsCurlAnatomyPreview() {
mport com.replog.domain.visual.anatomy.Muscles
    AnatomyExerciseCard(
mport com.replog.domain.visual.anatomy.Muscles
        title = "Barbell Biceps Curl",
mport com.replog.domain.visual.anatomy.Muscles
        spec = AnatomySpec(
mport com.replog.domain.visual.anatomy.Muscles
            primaryMuscles = setOf(Muscles.BICEPS),
mport com.replog.domain.visual.anatomy.Muscles
            secondaryMuscles = setOf(Muscles.FOREARMS)
mport com.replog.domain.visual.anatomy.Muscles
        )
mport com.replog.domain.visual.anatomy.Muscles
    )
mport com.replog.domain.visual.anatomy.Muscles
}
