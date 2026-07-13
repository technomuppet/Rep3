package com.replog.domain.visual.biomechanics

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sin

/**
 * Equipment trajectory engine implementing correct bar paths.
 *
 * Examples:
 * - Bench Press: Nearly vertical path, slight S-curve from chest (lower) to over shoulders (upper)
 * - Squat: Slight S-curve, bar over mid-foot, hips back knees forward
 * - Deadlift: Bar remains close to shins, vertical path over mid-foot
 * - Curl: Arc around elbow
 * - Lateral Raise: Large shoulder arc
 * - Cable: Constrained by pulley
 *
 * Offline, no external libs.
 */
object BarPathEngine {

    enum class BarPathType {
        VERTICAL, // bench, overhead press
        S_CURVE, // squat, front squat
        CLOSE_VERTICAL, // deadlift
        ARC_ELBOW, // curl, skull crusher
        ARC_SHOULDER, // lateral raise, front raise, rear delt
        CABLE_CONSTRAINED, // cable row, lat pulldown, face pull
        HORIZONTAL, // leg press sled
        FIXED // plank, isometric
    }

    data class BarPath(
        val type: BarPathType,
        val start: Offset,
        val end: Offset,
        val controlPoints: List<Offset> = emptyList() // for bezier
    )

    /**
     * Calculates bar path for given exercise family and time progress 0..1.
     * Returns desired bar center at that progress.
     */
    fun calculateBarPosition(
        pathType: BarPathType,
        start: Offset,
        end: Offset,
        progress: Float, // 0..1 in current segment (e.g., concentric)
        pulley: Offset? = null // for cable
    ): Offset {
        val t = progress.coerceIn(0f, 1f)
        return when (pathType) {
            BarPathType.VERTICAL -> {
                // Nearly vertical with slight horizontal S: lerp Y, small X sine
                val x = start.x + (end.x - start.x) * t + sin(t * Math.PI.toFloat()) * 0.02f * (if (end.y < start.y) -1f else 1f)
                val y = start.y + (end.y - start.y) * t
                Offset(x, y)
            }
            BarPathType.S_CURVE -> {
                // Slight S-curve for squat: hips back moves bar slightly back at bottom
                val xOffset = sin(t * Math.PI.toFloat()) * 0.04f * (if (t < 0.5f) -1f else 1f)
                val x = start.x + (end.x - start.x) * t + xOffset
                val y = start.y + (end.y - start.y) * t
                Offset(x, y)
            }
            BarPathType.CLOSE_VERTICAL -> {
                // Bar remains close to shins: X almost constant over mid-foot, Y vertical
                val x = start.x * (1f - t) + end.x * t // should be same X
                val y = start.y + (end.y - start.y) * t
                // Add slight knee clearance at mid: small forward bias at mid
                val clearance = sin(t * Math.PI.toFloat()) * 0.015f
                Offset(x + clearance, y)
            }
            BarPathType.ARC_ELBOW -> {
                // Arc around elbow: circular path
                // For curl, elbow is pivot, hand arcs
                // We approximate arc via quadratic bezier with elbow as control
                // If start and end are hand positions, arc mid is outwards
                val midX = (start.x + end.x) * 0.5f
                val midY = (start.y + end.y) * 0.5f - 0.08f // arc up
                // Quadratic bezier
                val omt = 1f - t
                val x = omt * omt * start.x + 2f * omt * t * midX + t * t * end.x
                val y = omt * omt * start.y + 2f * omt * t * midY + t * t * end.y
                Offset(x, y)
            }
            BarPathType.ARC_SHOULDER -> {
                // Large shoulder arc for lateral raise: hand moves in large circle around shoulder
                // Implement circular interpolation around shoulder pivot (approx mid between start and end + offset)
                // Simplified: large arc outward
                val angleSpan = Math.toRadians(80.0).toFloat() // 80 deg arc
                val angle = -Math.PI.toFloat() * 0.5f + t * angleSpan // from -90 to -10 deg
                val radius = 0.18f // normalized
                val pivot = Offset((start.x + end.x) * 0.5f, start.y - 0.05f) // shoulder approx
                val x = pivot.x + kotlin.math.cos(angle) * radius
                val y = pivot.y + kotlin.math.sin(angle) * radius
                Offset(x, y)
            }
            BarPathType.CABLE_CONSTRAINED -> {
                // Constrained by pulley: bar/hand moves along line from pulley to start-end line
                // For simplicity, straight line from start to end, but ensure distance to pulley decreases/increases naturally
                // Pulley is fixed, hand moves towards pulley
                if (pulley == null) {
                    Offset(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t)
                } else {
                    // Straight line to pulley: hand moves along line pulley<->end
                    // Actually cable exercises: hand moves between start (stretched) far from pulley and end (contracted) near pulley
                    // So interpolate along line pulley->start to pulley->end? Simplified lerp
                    Offset(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t)
                }
            }
            BarPathType.HORIZONTAL -> {
                // Leg press sled horizontal
                val x = start.x + (end.x - start.x) * t
                Offset(x, start.y)
            }
            BarPathType.FIXED -> start
        }
    }

    /**
     * Validates bar path correctness.
     */
    fun validateBarPath(
        pathType: BarPathType,
        positions: List<Offset> // sampled positions over full cycle
    ): BarPathValidation {
        if (positions.size < 2) return BarPathValidation(false, "Too few samples")

        // Check verticality for bench: X variance should be <0.05 for nearly vertical
        val xVals = positions.map { it.x }
        val xMin = xVals.minOrNull() ?: 0f
        val xMax = xVals.maxOrNull() ?: 0f
        val xVariance = abs(xMax - xMin)

        val yVals = positions.map { it.y }
        val yMin = yVals.minOrNull() ?: 0f
        val yMax = yVals.maxOrNull() ?: 0f
        val yRange = abs(yMax - yMin)

        return when (pathType) {
            BarPathType.VERTICAL -> {
                val ok = xVariance < 0.06f && yRange > 0.1f
                BarPathValidation(ok, "Vertical: xVar=$xVariance yRange=$yRange ok=$ok")
            }
            BarPathType.S_CURVE -> {
                val ok = xVariance in 0.02f..0.08f && yRange > 0.12f
                BarPathValidation(ok, "S-curve xVar=$xVariance yRange=$yRange ok=$ok")
            }
            BarPathType.CLOSE_VERTICAL -> {
                val ok = xVariance < 0.04f && yRange > 0.15f
                BarPathValidation(ok, "Close vertical xVar=$xVariance ok=$ok")
            }
            BarPathType.ARC_ELBOW -> {
                // Arc should have significant Y variation and moderate X
                val ok = yRange > 0.08f && xVariance < 0.15f
                BarPathValidation(ok, "Arc elbow xVar=$xVariance yRange=$yRange")
            }
            BarPathType.ARC_SHOULDER -> {
                val ok = yRange > 0.1f && xVariance > 0.08f // large arc
                BarPathValidation(ok, "Arc shoulder xVar=$xVariance yRange=$yRange")
            }
            else -> BarPathValidation(true, "No strict validation for $pathType")
        }
    }

    data class BarPathValidation(val isValid: Boolean, val message: String)
}
