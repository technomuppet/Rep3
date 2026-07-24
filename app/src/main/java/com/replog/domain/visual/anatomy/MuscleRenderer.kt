package com.replog.domain.visual.anatomy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.replog.domain.visual.spec.AnatomySpec

/**
 * Public anatomy diagram API preserved for existing UI callers.
 *
 * The active backend is [LayeredAnatomyRenderer], which composites complete SVG
 * assets from [MuscleAssetRegistry]. Inactive muscles are not rendered.
 */
@Composable
fun AnatomicalMuscleDiagram(
    anatomySpec: AnatomySpec,
    modifier: Modifier = Modifier,
    palette: AnatomyPalette = AnatomyPalette.default(),
    progress: Float? = null,
    familyId: String? = null
) {
    @Suppress("UNUSED_PARAMETER") val retainedForApiCompatibility = palette
    LayeredAnatomyDiagram(
        anatomySpec = anatomySpec,
        modifier = modifier,
        progress = progress,
        familyId = familyId
    )
}
