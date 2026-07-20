package com.replog.domain.visual.anatomy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

    /**
     * Modern modular drawing pass — Version 2 Visual Engine
     * Consumes independent BodyRegion structures and draws them individually.
     * Fully decoupled from exercise specifications or raw bodies.
     */
    fun drawRegions(
        drawScope: DrawScope,
        regions: List<BodyRegion>,
        silhouettePath: Path,
        primaryRegions: Set<MuscleRegion>,
        secondaryRegions: Set<MuscleRegion>,
        palette: AnatomyPalette,
        activations: List<MuscleActivationEngine.Activation>,
        stabiliserRegions: Set<MuscleRegion> = emptySet()
    ) {
        with(drawScope) {
            val scaleX = size.width / 500f
            val scaleY = size.height / 1000f
            
            withTransform({ scale(scaleX, scaleY, pivot = Offset.Zero) }) {
                // 1. Draw base human silhouette
                drawPath(path = silhouettePath, color = palette.bodyFill)
                drawPath(path = silhouettePath, color = palette.bodyOutline, style = RenderStyles.silhouetteStroke)

                val activationMap = activations.associateBy { it.region }

                // 2. Draw each independent muscle region individually
                for (region in regions) {
                    val isPrimary = primaryRegions.contains(region.id)
                    val isSecondary = secondaryRegions.contains(region.id)
                    val isStabiliser = stabiliserRegions.contains(region.id)
                    
                    if (!isPrimary && !isSecondary && !isStabiliser) {
                        // Inactive muscle region is drawn subdued
                        drawPath(path = region.path, color = palette.bodyFill.copy(alpha = 0.5f))
                        continue
                    }

                    val act = activationMap[region.id]
                    val factor = act?.factor ?: 0.5f

                    if (isPrimary) {
                        // Primary muscles: thick outline and animated activation fill opacity
                        val alpha = (0.45f + 0.5f * factor).coerceIn(0.4f, 0.95f)
                        val strokeWidth = 5f + 2f * factor
                        drawPath(path = region.path, color = palette.primaryFill.copy(alpha = alpha))
                        drawPath(path = region.path, color = palette.primaryOutline, style = Stroke(width = strokeWidth))
                    } else if (isSecondary) {
                        // Secondary muscles: moderate opacity and dashed border
                        val alpha = (0.18f + 0.37f * factor).coerceIn(0.15f, 0.7f)
                        val phaseAdjust = if (act?.phase == MuscleActivationEngine.Phase.ECCENTRIC) 0.9f else 1f
                        drawPath(path = region.path, color = palette.secondaryFill.copy(alpha = alpha * phaseAdjust))
                        drawPath(path = region.path, color = palette.secondaryOutline, style = RenderStyles.secondaryStroke)
                    } else if (isStabiliser) {
                        // Stabiliser muscles: light blue-teal opacity and fine outline
                        val alpha = (0.22f + 0.28f * factor).coerceIn(0.2f, 0.6f)
                        drawPath(path = region.path, color = palette.stabiliserFill.copy(alpha = alpha))
                        drawPath(path = region.path, color = palette.stabiliserOutline, style = Stroke(width = 3f))
                    }
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
    val stabiliserRegions = remember(anatomySpec.stabiliserMuscles) {
        anatomySpec.stabiliserMuscles.mapNotNull { name ->
            try {
                MuscleRegion.valueOf(name.uppercase())
            } catch (e: Exception) {
                null
            }
        }.toSet()
    }

    val activations = if (progress != null && familyId != null) {
        remember(anatomySpec, progress, familyId) {
            MuscleActivationEngine.calculateActivations(anatomySpec, progress, familyId)
        }
    } else emptyList()

    val desc = buildString {
        append("Anatomical vector diagram. ")
        append("Primary: ${if (primaryRegions.isEmpty()) "none" else primaryRegions.joinToString { it.displayName }}. ")
        append("Secondary: ${if (secondaryRegions.isEmpty()) "none" else secondaryRegions.joinToString { it.displayName }}.")
        if (stabiliserRegions.isNotEmpty()) append(" Stabilisers: ${stabiliserRegions.joinToString { it.displayName }}.")
        if (activations.isNotEmpty()) append(" Activation synchronized with movement phase.")
    }

    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                val regions = V2AnatomyModel.loadRegionsForSide(isFront = true, context = context)
                MuscleRenderer.drawRegions(
                    drawScope = this,
                    regions = regions,
                    silhouettePath = V2AnatomyModel.frontSilhouette,
                    primaryRegions = primaryRegions,
                    secondaryRegions = secondaryRegions,
                    palette = palette,
                    activations = activations,
                    stabiliserRegions = stabiliserRegions
                )
            }
            Text("Anterior (Front)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                val regions = V2AnatomyModel.loadRegionsForSide(isFront = false, context = context)
                MuscleRenderer.drawRegions(
                    drawScope = this,
                    regions = regions,
                    silhouettePath = V2AnatomyModel.rearSilhouette,
                    primaryRegions = primaryRegions,
                    secondaryRegions = secondaryRegions,
                    palette = palette,
                    activations = activations,
                    stabiliserRegions = stabiliserRegions
                )
            }
            Text("Posterior (Back)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
