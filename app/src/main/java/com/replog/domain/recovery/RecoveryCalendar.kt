package com.replog.domain.recovery

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Recovery Calendar — a green/yellow/red day grid (offline).
 *
 * Derives a per-day readiness colour from training density: a day right after
 * heavy/back-to-back training reads "recovering" (red), a day after moderate
 * load reads "caution" (yellow), and a well-rested day reads "ready" (green).
 * Pure and deterministic; built from session start-times + per-session volume.
 */
enum class RecoveryDay { READY, CAUTION, RECOVERING, REST_NO_DATA }

data class RecoveryCalendarDay(
    val epochDay: Long,
    val dayLabel: String,    // e.g. "Mon"
    val dayOfMonth: Int,
    val state: RecoveryDay,
    val trainedToday: Boolean
)

object RecoveryCalendar {

    /**
     * @param sessionDaysToVolume completed-session (startTimeMillis, totalVolume) pairs
     * @param days how many trailing days to render (default 14)
     */
    fun build(
        sessionDaysToVolume: List<Pair<Long, Double>>,
        nowMillis: Long,
        days: Int = 14
    ): List<RecoveryCalendarDay> {
        // Aggregate volume per calendar day.
        val volumeByDay = HashMap<Long, Double>()
        val trainedDays = HashSet<Long>()
        for ((ts, vol) in sessionDaysToVolume) {
            val d = dayIndex(ts)
            volumeByDay[d] = (volumeByDay[d] ?: 0.0) + vol
            trainedDays += d
        }

        val today = dayIndex(nowMillis)
        // Rolling baseline for "heavy" — median-ish via average of trained days.
        val trainedVolumes = volumeByDay.values.filter { it > 0 }
        val avgVolume = if (trainedVolumes.isNotEmpty()) trainedVolumes.average() else 0.0

        return (0 until days).map { offset ->
            val day = today - (days - 1 - offset)
            val trainedToday = day in trainedDays
            val state = stateFor(day, trainedDays, volumeByDay, avgVolume)
            RecoveryCalendarDay(
                epochDay = day,
                dayLabel = weekdayLabel(day),
                dayOfMonth = dayOfMonth(day),
                state = state,
                trainedToday = trainedToday
            )
        }
    }

    private fun stateFor(
        day: Long,
        trainedDays: Set<Long>,
        volumeByDay: Map<Long, Double>,
        avgVolume: Double
    ): RecoveryDay {
        if (trainedDays.isEmpty()) return RecoveryDay.REST_NO_DATA

        val yesterday = day - 1
        val twoAgo = day - 2
        val trainedYesterday = yesterday in trainedDays
        val trainedTwoAgo = twoAgo in trainedDays
        val trainedToday = day in trainedDays

        val yVol = volumeByDay[yesterday] ?: 0.0
        val heavyYesterday = avgVolume > 0 && yVol >= avgVolume * 1.15

        return when {
            // Back-to-back days, or a heavy session yesterday → still recovering.
            trainedYesterday && (trainedTwoAgo || heavyYesterday) -> RecoveryDay.RECOVERING
            // Trained yesterday (moderate) or today → some fatigue present.
            trainedYesterday || trainedToday -> RecoveryDay.CAUTION
            // Otherwise rested and ready.
            else -> RecoveryDay.READY
        }
    }

    /** Converts an instant to its local calendar day without legacy Calendar truncation. */
    private fun dayIndex(millis: Long): Long =
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()

    private fun dateForDay(day: Long): LocalDate = LocalDate.ofEpochDay(day)

    private fun weekdayLabel(day: Long): String =
        dateForDay(day).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    private fun dayOfMonth(day: Long): Int = dateForDay(day).dayOfMonth
}
