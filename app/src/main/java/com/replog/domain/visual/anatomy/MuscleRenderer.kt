package com.replog.domain.visual.anatomy

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.visual.spec.AnatomySpec

private object RenderStyles {
    val silhouetteStroke = Stroke(width = 4f)
    val primaryStroke = Stroke(width = 5f)
    val secondaryStroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f))
}

object MuscleRenderer {

    fun drawBody(
        drawScope: DrawScope,
        body: VectorBody,
        primaryRegions: Set<MuscleRegion>,
        secondaryRegions: Set<MuscleRegion>,
        palette: AnatomyPalette
    ) {
        drawBodyWithActivation(
            drawScope = drawScope,
            body = body,
            primaryRegions = primaryRegions,
            secondaryRegions = secondaryRegions,
            activations = emptyList(),
            palette = palette
        )
    }

    /**
     * RC20.4: Synchronised muscle activation with animation progress.
     * Primary muscles visibly contract (alpha 0.45-0.95 based on activation factor),
     * secondary appropriately lower (0.2-0.7), supports eccentric vs concentric via phase.
     * Drive from movement phase, not fake.
     */
    fun drawBodyWithActivation(
        drawScope: DrawScope,
        body: VectorBody,
        primaryRegions: Set<MuscleRegion>,
        secondaryRegions: Set<MuscleRegion>,
        activations: List<MuscleActivationEngine.Activation>,
        palette: AnatomyPalette
    ) {
        with(drawScope) {
            val scaleX = size.width / 500f
            val scaleY = size.height / 1000f
            withTransform({ scale(scaleX, scaleY, pivot = Offset.Zero) }) {
                drawPath(path = body.silhouettePath, color = palette.bodyFill)
                drawPath(path = body.silhouettePath, color = palette.bodyOutline, style = RenderStyles.silhouetteStroke)

                // Build activation map for quick lookup
                val activationMap = activations.associateBy { it.region }

                // Secondary with activation-based alpha
                for (region in secondaryRegions) {
                    val path = body.regionPaths[region] ?: continue
                    val act = activationMap[region]
                    val baseAlpha = 0.28f
                    val factor = act?.factor ?: 0.5f
                    // Secondary alpha 0.18-0.55 based on factor
                    val alpha = (0.18f + 0.37f * factor).coerceIn(0.15f, 0.7f)
                    // Eccentric vs concentric emphasis: if eccentric, slightly lower alpha
                    val phaseAdjust = if (act?.phase == MuscleActivationEngine.Phase.ECCENTRIC) 0.9f else 1f
                    drawPath(path = path, color = palette.secondaryFill.copy(alpha = alpha * phaseAdjust))
                    drawPath(path = path, color = palette.secondaryOutline, style = RenderStyles.secondaryStroke)
                }

                // Primary with activation-based alpha and stroke emphasis
                for (region in primaryRegions) {
                    val path = body.regionPaths[region] ?: continue
                    val act = activationMap[region]
                    val factor = act?.factor ?: 0.85f
                    // Primary alpha 0.45-0.95 based on activation, visible contraction
                    val alpha = (0.45f + 0.5f * factor).coerceIn(0.4f, 0.95f)
                    val strokeWidth = 5f + 2f * factor // thicker when contracted
                    drawPath(path = path, color = palette.primaryFill.copy(alpha = alpha))
                    drawPath(
                        path = path,
                        color = palette.primaryOutline,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
    }
}

@Composable
fun AnatomicalMuscleDiagram(
    anatomySpec: AnatomySpec,
    modifier: Modifier = Modifier,
    palette: AnatomyPalette = AnatomyPalette.default(),
    progress: Float? = null,
    familyId: String? = null
) {
    val primaryRegions = remember(anatomySpec.primaryMuscles) { MuscleMap.mapPrimary(anatomySpec) }
    val secondaryRegions = remember(anatomySpec.secondaryMuscles) { MuscleMap.mapSecondary(anatomySpec) }

    val activations = if (progress != null && familyId != null) {
        remember(anatomySpec, progress, familyId) {
            MuscleActivationEngine.calculateActivations(anatomySpec, progress, familyId)
        }
    } else emptyList()

    val desc = buildString {
        append("Anatomical vector diagram. ")
        append("Primary: ${if (primaryRegions.isEmpty()) "none" else primaryRegions.joinToString { it.displayName }}. ")
        append("Secondary: ${if (secondaryRegions.isEmpty()) "none" else secondaryRegions.joinToString { it.displayName }}.")
        if (activations.isNotEmpty()) append(" Activation synchronized with movement phase.")
    }

    Row(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                MuscleRenderer.drawBodyWithActivation(
                    drawScope = this,
                    body = VectorBody.FRONT,
                    primaryRegions = primaryRegions,
                    secondaryRegions = secondaryRegions,
                    activations = activations,
                    palette = palette
                )
            }
            Text("Anterior (Front)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                MuscleRenderer.drawBodyWithActivation(
                    drawScope = this,
                    body = VectorBody.BACK,
                    primaryRegions = primaryRegions,
                    secondaryRegions = secondaryRegions,
                    activations = activations,
                    palette = palette
                )
            }
            Text("Posterior (Back)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
