package com.replog.data.model

/**
 * SQL-aggregated all-time training totals (Sprint 10, Priority 1).
 *
 * Computed entirely in the database (COUNT/SUM over completed sessions' sets) so
 * the Progress headline stats stay exact and cheap regardless of history size -
 * no need to materialise every session/set into memory. Only the deeper
 * analytics (rankings, forecasts, recovery) use a bounded recent window.
 */
data class ProgressTotalsRow(
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val totalPrs: Int
)
