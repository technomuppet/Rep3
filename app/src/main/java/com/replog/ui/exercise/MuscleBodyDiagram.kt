package com.replog.ui.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.data.model.Exercise
import com.replog.domain.library.MuscleMap
import com.replog.domain.library.MuscleRegion

/**
 * Code-drawn body diagrams (Phase 4). No GIFs, photos or videos and no image
 * assets - a simple vector silhouette is drawn with Compose Canvas and the
 * targeted muscle regions are highlighted. Primary regions use a filled tint;
 * secondary regions use a lighter, outlined fill so the distinction does NOT rely
 * on colour alone (Phase 8). A spoken content description is provided for
 * TalkBack, and a text legend reinforces the colour coding.
 *
 * Footprint: pure code, effectively 0 KB of assets.
 */
@Composable
fun MuscleBodyDiagram(exercise: Exercise, modifier: Modifier = Modifier) {
    val primary = MuscleMap.primaryRegions(exercise)
    val secondary = MuscleMap.secondaryRegions(exercise)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val silhouette = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.onSurfaceVariant

    val desc = buildString {
        append("Body diagram for ${exercise.name}. ")
        append("Primary muscles: ${if (primary.isEmpty()) "none" else primary.joinToString { regionLabel(it) }}. ")
        append("Secondary muscles: ${if (secondary.isEmpty()) "none" else secondary.joinToString { regionLabel(it) }}.")
    }

    Column(modifier = modifier.semantics { contentDescription = desc }) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            BodyView("Front", MuscleRegion.Side.FRONT, primary, secondary, primaryColor, secondaryColor, silhouette, outline, Modifier.weight(1f))
            BodyView("Back", MuscleRegion.Side.BACK, primary, secondary, primaryColor, secondaryColor, silhouette, outline, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(primaryColor, "Primary")
            LegendDot(secondaryColor, "Secondary (outlined)")
        }
    }
}

@Composable
private fun BodyView(
    label: String,
    side: MuscleRegion.Side,
    primary: Set<MuscleRegion>,
    secondary: Set<MuscleRegion>,
    primaryColor: Color,
    secondaryColor: Color,
    silhouette: Color,
    outline: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            drawSilhouette(silhouette, outline)
            // Highlight regions belonging to this side.
            REGION_BOXES.filter { it.key.side == side }.forEach { (region, box) ->
                when {
                    region in primary -> drawRegion(box, primaryColor, filled = true)
                    region in secondary -> drawRegion(box, secondaryColor, filled = false)
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(12.dp)) {}
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Normalised region rectangles (x, y, w, h in 0..1) over the silhouette.
private data class Box(val x: Float, val y: Float, val w: Float, val h: Float)

private val REGION_BOXES: Map<MuscleRegion, Box> = mapOf(
    // Front
    MuscleRegion.FRONT_DELTS to Box(0.30f, 0.20f, 0.40f, 0.06f),
    MuscleRegion.SIDE_DELTS to Box(0.26f, 0.21f, 0.48f, 0.05f),
    MuscleRegion.INNER_THIGHS to Box(0.42f, 0.52f, 0.16f, 0.14f),
    MuscleRegion.OUTER_HIPS to Box(0.32f, 0.48f, 0.36f, 0.06f),
    MuscleRegion.CHEST to Box(0.34f, 0.24f, 0.32f, 0.08f),
    MuscleRegion.BICEPS to Box(0.20f, 0.26f, 0.10f, 0.10f),
    MuscleRegion.FOREARMS to Box(0.16f, 0.36f, 0.10f, 0.10f),
    MuscleRegion.ABS to Box(0.38f, 0.34f, 0.24f, 0.12f),
    MuscleRegion.OBLIQUES to Box(0.32f, 0.36f, 0.36f, 0.08f),
    MuscleRegion.QUADS to Box(0.34f, 0.52f, 0.32f, 0.16f),
    // Back
    MuscleRegion.TRAPS to Box(0.36f, 0.18f, 0.28f, 0.06f),
    MuscleRegion.REAR_DELTS to Box(0.28f, 0.20f, 0.44f, 0.05f),
    MuscleRegion.UPPER_BACK to Box(0.34f, 0.24f, 0.32f, 0.08f),
    MuscleRegion.LATS to Box(0.30f, 0.30f, 0.40f, 0.10f),
    MuscleRegion.TRICEPS to Box(0.20f, 0.26f, 0.10f, 0.10f),
    MuscleRegion.LOWER_BACK to Box(0.38f, 0.40f, 0.24f, 0.07f),
    MuscleRegion.GLUTES to Box(0.34f, 0.48f, 0.32f, 0.08f),
    MuscleRegion.HAMSTRINGS to Box(0.34f, 0.58f, 0.32f, 0.12f),
    MuscleRegion.CALVES to Box(0.34f, 0.78f, 0.32f, 0.12f)
)

private fun DrawScope.drawSilhouette(fill: Color, outline: Color) {
    val w = size.width
    val h = size.height
    // Head
    drawCircle(fill, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.10f))
    // Torso
    drawRoundRectN(fill, 0.32f, 0.18f, 0.36f, 0.30f)
    // Arms
    drawRoundRectN(fill, 0.18f, 0.20f, 0.10f, 0.26f)
    drawRoundRectN(fill, 0.72f, 0.20f, 0.10f, 0.26f)
    // Legs
    drawRoundRectN(fill, 0.34f, 0.48f, 0.14f, 0.44f)
    drawRoundRectN(fill, 0.52f, 0.48f, 0.14f, 0.44f)
}

private fun DrawScope.drawRoundRectN(color: Color, x: Float, y: Float, w: Float, h: Float) {
    drawRect(
        color = color,
        topLeft = Offset(size.width * x, size.height * y),
        size = Size(size.width * w, size.height * h)
    )
}

private fun DrawScope.drawRegion(box: Box, color: Color, filled: Boolean) {
    val topLeft = Offset(size.width * box.x, size.height * box.y)
    val s = Size(size.width * box.w, size.height * box.h)
    if (filled) {
        drawRect(color = color.copy(alpha = 0.85f), topLeft = topLeft, size = s)
    } else {
        // Outlined (pattern-style) highlight so it is distinguishable without colour.
        drawRect(color = color.copy(alpha = 0.18f), topLeft = topLeft, size = s)
        drawRect(
            color = color,
            topLeft = topLeft,
            size = s,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.012f)
        )
    }
}

private fun regionLabel(r: MuscleRegion): String = when (r) {
    MuscleRegion.CHEST -> "chest"
    MuscleRegion.FRONT_DELTS -> "front shoulders"
    MuscleRegion.SIDE_DELTS -> "side shoulders"
    MuscleRegion.INNER_THIGHS -> "inner thighs"
    MuscleRegion.OUTER_HIPS -> "outer hips"
    MuscleRegion.BICEPS -> "biceps"
    MuscleRegion.FOREARMS -> "forearms"
    MuscleRegion.ABS -> "abs"
    MuscleRegion.OBLIQUES -> "obliques"
    MuscleRegion.QUADS -> "quads"
    MuscleRegion.UPPER_BACK -> "upper back"
    MuscleRegion.LATS -> "lats"
    MuscleRegion.REAR_DELTS -> "rear shoulders"
    MuscleRegion.TRICEPS -> "triceps"
    MuscleRegion.TRAPS -> "traps"
    MuscleRegion.LOWER_BACK -> "lower back"
    MuscleRegion.GLUTES -> "glutes"
    MuscleRegion.HAMSTRINGS -> "hamstrings"
    MuscleRegion.CALVES -> "calves"
}
