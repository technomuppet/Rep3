package com.replog.data.model

/**
 * Lightweight, SQL-aggregated per-session summary used by dashboards and stats.
 *
 * Avoids loading the full session -> exercises -> sets object graph just to
 * compute counts/volume/streaks. Volume is summed in SQL via a LEFT JOIN, so a
 * user with thousands of sessions and tens of thousands of sets pays only for a
 * compact projection rather than the entire history.
 */
data class SessionSummaryRow(
    val id: Int,
    val startTime: Long,
    val endTime: Long?,
    val volume: Double
)
