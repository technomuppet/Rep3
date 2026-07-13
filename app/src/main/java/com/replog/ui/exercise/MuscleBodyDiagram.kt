package com.replog.ui.exercise

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.replog.data.model.Exercise
import com.replog.domain.visual.anatomy.AnatomicalMuscleDiagram
import com.replog.ui.exercise.adapter.VisualEngineAdapter

/**
 * Body diagram presentation — RC20.4 Production Polish
 * Now uses only commercial vector Anatomical Muscle Renderer, legacy boxes removed.
 * Synchronised muscle activation via progress available in AnatomicalMuscleDiagram.
 */
@Composable
fun MuscleBodyDiagram(exercise: Exercise, modifier: Modifier = Modifier) {
    when (val mode = VisualEngineAdapter.resolveAnatomy(exercise)) {
        is VisualEngineAdapter.AnatomyRenderMode.VectorEngine -> {
            AnatomicalMuscleDiagram(
                anatomySpec = mode.spec,
                modifier = modifier
            )
        }
    }
}
