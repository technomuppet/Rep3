package com.replog.domain.analytics

import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog

data class AnalyticsContext(
    val sessions: List<SessionWithExercises>,
    val nowMillis: Long = System.currentTimeMillis()
)

data class VolumeSummary(
    val totalVolume: Double,
    val totalSets: Int,
    val totalReps: Int,
    val hardSetCount: Int
)

data class ProgressionPoint(
    val timestamp: Long,
    val value: Double
)

data class TrendSummary(
    val direction: TrendDirection,
    val absoluteChange: Double,
    val percentChange: Double
)

enum class TrendDirection {
    UP,
    DOWN,
    FLAT,
    INSUFFICIENT_DATA
}

fun SetLog.estimatedOneRepMax(): Double = if (reps <= 1) weight else weight * (1.0 + reps / 30.0)
