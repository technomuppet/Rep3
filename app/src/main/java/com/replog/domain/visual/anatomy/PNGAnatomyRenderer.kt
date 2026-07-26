package com.replog.domain.visual.anatomy

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.visual.spec.AnatomySpec

/**
 * Compose-only renderer for the layered PNG anatomy library.
 *
 * Draw order for each side:
 * 1. base anatomy PNG
 * 2. deterministic overlay PNGs for active regions only
 *
 * Every PNG is authored on the same transparent canvas and is rendered as a
 * full-size Image inside the same Box so front/back bases and overlays remain
 * pixel-aligned without SVG parsing, Canvas drawing or asset loading.
 */
object PNGAnatomyRenderer {

    fun layersFor(side: BodySide, activeRegions: Set<MuscleRegion>): List<PngAnatomyLayerAsset> {
        val layers = listOf(MusclePngRegistry.baseFor(side)) +
            MusclePngRegistry.overlayAssetsFor(side, activeRegions)
        RendererDiagnostics.pngLayerStack(side, layers)
        return layers
    }

    @Composable
    fun DrawLayers(
        layers: List<PngAnatomyLayerAsset>,
        modifier: Modifier = Modifier
    ) {
        Box(modifier = modifier) {
            layers.forEach { layer ->
                Image(
                    painter = painterResource(id = layer.drawableId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }
    }
}

@Composable
fun PNGAnatomyDiagram(
    anatomySpec: AnatomySpec,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    familyId: String? = null,
    options: AnatomyRenderOptions = AnatomyRenderOptions()
) {
    val primaryRegions = remember(anatomySpec) { MuscleMap.mapPrimary(anatomySpec) }
    val secondaryRegions = remember(anatomySpec) { MuscleMap.mapSecondary(anatomySpec) }
    val stabiliserRegions = remember(anatomySpec) {
        anatomySpec.stabiliserMuscles.flatMap { name -> MuscleMap.resolveRegions(name) }.toSet()
    }
    val activeRegions = remember(primaryRegions, secondaryRegions, stabiliserRegions) {
        primaryRegions + secondaryRegions + stabiliserRegions
    }

    // progress, familyId and options are retained in the public API for future
    // intensity/animation overlay support. The current complete-PNG assets render
    // as-authored, so no per-frame activation calculation is needed.
    @Suppress("UNUSED_VARIABLE") val retainedProgress = progress
    @Suppress("UNUSED_VARIABLE") val retainedFamilyId = familyId
    @Suppress("UNUSED_VARIABLE") val retainedOptionsForFutureOverlays = options

    val frontLayers = remember(activeRegions) { PNGAnatomyRenderer.layersFor(BodySide.FRONT, activeRegions) }
    val backLayers = remember(activeRegions) { PNGAnatomyRenderer.layersFor(BodySide.BACK, activeRegions) }

    val desc = remember(primaryRegions, secondaryRegions, stabiliserRegions) {
        buildString {
            append("Layered anatomical PNG diagram. ")
            append("Primary: ${if (primaryRegions.isEmpty()) "none" else primaryRegions.joinToString { it.displayName }}. ")
            append("Secondary: ${if (secondaryRegions.isEmpty()) "none" else secondaryRegions.joinToString { it.displayName }}.")
            if (stabiliserRegions.isNotEmpty()) append(" Stabilisers: ${stabiliserRegions.joinToString { it.displayName }}.")
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AnatomyUiTags.DIAGRAM)
            .semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            PNGAnatomyRenderer.DrawLayers(
                layers = frontLayers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
            Text("Anterior (Front)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            PNGAnatomyRenderer.DrawLayers(
                layers = backLayers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
            Text("Posterior (Back)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
