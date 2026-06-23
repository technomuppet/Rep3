package com.replog.domain.home

import java.util.Calendar

/**
 * Priority 2 — Home dashboard stats (issue #3: make Home a dashboard, not a
 * duplicate of the Workout screen). Pure, deterministic helpers computed from
 * completed-session start times. No schema or analytics-engine changes.
 */
data class WeeklyProgress(
    val sessionsThisWeek: Int,
    val volumeThisWeek: Double
)

object HomeDashboardStats {

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    /** Sessions + total volume since the start of the current calendar week. */
    fun weeklyProgress(
        completedStartTimesToVolume: List<Pair<Long, Double>>,
        nowMillis: Long
    ): WeeklyProgress {
        val weekStart = startOfWeek(nowMillis)
        val inWeek = completedStartTimesToVolume.filter { it.first >= weekStart }
        return WeeklyProgress(
            sessionsThisWeek = inWeek.size,
            volumeThisWeek = inWeek.sumOf { it.second }
        )
    }

    /**
     * Day streak: number of consecutive days up to today (or yesterday) that
     * contain at least one completed workout. Training yesterday but not today
     * keeps the streak alive; a gap of a full day breaks it.
     */
    fun dayStreak(completedStartTimes: List<Long>, nowMillis: Long): Int {
        if (completedStartTimes.isEmpty()) return 0
        val trainedDays = completedStartTimes.map { dayIndex(it) }.toSet()
        val today = dayIndex(nowMillis)
        // Streak may end today or yesterday (today not yet trained is still fine).
        var cursor = when {
            today in trainedDays -> today
            (today - 1) in trainedDays -> today - 1
            else -> return 0
        }
        var streak = 0
        while (cursor in trainedDays) {
            streak++
            cursor--
        }
        return streak
    }

    private fun dayIndex(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / DAY_MS
    }

    private fun startOfWeek(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        // If setting DAY_OF_WEEK rolled forward past now, step back a week.
        if (cal.timeInMillis > millis) cal.add(Calendar.DAY_OF_YEAR, -7)
        return cal.timeInMillis
    }
}
