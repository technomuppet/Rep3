package com.replog.domain.forecast

import com.replog.data.model.TrainingDnaProgressionScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2 Gap 6 — deterministic JUnit 4 tests for the pure ProgressionForecaster.
 *
 * Drives the engine that BOTH
 *
 *   1. the Home `ProgressionForecastCard` composable, and
 *   2. the `IntelligenceRepository.buildProgressionForecasts()` helper
 *
 * consume. Locks the trend ladder (RISING / FLAT / DECLINING), the
 * confidence ladder (HIGH / MEDIUM / LOW), and the projection label
 * formatting so a tuning of the engine cannot regress either UI consumer
 * without a test failure.
 *
 * Host-runnable via
 * `./gradlew :app:testDebugUnitTest --tests com.replog.domain.forecast.ProgressionForecasterTest`.
 */
class ProgressionForecasterTest {

    @Test(timeout = 1_000L)
    fun rising_withBoth30And90Day_returnsHighConfidence() {
        val score = TrainingDnaProgressionScore(
            exerciseId = 42, calculatedAt = System.currentTimeMillis(),
            score30Day = 1.0, score90Day = 0.9, scoreLifetime = 0.8,
            estimatedOneRm30Day = 105.0, estimatedOneRm90Day = 100.0,
            estimatedOneRmLifetime = 90.0
        )
        val f = ProgressionForecaster.forecast(score)
        assertEquals(ForecastTrend.RISING, f.trend)
        assertEquals(ForecastConfidence.HIGH, f.confidence)
        assertTrue("label carries kg + weeks markers", f.projectionLabel.contains("kg") && f.projectionLabel.contains("4 weeks"))
        assertTrue("weekly gain > 0", f.weeklyGainKg > 0.0)
    }

    @Test(timeout = 1_000L)
    fun only30DayPresent_returnsMediumConfidence() {
        val score = TrainingDnaProgressionScore(
            exerciseId = 1, calculatedAt = System.currentTimeMillis(),
            score30Day = 1.0, score90Day = 0.0, scoreLifetime = 0.0,
            estimatedOneRm30Day = 100.0, estimatedOneRm90Day = 0.0,
            estimatedOneRmLifetime = 0.0
        )
        val f = ProgressionForecaster.forecast(score)
        assertEquals(ForecastConfidence.MEDIUM, f.confidence)
    }

    @Test(timeout = 1_000L)
    fun onlyLifetimePresent_returnsLowConfidence() {
        val score = TrainingDnaProgressionScore(
            exerciseId = 2, calculatedAt = System.currentTimeMillis(),
            score30Day = 0.0, score90Day = 0.0, scoreLifetime = 1.0,
            estimatedOneRm30Day = 0.0, estimatedOneRm90Day = 0.0,
            estimatedOneRmLifetime = 90.0
        )
        val f = ProgressionForecaster.forecast(score)
        assertEquals(ForecastConfidence.LOW, f.confidence)
    }

    @Test(timeout = 1_000L)
    fun declining30Day_vs90Day_returnsDecliningWithRecoveryExplanation() {
        val score = TrainingDnaProgressionScore(
            exerciseId = 3, calculatedAt = System.currentTimeMillis(),
            score30Day = 0.5, score90Day = 1.0, scoreLifetime = 1.0,
            estimatedOneRm30Day = 90.0, estimatedOneRm90Day = 100.0,
            estimatedOneRmLifetime = 110.0
        )
        val f = ProgressionForecaster.forecast(score)
        assertEquals(ForecastTrend.DECLINING, f.trend)
        assertTrue(
            "explanation must reference the deload / fatigue tail",
            f.explanation.lowercase().contains("dipped") ||
                f.explanation.lowercase().contains("fatigue") ||
                f.explanation.lowercase().contains("deload")
        )
    }

    @Test(timeout = 1_000L)
    fun flatTrend_returnsFlatWhenWeeklyGainBelowThreshold() {
        // 30d ~ 90d (90 → 90.5 -> 0.6kg/week gain) => trend is FLAT (gains < 0.05).
        val score = TrainingDnaProgressionScore(
            exerciseId = 4, calculatedAt = System.currentTimeMillis(),
            score30Day = 0.95, score90Day = 0.95, scoreLifetime = 0.9,
            estimatedOneRm30Day = 90.5, estimatedOneRm90Day = 90.0,
            estimatedOneRmLifetime = 85.0
        )
        val f = ProgressionForecaster.forecast(score)
        assertEquals(ForecastTrend.FLAT, f.trend)
        assertTrue(
            "flat lifts mention 'steady' in coach narrative",
            f.explanation.lowercase().contains("steady") ||
                f.explanation.lowercase().contains("overload")
        )
    }

    @Test(timeout = 1_000L)
    fun projectionLabel_reflectsTargetRepsAndHorizon() {
        val score = TrainingDnaProgressionScore(
            exerciseId = 5, calculatedAt = System.currentTimeMillis(),
            score30Day = 1.0, score90Day = 0.95, scoreLifetime = 0.9,
            estimatedOneRm30Day = 100.0, estimatedOneRm90Day = 95.0,
            estimatedOneRmLifetime = 85.0
        )
        val f5x4 = ProgressionForecaster.forecast(score, targetReps = 5, horizonWeeks = 4)
        assertTrue(
            "5-rep-4-weeks forecast mentions both numbers",
            f5x4.projectionLabel.contains("5") && f5x4.projectionLabel.contains("4 weeks")
        )
        val f8x6 = ProgressionForecaster.forecast(score, targetReps = 8, horizonWeeks = 6)
        assertTrue(
            "8-rep-6-weeks forecast mentions both numbers",
            f8x6.projectionLabel.contains("8") && f8x6.projectionLabel.contains("6 weeks")
        )
    }
}
