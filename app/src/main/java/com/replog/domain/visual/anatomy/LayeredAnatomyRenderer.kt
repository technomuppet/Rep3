package com.replog.domain.visual.anatomy

import android.content.Context
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caverock.androidsvg.SVG
import com.replog.domain.visual.spec.AnatomySpec

/**
 * Compose renderer for the layered SVG anatomy library.
 *
 * Draw order for each side:
 * 1. base anatomy SVG
 * 2. deterministic overlay SVGs for active regions only
 *
 * The renderer treats each SVG as a complete transparent document. It does not
 * parse path IDs, mutate fills/strokes, or generate procedural anatomy geometry.
 */
object LayeredAnatomyRenderer {

    fun layersFor(side: BodySide, activeRegions: Set<MuscleRegion>): List<AnatomyLayerAsset> {
        val layers = listOf(MuscleAssetRegistry.baseFor(side)) +
            MuscleAssetRegistry.overlayAssetsFor(side, activeRegions)
        RendererDiagnostics.layerStack(side, layers)
        return layers
    }

    @Suppress("UNUSED_PARAMETER")
    fun drawSide(
        drawScope: DrawScope,
        context: Context,
        side: BodySide,
        activeRegions: Set<MuscleRegion>,
        activations: List<MuscleActivationEngine.Activation> = emptyList(),
        options: AnatomyRenderOptions = AnatomyRenderOptions()
    ) {
        drawLayers(drawScope, context, layersFor(side, activeRegions))
    }

    fun drawLayers(
        drawScope: DrawScope,
        context: Context,
        layers: List<AnatomyLayerAsset>
    ) {
        if (layers.isEmpty()) return
        with(drawScope) {
            if (size.width <= 0f || size.height <= 0f) return
            for (layer in layers) {
                val svg = SVGCache.get(context, layer.assetPath) ?: continue
                renderSvgLayer(svg = svg, assetPath = layer.assetPath)
            }
        }
    }

    private fun DrawScope.renderSvgLayer(svg: SVG, assetPath: String) {
        val viewportWidth = MuscleAssetRegistry.CANVAS_WIDTH
        val viewportHeight = MuscleAssetRegistry.CANVAS_HEIGHT
        val scale = minOf(size.width / viewportWidth, size.height / viewportHeight)
        if (scale <= 0f || !scale.isFinite()) return

        val renderWidth = viewportWidth * scale
        val renderHeight = viewportHeight * scale
        val dx = (size.width - renderWidth) / 2f
        val dy = (size.height - renderHeight) / 2f
        val destination = RectF(dx, dy, dx + renderWidth, dy + renderHeight)

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            var saveCount = -1
            try {
                saveCount = nativeCanvas.save()
                // Explicit clipping prevents any oversized document content from
                // bleeding outside the intended anatomy canvas while preserving
                // transparency inside the layer.
                nativeCanvas.clipRect(destination)
                // AndroidSVG maps the SVG viewBox/document dimensions into this
                // viewport, preserving fills, strokes, transforms, clipping and
                // anti-aliasing handled by Android's Canvas/Paint pipeline.
                svg.renderToCanvas(nativeCanvas, destination)
            } catch (e: Exception) {
                RendererDiagnostics.renderFailure(assetPath, e.message)
            } finally {
                if (saveCount >= 0) {
                    try {
                        nativeCanvas.restoreToCount(saveCount)
                    } catch (ignored: Exception) {
                        RendererDiagnostics.renderFailure(assetPath, ignored.message)
                    }
                }
            }
        }
    }
}

@Composable
fun LayeredAnatomyDiagram(
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
    // intensity/animation overlay support. The current complete-SVG assets render
    // as-authored, so no per-frame activation calculation is needed.
    @Suppress("UNUSED_VARIABLE") val retainedProgress = progress
    @Suppress("UNUSED_VARIABLE") val retainedFamilyId = familyId
    @Suppress("UNUSED_VARIABLE") val retainedOptionsForFutureOverlays = options

    val frontLayers = remember(activeRegions) { LayeredAnatomyRenderer.layersFor(BodySide.FRONT, activeRegions) }
    val backLayers = remember(activeRegions) { LayeredAnatomyRenderer.layersFor(BodySide.BACK, activeRegions) }

    val desc = remember(primaryRegions, secondaryRegions, stabiliserRegions) {
        buildString {
            append("Layered anatomical SVG diagram. ")
            append("Primary: ${if (primaryRegions.isEmpty()) "none" else primaryRegions.joinToString { it.displayName }}. ")
            append("Secondary: ${if (secondaryRegions.isEmpty()) "none" else secondaryRegions.joinToString { it.displayName }}.")
            if (stabiliserRegions.isNotEmpty()) append(" Stabilisers: ${stabiliserRegions.joinToString { it.displayName }}.")
        }
    }

    val localContext = LocalContext.current
    val appContext = remember(localContext) { localContext.applicationContext ?: localContext }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AnatomyUiTags.DIAGRAM)
            .semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                LayeredAnatomyRenderer.drawLayers(
                    drawScope = this,
                    context = appContext,
                    layers = frontLayers
                )
            }
            Text("Anterior (Front)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                LayeredAnatomyRenderer.drawLayers(
                    drawScope = this,
                    context = appContext,
                    layers = backLayers
                )
            }
            Text("Posterior (Back)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
