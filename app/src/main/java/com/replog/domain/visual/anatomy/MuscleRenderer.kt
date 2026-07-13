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

/**
 * Cached immutable stroke styles ensuring 0 object allocations during frame drawing.
 */
private object RenderStyles {
    val silhouetteStroke = Stroke(width = 4f)
    val primaryStroke = Stroke(width = 5f)
    val secondaryStroke = Stroke(
        width = 4f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
    )
}

/**
 * Pure offline vector muscle renderer drawing anatomical silhouettes and layered
 * muscle activations with accessibility contrast and zero draw loop allocations.
 */
object MuscleRenderer {

    fun drawBody(
        drawScope: DrawScope,
        body: VectorBody,
        primaryRegions: Set<MuscleRegion>,
        secondaryRegions: Set<MuscleRegion>,
        palette: AnatomyPalette
    ) {
        with(drawScope) {
            val scaleX = size.width / 500f
            val scaleY = size.height / 1000f

            withTransform({
                scale(scaleX, scaleY, pivot = Offset.Zero)
            }) {
                // 1. Draw Base Silhouette
                drawPath(path = body.silhouettePath, color = palette.bodyFill)
                drawPath(path = body.silhouettePath, color = palette.bodyOutline, style = RenderStyles.silhouetteStroke)

                // 2. Draw Secondary Muscles (Dashed outline + reduced alpha fill for accessibility)
                for (region in secondaryRegions) {
                    val path = body.regionPaths[region] ?: continue
                    drawPath(path = path, color = palette.secondaryFill.copy(alpha = 0.28f))
                    drawPath(path = path, color = palette.secondaryOutline, style = RenderStyles.secondaryStroke)
                }

                // 3. Draw Primary Muscles (Solid dominant fill + bold continuous stroke)
                for (region in primaryRegions) {
                    val path = body.regionPaths[region] ?: continue
                    drawPath(path = path, color = palette.primaryFill.copy(alpha = 0.88f))
                    drawPath(path = path, color = palette.primaryOutline, style = RenderStyles.primaryStroke)
                }
            }
        }
    }
}

/**
 * Composable rendering front and back anatomical vector diagrams from an AnatomySpec.
 */
@Composable
fun AnatomicalMuscleDiagram(
    anatomySpec: AnatomySpec,
    modifier: Modifier = Modifier,
    palette: AnatomyPalette = AnatomyPalette.default()
) {
    val primaryRegions = remember(anatomySpec.primaryMuscles) { MuscleMap.mapPrimary(anatomySpec) }
    val secondaryRegions = remember(anatomySpec.secondaryMuscles) { MuscleMap.mapSecondary(anatomySpec) }

    val desc = buildString {
        append("Anatomical vector diagram. ")
        append("Primary target muscles: ${if (primaryRegions.isEmpty()) "none" else primaryRegions.joinToString { it.displayName }}. ")
        append("Secondary supporting muscles: ${if (secondaryRegions.isEmpty()) "none" else secondaryRegions.joinToString { it.displayName }}.")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                MuscleRenderer.drawBody(
                    drawScope = this,
                    body = VectorBody.FRONT,
                    primaryRegions = primaryRegions,
                    secondaryRegions = secondaryRegions,
                    palette = palette
                )
            }
            Text("Anterior (Front)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                MuscleRenderer.drawBody(
                    drawScope = this,
                    body = VectorBody.BACK,
                    primaryRegions = primaryRegions,
                    secondaryRegions = secondaryRegions,
                    palette = palette
                )
            }
            Text("Posterior (Back)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
