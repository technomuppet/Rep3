package com.replog.domain.genome

import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetType
import com.replog.util.PRCalculator

/**
 * Training Genome — the long-term moat.
 *
 * Where Training DNA captures *preferences* (what you do most), the Genome
 * learns *response patterns over months*: it correlates training INPUTS
 * (rep range, weekly sets, frequency, recovery gap) against the OUTCOME
 * (estimated-1RM growth) to surface how THIS user grows best.
 *
 * Fully offline, deterministic, no new tables — computed from existing
 * session history (the same source the DNA engine uses).
 */

enum class GenomeConfidence { EMERGING, MODERATE, STRONG }

/** A single learned "you grow best with…" trait. */
data class GenomeTrait(
    val dimension: String,   // "Rep range" | "Weekly sets" | "Frequency" | "Recovery"
    val bestValue: String,   // e.g. "4-6 reps", "14 sets/week", "4x/week", "72h"
    val detail: String,
    val confidence: GenomeConfidence
)

data class TrainingGenome(
    val hasEnoughData: Boolean,
    val windowsAnalyzed: Int,
    val traits: List<GenomeTrait>,
    val summary: String
)

/** One ~monthly training block + the growth it produced. */
data class GenomeWindow(
    val startMillis: Long,
    val sessions: Int,
    val avgRepBucket: String,
    val weeklySets: Double,
    val weeklyFrequency: Double,
    val avgRecoveryHours: Double,
    val growthPctPerWeek: Double
)

object TrainingGenomeEngine {

    private const val DAY = 24L * 60L * 60L * 1000L
    private const val WINDOW = 28L * DAY
    private const val MIN_WINDOWS = 3

    fun analyze(sessions: List<SessionWithExercises>, nowMillis: Long): TrainingGenome {
        val completed = sessions.filter { it.session.endTime != null }
            .sortedBy { it.session.startTime }
        if (completed.size < 8) return notEnough(0)

        val rated = buildWindows(completed).filter { it.sessions >= 2 }
        if (rated.size < MIN_WINDOWS) return notEnough(rated.size)

        val confidence = when {
            rated.size >= 6 -> GenomeConfidence.STRONG
            rated.size >= 4 -> GenomeConfidence.MODERATE
            else -> GenomeConfidence.EMERGING
        }

        val traits = listOfNotNull(
            bestCategorical(rated, confidence, "Rep range", "You grow best training in", { it.avgRepBucket }) { repLabel(it) },
            bestNumeric(rated, confidence, "Weekly sets", "Your best growth came at", { it.weeklySets }) { "${it.toInt()} sets/week" },
            bestNumeric(rated, confidence, "Frequency", "You respond best to", { it.weeklyFrequency }) { "${roundHalf(it)}x/week" },
            bestNumeric(rated, confidence, "Recovery", "You recover and grow best with", { it.avgRecoveryHours }) { "${it.toInt()}h between sessions" }
        )

        return TrainingGenome(
            hasEnoughData = traits.isNotEmpty(),
            windowsAnalyzed = rated.size,
            traits = traits,
            summary = if (traits.isEmpty())
                "Not enough variation in your training yet to find clear growth drivers."
            else
                "Learned from ${rated.size} training blocks of your own data — this is how you grow best."
        )
    }

    private fun notEnough(windows: Int) = TrainingGenome(
        hasEnoughData = false,
        windowsAnalyzed = windows,
        traits = emptyList(),
        summary = "Keep training — your Genome needs a few months of data to learn how you grow best."
    )

    // --- window construction ---

    private fun buildWindows(sessions: List<SessionWithExercises>): List<GenomeWindow> {
        if (sessions.isEmpty()) return emptyList()
        val start = sessions.first().session.startTime
        val end = sessions.last().session.startTime
        val windows = mutableListOf<GenomeWindow>()
        var ws = start
        while (ws <= end) {
            val we = ws + WINDOW
            val inWindow = sessions.filter { it.session.startTime in ws until we }
            if (inWindow.isNotEmpty()) windows += summarizeWindow(ws, inWindow)
            ws = we
        }
        return windows
    }

    private fun summarizeWindow(start: Long, w: List<SessionWithExercises>): GenomeWindow {
        val workingSets = w.flatMap { it.exercises }.flatMap { it.sets }
            .filter { it.setType != SetType.WARMUP }
        val repBucket = workingSets.groupBy { repBucket(it.reps) }
            .maxByOrNull { it.value.size }?.key ?: "Unknown"
        val starts = w.map { it.session.startTime }.sorted()
        // Normalise rates by the ACTIVE span of training in this block (not the
        // fixed 28-day window), so sparse weeks don't understate weekly volume.
        val activeWeeks = (((starts.last() - starts.first()) / (7.0 * DAY)))
            .coerceAtLeast(1.0)
        val weeklySets = workingSets.size / activeWeeks
        val weeklyFrequency = w.size / activeWeeks
        val gaps = starts.zipWithNext { a, b -> (b - a) / 3_600_000.0 }
        val avgRecoveryHours = if (gaps.isEmpty()) 0.0 else gaps.average()
        return GenomeWindow(start, w.size, repBucket, weeklySets, weeklyFrequency, avgRecoveryHours, growthPctPerWeek(w))
    }

    /** e1RM growth rate (%/week) across the window using best-set e1RM trend. */
    private fun growthPctPerWeek(w: List<SessionWithExercises>): Double {
        val points = w.flatMap { s ->
            s.exercises.flatMap { e ->
                e.sets.filter { it.setType != SetType.WARMUP }
                    .map { it.timestamp to PRCalculator.epley1RM(it.weight, it.reps) }
            }
        }.sortedBy { it.first }
        if (points.size < 2) return 0.0
        val firstBest = points.take(points.size / 3 + 1).maxOf { it.second }
        val lastBest = points.takeLast(points.size / 3 + 1).maxOf { it.second }
        if (firstBest <= 0) return 0.0
        val weeks = ((points.last().first - points.first().first) / (7.0 * DAY)).coerceAtLeast(1.0)
        return ((lastBest - firstBest) / firstBest) * 100.0 / weeks
    }

    // --- correlation: which input value coincided with the best growth ---

    private fun bestCategorical(
        windows: List<GenomeWindow>,
        confidence: GenomeConfidence,
        dimension: String,
        prefix: String,
        key: (GenomeWindow) -> String,
        label: (String) -> String
    ): GenomeTrait? {
        val byKey = windows.groupBy(key).filterKeys { it != "Unknown" }
        if (byKey.isEmpty()) return null
        val best = byKey.maxByOrNull { (_, ws) -> ws.map { it.growthPctPerWeek }.average() } ?: return null
        val bestGrowth = best.value.map { it.growthPctPerWeek }.average()
        if (bestGrowth <= 0.0) return null
        // If the user consistently trains one rep range and it produces growth,
        // that single dominant bucket is itself a valid signal.
        return GenomeTrait(
            dimension = dimension,
            bestValue = label(best.key),
            detail = "$prefix ${label(best.key)} — your strongest gains came from those blocks.",
            confidence = confidence
        )
    }

    private fun bestNumeric(
        windows: List<GenomeWindow>,
        confidence: GenomeConfidence,
        dimension: String,
        prefix: String,
        value: (GenomeWindow) -> Double,
        label: (Double) -> String
    ): GenomeTrait? {
        val valid = windows.filter { value(it) > 0.0 }
        if (valid.size < 2) return null
        // Split windows into "lower" and "higher" halves of the input and compare growth.
        val sorted = valid.sortedBy { value(it) }
        val mid = sorted.size / 2
        val lower = sorted.take(mid).ifEmpty { return null }
        val upper = sorted.drop(mid).ifEmpty { return null }
        val lowerGrowth = lower.map { it.growthPctPerWeek }.average()
        val upperGrowth = upper.map { it.growthPctPerWeek }.average()
        val winner = if (upperGrowth >= lowerGrowth) upper else lower
        val winnerGrowth = if (upperGrowth >= lowerGrowth) upperGrowth else lowerGrowth
        if (winnerGrowth <= 0.0) return null
        val bestValueNum = winner.map { value(it) }.average()
        return GenomeTrait(
            dimension = dimension,
            bestValue = label(bestValueNum),
            detail = "$prefix ${label(bestValueNum)}.",
            confidence = confidence
        )
    }

    private fun repBucket(reps: Int): String = when {
        reps <= 3 -> "1-3"
        reps <= 6 -> "4-6"
        reps <= 10 -> "7-10"
        reps <= 15 -> "11-15"
        else -> "16+"
    }

    private fun repLabel(bucket: String): String = "$bucket reps"

    private fun roundHalf(v: Double): String {
        val r = Math.round(v * 2) / 2.0
        return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
    }
}
