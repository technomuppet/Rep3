package com.replog.ui.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.replog.data.model.Exercise
import com.replog.domain.library.AnimationClip
import com.replog.domain.library.ExerciseAnimation
import com.replog.domain.library.Point
import com.replog.domain.library.Pose
import com.replog.domain.visual.animation.ForwardKinematicsSolver
import com.replog.domain.visual.animation.SkeletalRenderer
import com.replog.ui.exercise.adapter.VisualEngineAdapter

/**
 * Exercise animation demonstration presentation facade (Phase 4 Integration).
 * Automatically prefers the rotational Forward Kinematics Skeletal Animation Engine
 * while retaining legacy stick figure rendering as a safe fallback.
 */
@Composable
fun ExerciseAnimationView(exercise: Exercise, modifier: Modifier = Modifier) {
    when (val mode = remember(exercise.id, exercise.movementPattern, exercise.equipment) {
        VisualEngineAdapter.resolveAnimation(exercise)
    }) {
        is VisualEngineAdapter.AnimationRenderMode.SkeletalEngine -> {
            var playing by remember(exercise.id) { mutableStateOf(true) }
            var elapsedSeconds by remember(exercise.id) { mutableFloatStateOf(0f) }

            LaunchedEffect(exercise.id, playing, mode.timeline.durationSeconds) {
                if (!playing) return@LaunchedEffect
                var lastTime = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    val deltaSeconds = (now - lastTime) / 1_000_000_000f
                    lastTime = now
                    elapsedSeconds += deltaSeconds
                }
            }

            val boneColor = MaterialTheme.colorScheme.primary
            val implementColor = MaterialTheme.colorScheme.tertiary
            val jointColor = MaterialTheme.colorScheme.onSurface

            Column(modifier = modifier) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .semantics { contentDescription = "Animated demonstration of ${exercise.name}" }
                ) {
                    val pose = mode.timeline.evaluate(elapsedSeconds, mode.spec.playbackSpeed)
                    val solved = ForwardKinematicsSolver.solve(pose.jointRotations, pose.rootPositionOffset)
                    SkeletalRenderer.drawSkeleton(
                        drawScope = this,
                        skeleton = solved,
                        boneColor = boneColor,
                        jointColor = jointColor,
                        implementColor = implementColor,
                        equipmentType = mode.spec.equipment.type
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { playing = !playing }) {
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause animation" else "Play animation"
                        )
                    }
                    IconButton(onClick = { elapsedSeconds = 0f; playing = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart animation")
                    }
                    Text(
                        if (playing) "Playing" else "Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is VisualEngineAdapter.AnimationRenderMode.LegacyStickFigure -> {
            LegacyExerciseAnimationView(exercise = exercise, clip = mode.clip, modifier = modifier)
        }
    }
}

/**
 * Legacy single-line stick figure animation retained strictly as fallback.
 */
@Composable
private fun LegacyExerciseAnimationView(exercise: Exercise, clip: AnimationClip, modifier: Modifier = Modifier) {
    var playing by remember(exercise.id) { mutableStateOf(true) }
    var phase by remember(exercise.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(exercise.id, playing, clip.cycleMillis) {
        if (!playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val deltaMs = (now - last) / 1_000_000f
            last = now
            val cycle = clip.cycleMillis.coerceAtLeast(1)
            phase = (phase + deltaMs / cycle) % 1f
        }
    }

    val figure = MaterialTheme.colorScheme.primary
    val implementColor = MaterialTheme.colorScheme.tertiary
    val joint = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .semantics { contentDescription = "Animated demonstration of ${exercise.name}" }
        ) {
            val pose = interpolate(clip, phase)
            drawFigure(pose, figure, implementColor, joint)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { playing = !playing }) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause animation" else "Play animation"
                )
            }
            IconButton(onClick = { phase = 0f; playing = true }) {
                Icon(Icons.Default.Refresh, contentDescription = "Restart animation")
            }
            Text(
                if (playing) "Playing" else "Paused",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val NEUTRAL_POSE = Pose(
    head = Point(0.5f, 0.10f),
    shoulder = Point(0.5f, 0.22f),
    elbow = Point(0.42f, 0.34f),
    hand = Point(0.42f, 0.46f),
    hip = Point(0.5f, 0.50f),
    knee = Point(0.5f, 0.72f),
    foot = Point(0.5f, 0.95f),
    implement = null
)

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun lerp(a: Point, b: Point, f: Float) = Point(lerp(a.x, b.x, f), lerp(a.y, b.y, f))
private fun lerpNullable(a: Point?, b: Point?, f: Float): Point? =
    if (a == null || b == null) null else lerp(a, b, f)

private fun interpolate(clip: AnimationClip, t: Float): Pose {
    val frames = clip.keyframes
    if (frames.isEmpty()) return NEUTRAL_POSE
    if (frames.size == 1) return frames[0]
    val segments = frames.size - 1
    val scaled = (t.coerceIn(0f, 1f)) * segments
    val i = scaled.toInt().coerceIn(0, segments - 1)
    val local = scaled - i
    val a = frames[i]
    val b = frames[i + 1]
    return Pose(
        head = lerp(a.head, b.head, local),
        shoulder = lerp(a.shoulder, b.shoulder, local),
        elbow = lerp(a.elbow, b.elbow, local),
        hand = lerp(a.hand, b.hand, local),
        hip = lerp(a.hip, b.hip, local),
        knee = lerp(a.knee, b.knee, local),
        foot = lerp(a.foot, b.foot, local),
        implement = lerpNullable(a.implement, b.implement, local)
    )
}

private fun DrawScope.p(pt: Point) = Offset(size.width * pt.x, size.height * pt.y)

private fun DrawScope.drawFigure(pose: Pose, figure: Color, implementColor: Color, joint: Color) {
    val stroke = size.width * 0.018f
    fun line(a: Point, b: Point) = drawLine(figure, p(a), p(b), strokeWidth = stroke, cap = StrokeCap.Round)

    drawCircle(figure, radius = size.width * 0.045f, center = p(pose.head))
    line(pose.shoulder, pose.hip)
    line(pose.shoulder, pose.elbow)
    line(pose.elbow, pose.hand)
    line(pose.hip, pose.knee)
    line(pose.knee, pose.foot)
    listOf(pose.shoulder, pose.elbow, pose.hand, pose.hip, pose.knee, pose.foot).forEach {
        drawCircle(joint, radius = size.width * 0.012f, center = p(it))
    }
    pose.implement?.let {
        val c = p(it)
        drawLine(
            implementColor,
            Offset(c.x - size.width * 0.12f, c.y),
            Offset(c.x + size.width * 0.12f, c.y),
            strokeWidth = stroke * 1.4f,
            cap = StrokeCap.Round
        )
    }
}
