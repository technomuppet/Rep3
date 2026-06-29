package com.replog.ui.exercise

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

/**
 * Renders the lightweight keyframe animation (Phase 5) as a moving stick figure
 * on a Compose Canvas. Frames are generated locally by [ExerciseAnimation]; there
 * is no video, GIF or photo and no downloaded media. Linear interpolation between
 * the (<= 8) poses produces smooth motion. A content description names the
 * movement for screen readers (Phase 8).
 */
@Composable
fun ExerciseAnimationView(exercise: Exercise, modifier: Modifier = Modifier) {
    val clip = remember(exercise.id, exercise.movementPattern, exercise.equipment) {
        ExerciseAnimation.clip(exercise)
    }
    val transition = rememberInfiniteTransition(label = "exerciseAnim")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = clip.cycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val figure = MaterialTheme.colorScheme.primary
    val implementColor = MaterialTheme.colorScheme.tertiary
    val joint = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = "Animated demonstration of ${exercise.name}" }
    ) {
        val pose = interpolate(clip, t)
        drawFigure(pose, figure, implementColor, joint)
    }
}

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun lerp(a: Point, b: Point, f: Float) = Point(lerp(a.x, b.x, f), lerp(a.y, b.y, f))
private fun lerp(a: Point?, b: Point?, f: Float): Point? =
    if (a == null || b == null) null else lerp(a, b, f)

private fun interpolate(clip: AnimationClip, t: Float): Pose {
    val frames = clip.keyframes
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
        implement = lerp(a.implement, b.implement, local)
    )
}

private fun DrawScope.p(pt: Point) = Offset(size.width * pt.x, size.height * pt.y)

private fun DrawScope.drawFigure(pose: Pose, figure: Color, implementColor: Color, joint: Color) {
    val stroke = size.width * 0.018f
    fun line(a: Point, b: Point) = drawLine(figure, p(a), p(b), strokeWidth = stroke, cap = StrokeCap.Round)

    // Head
    drawCircle(figure, radius = size.width * 0.045f, center = p(pose.head))
    // Spine
    line(pose.shoulder, pose.hip)
    // Arm (shoulder -> elbow -> hand)
    line(pose.shoulder, pose.elbow)
    line(pose.elbow, pose.hand)
    // Leg (hip -> knee -> foot)
    line(pose.hip, pose.knee)
    line(pose.knee, pose.foot)
    // Joints
    listOf(pose.shoulder, pose.elbow, pose.hand, pose.hip, pose.knee, pose.foot).forEach {
        drawCircle(joint, radius = size.width * 0.012f, center = p(it))
    }
    // Implement (bar / dumbbell) if present
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
