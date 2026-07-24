package com.replog.domain.visual.anatomy

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.replog.domain.visual.spec.AnatomySpec
import org.junit.Rule
import org.junit.Test

class AnatomicalMuscleDiagramComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anatomicalMuscleDiagram_composesWithEmptySpec() {
        composeRule.setContent {
            MaterialTheme {
                AnatomicalMuscleDiagram(anatomySpec = AnatomySpec())
            }
        }

        composeRule.onNodeWithTag(AnatomyUiTags.DIAGRAM).assertExists()
        composeRule.onNodeWithTag(AnatomyUiTags.DIAGRAM).assertContentDescriptionContains("Primary: none")
    }

    @Test
    fun anatomicalMuscleDiagram_composesRepresentativeSpecs() {
        val specs = listOf(
            AnatomySpec(primaryMuscles = setOf(Muscles.CHEST), secondaryMuscles = setOf(Muscles.TRICEPS)),
            AnatomySpec(primaryMuscles = setOf(Muscles.QUADRICEPS, Muscles.GLUTE_MAXIMUS), secondaryMuscles = setOf(Muscles.HAMSTRINGS)),
            AnatomySpec(primaryMuscles = setOf(Muscles.LATISSIMUS_DORSI), secondaryMuscles = setOf(Muscles.BICEPS, Muscles.MIDDLE_TRAPEZIUS)),
            AnatomySpec(primaryMuscles = setOf(Muscles.ANTERIOR_DELTOID, Muscles.LATERAL_DELTOID), secondaryMuscles = setOf(Muscles.TRICEPS))
        )

        specs.forEach { spec ->
            composeRule.setContent {
                MaterialTheme {
                    AnatomicalMuscleDiagram(anatomySpec = spec)
                }
            }
            composeRule.onNodeWithTag(AnatomyUiTags.DIAGRAM).assertExists()
        }
    }

    @Test
    fun anatomicalMuscleDiagram_recompositionUpdatesSemantics() {
        val spec = mutableStateOf(AnatomySpec(primaryMuscles = setOf(Muscles.CHEST)))

        composeRule.setContent {
            MaterialTheme {
                AnatomicalMuscleDiagram(anatomySpec = spec.value)
            }
        }

        composeRule.onNodeWithTag(AnatomyUiTags.DIAGRAM).assertContentDescriptionContains("Chest")

        composeRule.runOnIdle {
            spec.value = AnatomySpec(primaryMuscles = setOf(Muscles.QUADRICEPS))
        }

        composeRule.onNodeWithTag(AnatomyUiTags.DIAGRAM).assertContentDescriptionContains("Quadriceps")
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertContentDescriptionContains(
        expected: String
    ) {
        assert(SemanticsMatcher("contentDescription contains '$expected'") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)
                ?.any { it.contains(expected) } == true
        })
    }
}
