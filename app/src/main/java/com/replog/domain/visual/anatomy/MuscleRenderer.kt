package com.replog.domain.visual.anatomy

import android.content.Context
import android.graphics.BitmapFactory
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.replog.domain.visual.spec.AnatomySpec
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Premium anatomical illustration renderer.
 * Draws the bundled shaded medical artwork (anatomy/anatomy_illustration.png),
 * which contains the front figure in the left half of the sprite sheet and the
 * back figure in the right half. The legacy SVG vector pipeline was removed.
 */
@Composable
fun AnatomicalMuscleDiagram(
    anatomySpec: AnatomySpec,
    modifier: Modifier = Modifier,
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
        append("Premium anatomical illustration. ")
        append("Primary: ${if (primaryRegions.isEmpty()) "none" else primaryRegions.joinToString { it.displayName }}. ")
        append("Secondary: ${if (secondaryRegions.isEmpty()) "none" else secondaryRegions.joinToString { it.displayName }}.")
        if (stabiliserRegions.isNotEmpty()) append(" Stabilisers: ${stabiliserRegions.joinToString { it.displayName }}.")
        if (activations.isNotEmpty()) append(" Activation synchronized with movement phase.")
    }

    val context = LocalContext.current
    val anatomyImage = remember { loadAnatomyIllustration(context) }

    Row(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                anatomyImage?.let { image ->
                    // Premium PNG artwork: front figure occupies the left half of the sprite sheet.
                    drawAnatomyHalf(image = image, isFront = true)
                    drawMuscleHighlightOverlay(
                        image = image,
                        isFront = true,
                        primaryRegions = primaryRegions,
                        secondaryRegions = secondaryRegions,
                        stabiliserRegions = stabiliserRegions,
                        activations = activations
                    )
                }
            }
            Text("Anterior (Front)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                anatomyImage?.let { image ->
                    // Premium PNG artwork: back figure occupies the right half of the sprite sheet.
                    drawAnatomyHalf(image = image, isFront = false)
                    drawMuscleHighlightOverlay(
                        image = image,
                        isFront = false,
                        primaryRegions = primaryRegions,
                        secondaryRegions = secondaryRegions,
                        stabiliserRegions = stabiliserRegions,
                        activations = activations
                    )
                }
            }
            Text("Posterior (Back)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Decodes the premium anatomy sprite sheet once. The sheet is 1536x1024 and
 * contains the front figure in the left half and the back figure in the right
 * half. Returns null when the asset cannot be opened so callers can draw an
 * empty canvas instead of crashing.
 */
internal fun loadAnatomyIllustration(context: Context): ImageBitmap? = try {
    context.assets.open("anatomy/anatomy_illustration.png").use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (e: Exception) {
    null
}

/**
 * Draws one half of the anatomy sprite sheet into the current draw scope,
 * fitting it while preserving the figure's aspect ratio and centering it.
 */
internal fun DrawScope.drawAnatomyHalf(image: ImageBitmap, isFront: Boolean) {
    val frame = anatomyFrame(image, isFront)
    drawImage(
        image = image,
        srcOffset = IntOffset(frame.srcLeft, 0),
        srcSize = IntSize(frame.srcWidth, frame.srcHeight),
        dstOffset = IntOffset(frame.dstLeft.roundToInt(), frame.dstTop.roundToInt()),
        dstSize = IntSize(frame.dstWidth.roundToInt(), frame.dstHeight.roundToInt()),
        filterQuality = FilterQuality.Medium
    )
}

private data class AnatomyFrame(
    val srcLeft: Int,
    val srcWidth: Int,
    val srcHeight: Int,
    val dstLeft: Float,
    val dstTop: Float,
    val dstWidth: Float,
    val dstHeight: Float
)

private fun DrawScope.anatomyFrame(image: ImageBitmap, isFront: Boolean): AnatomyFrame {
    val halfWidth = image.width / 2
    val scale = min(size.width / halfWidth, size.height / image.height)
    val dstWidth = halfWidth * scale
    val dstHeight = image.height * scale
    return AnatomyFrame(
        srcLeft = if (isFront) 0 else halfWidth,
        srcWidth = halfWidth,
        srcHeight = image.height,
        dstLeft = (size.width - dstWidth) / 2f,
        dstTop = (size.height - dstHeight) / 2f,
        dstWidth = dstWidth,
        dstHeight = dstHeight
    )
}

/**
 * Adds restrained translucent activation marks over the PNG. The artwork remains
 * the visual base; this overlay restores exercise-specific target feedback without
 * reviving the discarded SVG pipeline. Coordinates are normalized to each figure
 * half so the overlay follows the same fit/crop calculation as the bitmap.
 */
private fun DrawScope.drawMuscleHighlightOverlay(
    image: ImageBitmap,
    isFront: Boolean,
    primaryRegions: Set<MuscleRegion>,
    secondaryRegions: Set<MuscleRegion>,
    stabiliserRegions: Set<MuscleRegion>,
    activations: List<MuscleActivationEngine.Activation>
) {
    val frame = anatomyFrame(image, isFront)
    val activationByRegion = activations.associateBy { it.region }
    val regions = (primaryRegions + secondaryRegions + stabiliserRegions)
        .filter { it.side == if (isFront) BodySide.FRONT else BodySide.BACK }
        .distinct()

    regions.forEach { region ->
        val anchor = region.anchor() ?: return@forEach
        val isPrimary = region in primaryRegions
        val isSecondary = region in secondaryRegions
        val activation = activationByRegion[region]?.factor ?: 1f
        val color = when {
            isPrimary -> Color(0xFFFF7043)
            isSecondary -> Color(0xFFFFC107)
            else -> Color(0xFF64B5F6)
        }
        val alpha = when {
            isPrimary -> 0.20f + activation * 0.24f
            isSecondary -> 0.14f + activation * 0.16f
            else -> 0.10f
        }
        val center = Offset(
            frame.dstLeft + anchor.x * frame.dstWidth,
            frame.dstTop + anchor.y * frame.dstHeight
        )
        val radius = Size(anchor.radiusX * frame.dstWidth, anchor.radiusY * frame.dstHeight)
        drawOval(
            color = color.copy(alpha = alpha.coerceIn(0f, 0.55f)),
            topLeft = Offset(center.x - radius.width, center.y - radius.height),
            size = Size(radius.width * 2f, radius.height * 2f)
        )
    }
}

private data class RegionAnchor(val x: Float, val y: Float, val radiusX: Float, val radiusY: Float)

/** Approximate normalized anchors for the bundled reference artwork. */
private fun MuscleRegion.anchor(): RegionAnchor? = when (this) {
    MuscleRegion.CHEST, MuscleRegion.UPPER_CHEST, MuscleRegion.MIDDLE_CHEST, MuscleRegion.LOWER_CHEST -> RegionAnchor(.50f, .29f, .16f, .06f)
    MuscleRegion.ANTERIOR_DELTOID, MuscleRegion.LATERAL_DELTOID -> RegionAnchor(.37f, .27f, .06f, .06f)
    MuscleRegion.BICEPS, MuscleRegion.BRACHIALIS -> RegionAnchor(.35f, .36f, .045f, .085f)
    MuscleRegion.FOREARMS_ANTERIOR -> RegionAnchor(.33f, .46f, .035f, .075f)
    MuscleRegion.RECTUS_ABDOMINIS, MuscleRegion.TRANSVERSE_ABDOMINIS -> RegionAnchor(.50f, .40f, .08f, .12f)
    MuscleRegion.OBLIQUES -> RegionAnchor(.40f, .40f, .055f, .10f)
    MuscleRegion.HIP_FLEXORS -> RegionAnchor(.45f, .52f, .06f, .055f)
    MuscleRegion.QUADRICEPS, MuscleRegion.RECTUS_FEMORIS, MuscleRegion.VASTUS_LATERALIS, MuscleRegion.VASTUS_MEDIALIS, MuscleRegion.VASTUS_INTERMEDIUS -> RegionAnchor(.44f, .67f, .075f, .15f)
    MuscleRegion.ADDUCTORS, MuscleRegion.ABDUCTORS -> RegionAnchor(.50f, .60f, .055f, .09f)
    MuscleRegion.TIBIALIS_ANTERIOR -> RegionAnchor(.45f, .86f, .045f, .12f)
    MuscleRegion.POSTERIOR_DELTOID -> RegionAnchor(.37f, .27f, .06f, .06f)
    MuscleRegion.TRICEPS -> RegionAnchor(.35f, .37f, .05f, .09f)
    MuscleRegion.FOREARMS_POSTERIOR -> RegionAnchor(.33f, .46f, .035f, .075f)
    MuscleRegion.UPPER_TRAPEZIUS, MuscleRegion.MIDDLE_TRAPEZIUS, MuscleRegion.LOWER_TRAPEZIUS -> RegionAnchor(.50f, .25f, .10f, .10f)
    MuscleRegion.LATISSIMUS_DORSI, MuscleRegion.RHOMBOIDS, MuscleRegion.TERES_MAJOR, MuscleRegion.TERES_MINOR -> RegionAnchor(.42f, .34f, .13f, .12f)
    MuscleRegion.SPINAL_ERECTORS -> RegionAnchor(.50f, .43f, .055f, .15f)
    MuscleRegion.GLUTE_MAXIMUS, MuscleRegion.GLUTE_MEDIUS, MuscleRegion.GLUTE_MINIMUS -> RegionAnchor(.50f, .55f, .13f, .09f)
    MuscleRegion.HAMSTRINGS, MuscleRegion.BICEPS_FEMORIS, MuscleRegion.SEMITENDINOSUS, MuscleRegion.SEMIMEMBRANOSUS -> RegionAnchor(.44f, .69f, .075f, .15f)
    MuscleRegion.CALVES, MuscleRegion.GASTROCNEMIUS, MuscleRegion.SOLEUS, MuscleRegion.PERONEALS -> RegionAnchor(.45f, .86f, .055f, .12f)
}
