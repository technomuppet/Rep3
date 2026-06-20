package com.replog.domain.analytics

import com.replog.testing.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAnalyticsEngineTest {
    private val engine = DefaultAnalyticsEngine()

    @Test
    fun volumeSummaryCalculatesTotals() {
        val session = TestFixtures.session(
            sets = listOf(
                TestFixtures.set(weight = 100.0, reps = 5),
                TestFixtures.set(weight = 80.0, reps = 10)
            )
        )

        val summary = engine.volumeSummary(listOf(session))

        assertEquals(1300.0, summary.totalVolume, 0.001)
        assertEquals(2, summary.totalSets)
        assertEquals(15, summary.totalReps)
    }

    @Test
    fun trendDetectsUpwardProgress() {
        val trend = engine.trend(
            listOf(
                ProgressionPoint(1L, 100.0),
                ProgressionPoint(2L, 110.0)
            )
        )

        assertEquals(TrendDirection.UP, trend.direction)
        assertTrue(trend.percentChange > 0.09)
    }
}
