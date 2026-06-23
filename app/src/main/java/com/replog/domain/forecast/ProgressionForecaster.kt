package com.replog.domain.forecast

import com.replog.data.model.TrainingDnaProgressionScore
import kotlin.math.roundToInt

/**
 * Priority 3 (#11) — Exercise Progression Forecast.
 *
 * Pure projection built ENTIRELY from the existing TrainingDnaProgressionScore
 * rows (estimated 1RM at 30/90 day windows + lifetime). No new analytics engine;
 * this just extrapolates the recent trend into a friendly "where you're headed".
 */
data class ProgressionForecast(
    val exerciseId: Int,
    val currentE1rm: Double,
    val projectedE1rm: Double,
    val horizonWeeks: Int,
    val weeklyGainKg: Double,
    /** Plain-English headline, e.g. "85 kg × 5 in 4 weeks". */
    val projectionLabel: String,
    val trend: ForecastTrend,
    val confidence: ForecastConfidence,
    val explanation: String
)

enum class ForecastTrend { RISING, FLAT, DECLINING }
enum class ForecastConfidence { LOW, MEDIUM, HIGH }

object ProgressionForecaster {

    /**
     * @param score the latest progression score for an exercise
     * @param targetReps reps to express the projected working weight at (default 5)
     * @param horizonWeeks how far ahead to project (default 4)
     * @param roundingKg barbell rounding increment (default 2.5)
     */
    fun forecast(
        score: TrainingDnaProgressionScore,
        targetReps: Int = 5,
        horizonWeeks: Int = 4,
        roundingKg: Double = 2.5
    ): ProgressionForecast {
        val current = score.estimatedOneRm30Day.takeIf { it > 0 }
            ?: score.estimatedOneRm90Day.takeIf { it > 0 }
            ?: score.estimatedOneRmLifetime

        // Recent weekly rate from the 30-day window (≈4.3 weeks); fall back to 90-day.
        val weeklyGain = when {
            score.estimatedOneRm90Day > 0 && score.estimatedOneRm30Day > 0 ->
                (score.estimatedOneRm30Day - score.estimatedOneRm90Day) / 8.0 // 90→30d span ≈ 8 weeks
            else -> 0.0
        }

        val rawProjected = (current + weeklyGain * horizonWeeks).coerceAtLeast(current * 0.9)
        val projected = roundTo(rawProjected, roundingKg)
        val workingWeight = roundTo(projected / epleyFactor(targetReps), roundingKg)

        val trend = when {
            weeklyGain > 0.05 -> ForecastTrend.RISING
            weeklyGain < -0.05 -> ForecastTrend.DECLINING
            else -> ForecastTrend.FLAT
        }

        val confidence = when {
            score.estimatedOneRm90Day > 0 && score.estimatedOneRm30Day > 0 -> ForecastConfidence.MEDIUM
            current > 0 -> ForecastConfidence.LOW
            else -> ForecastConfidence.LOW
        }.let { base ->
            // Strong, steady rise with both windows present → high.
            if (base == ForecastConfidence.MEDIUM && trend == ForecastTrend.RISING) ForecastConfidence.HIGH else base
        }

        val label = "${fmt(workingWeight)} kg × $targetReps in $horizonWeeks weeks"
        val explanation = when (trend) {
            ForecastTrend.RISING -> "You're trending up about ${fmt(weeklyGain)} kg/week on estimated 1RM. Keep progressing."
            ForecastTrend.FLAT -> "Your estimated 1RM has been steady. A small overload nudge could restart progress."
            ForecastTrend.DECLINING -> "Estimated 1RM has dipped recently — likely fatigue or a deload. Projection assumes a gentle recovery."
        }

        return ProgressionForecast(
            exerciseId = score.exerciseId,
            currentE1rm = roundTo(current, 0.5),
            projectedE1rm = projected,
            horizonWeeks = horizonWeeks,
            weeklyGainKg = weeklyGain,
            projectionLabel = label,
            trend = trend,
            confidence = confidence,
            explanation = explanation
        )
    }

    /** Epley factor: 1RM ≈ weight × (1 + reps/30) ⇒ weight ≈ 1RM / factor. */
    private fun epleyFactor(reps: Int): Double = 1.0 + reps.coerceAtLeast(1) / 30.0

    private fun roundTo(value: Double, increment: Double): Double {
        if (increment <= 0) return value
        return (value / increment).roundToInt() * increment
    }

    private fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
}
