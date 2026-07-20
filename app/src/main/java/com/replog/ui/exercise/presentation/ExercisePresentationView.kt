package com.replog.ui.exercise.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.data.model.Exercise
import com.replog.domain.visual.anatomy.*
import com.replog.domain.visual.presentation.*
import com.replog.ui.components.RepLogCard

@Composable
fun ExercisePresentationView(exercise: Exercise, modifier: Modifier = Modifier) {
    val asset = remember(exercise.id) { ExercisePresentationFactory.createAsset(exercise) }
    var selectedPoseIndex by remember(exercise.id) { mutableIntStateOf(0) }
    val selectedPose = asset.poses[selectedPoseIndex]

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        // 1. Pose Stage Selection Tabs (Setup -> Start -> Midpoint -> Peak)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            asset.poses.forEachIndexed { index, pose ->
                val isSelected = index == selectedPoseIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { selectedPoseIndex = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        pose.stageName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Pose Diagram with Coaching Overlays (Static — NO ANIMATION per RC44)
        val currentPose = selectedPose
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left: Volumetric Human Posture Pose
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawHumanPose(this, currentPose, size.width, size.height)
                        drawCues(this, currentPose, size.width, size.height)
                    }

                }
                
                // Right: Stage Description Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        currentPose.stageName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        currentPose.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Textbook Anatomy Muscle Diagram
        RepLogCard {
            Text("Target Muscle Activation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        val regions = V2AnatomyModel.loadRegionsForSide(isFront = true)
                        MuscleRenderer.drawRegions(
                            drawScope = this,
                            regions = regions,
                            silhouettePath = V2AnatomyModel.frontSilhouette,
                            primaryRegions = if (selectedPoseIndex >= 2) asset.anatomySpec.primaryMuscles.map { MuscleRegion.valueOf(it.uppercase()) }.toSet() else emptySet(),
                            secondaryRegions = if (selectedPoseIndex >= 1) asset.anatomySpec.secondaryMuscles.map { MuscleRegion.valueOf(it.uppercase()) }.toSet() else emptySet(),
                            palette = AnatomyPalette.default(),
                            activations = emptyList(),
                            stabiliserRegions = if (selectedPoseIndex >= 1) asset.anatomySpec.stabiliserMuscles.mapNotNull {
                                try { MuscleRegion.valueOf(it.uppercase()) } catch (e: Exception) { null }
                            }.toSet() else emptySet()
                        )
                    }
                    Text("Anterior (Front)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        val regions = V2AnatomyModel.loadRegionsForSide(isFront = false)
                        MuscleRenderer.drawRegions(
                            drawScope = this,
                            regions = regions,
                            silhouettePath = V2AnatomyModel.rearSilhouette,
                            primaryRegions = if (selectedPoseIndex >= 2) asset.anatomySpec.primaryMuscles.map { MuscleRegion.valueOf(it.uppercase()) }.toSet() else emptySet(),
                            secondaryRegions = if (selectedPoseIndex >= 1) asset.anatomySpec.secondaryMuscles.map { MuscleRegion.valueOf(it.uppercase()) }.toSet() else emptySet(),
                            palette = AnatomyPalette.default(),
                            activations = emptyList(),
                            stabiliserRegions = if (selectedPoseIndex >= 1) asset.anatomySpec.stabiliserMuscles.mapNotNull {
                                try { MuscleRegion.valueOf(it.uppercase()) } catch (e: Exception) { null }
                            }.toSet() else emptySet()
                        )
                    }
                    Text("Posterior (Back)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 4. Detailed Coaching Cards
        RepLogCard {
            Text("Setup & Preparation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            asset.setupChecklist.forEachIndexed { i, step ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(step, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        RepLogCard {
            Text("Execution Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            asset.executionSteps.forEachIndexed { i, step ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("${i + 1}. ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(step, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        RepLogCard {
            Text("Common Mistakes to Avoid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            asset.commonMistakes.forEach { mistake ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("✗ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(mistake, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
    }
}

private fun drawHumanPose(drawScope: DrawScope, pose: PoseIllustration, w: Float, h: Float) {
    with(drawScope) {
        fun screenOffset(name: String): Offset {
            val norm = pose.skeletonOffsets[name] ?: Offset(0.5f, 0.5f)
            return Offset(w * norm.x, h * norm.y)
        }

        val primaryColor = Color(0xFFD8BFA0)
        val outlineColor = Color(0xFF1A1A1A)

        // Draw basic simplified joints in 15-point rig
        val pelvis = screenOffset("PELVIS")
        val midSpine = screenOffset("MID_SPINE")
        val head = screenOffset("HEAD")
        val shoulder = screenOffset("LEFT_SHOULDER")
        val elbow = screenOffset("LEFT_ELBOW")
        val wrist = screenOffset("LEFT_WRIST")
        val hip = screenOffset("LEFT_HIP")
        val knee = screenOffset("LEFT_KNEE")
        val ankle = screenOffset("LEFT_ANKLE")

        // Draw central trunk
        drawLine(outlineColor, pelvis, midSpine, strokeWidth = 14f, cap = StrokeCap.Round)
        drawLine(outlineColor, midSpine, head, strokeWidth = 14f, cap = StrokeCap.Round)
        drawCircle(primaryColor, radius = 24f, center = head)

        // Draw arms
        drawLine(outlineColor, shoulder, elbow, strokeWidth = 8f, cap = StrokeCap.Round)
        drawLine(outlineColor, elbow, wrist, strokeWidth = 8f, cap = StrokeCap.Round)
        drawCircle(primaryColor, radius = 10f, center = wrist)

        // Draw legs
        drawLine(outlineColor, hip, knee, strokeWidth = 12f, cap = StrokeCap.Round)
        drawLine(outlineColor, knee, ankle, strokeWidth = 10f, cap = StrokeCap.Round)
        drawCircle(primaryColor, radius = 12f, center = ankle)
    }
}

private fun drawCues(drawScope: DrawScope, pose: PoseIllustration, w: Float, h: Float) {
    with(drawScope) {
        fun screenOffset(name: String): Offset {
            val norm = pose.skeletonOffsets[name] ?: Offset(0.5f, 0.5f)
            return Offset(w * norm.x, h * norm.y)
        }

            pose.overlayCues.forEach { cue ->
                val p1 = screenOffset(cue.startJoint)
                val p2 = screenOffset(cue.endJoint)
                val cueColor = Color(android.graphics.Color.parseColor(cue.colorHex))

                when (cue.type) {
                    CueType.LINE -> {
                        drawLine(cueColor, p1, p2, strokeWidth = 5f, cap = StrokeCap.Round)
                    }
                    CueType.MARKER -> {
                        drawCircle(cueColor, radius = 12f, center = p1)
                        drawCircle(Color.White, radius = 6f, center = p1)
                    }
                    CueType.FORCE_VECTOR -> {
                        drawLine(cueColor, p1, p2, strokeWidth = 4f, cap = StrokeCap.Round)
                        drawCircle(cueColor, radius = 8f, center = p2)
                    }
                    CueType.ANGLE -> {
                        drawLine(cueColor.copy(alpha = 0.35f), p1, p2, strokeWidth = 12f, cap = StrokeCap.Round)
                    }
                    CueType.ARC -> {
                        val path = Path().apply {
                            moveTo(p1.x, p1.y)
                            quadraticTo((p1.x + p2.x) * 0.5f - 20f, (p1.y + p2.y) * 0.5f - 20f, p2.x, p2.y)
                        }
                        drawPath(path, cueColor, style = Stroke(width = 4f))
                    }
                    else -> {}
                }
            }
    }
}
