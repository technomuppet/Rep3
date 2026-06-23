package com.replog.domain.adaptive

import com.replog.data.model.Exercise
import com.replog.data.model.PlateauEvent

/**
 * Priority 3 (#14) — Adaptive Templates.
 *
 * When a lift stalls (existing PlateauEvent), suggest a temporary variation
 * swap drawn from the existing exercise library, e.g. "Bench Press -> Paused
 * Bench Press for 4 weeks". Pure rules + library lookup; no new analytics and
 * no schema change. The UI/repository decides whether to apply the swap.
 */
data class AdaptiveSwap(
    val fromExerciseId: Int,
    val fromName: String,
    val toExerciseId: Int,
    val toName: String,
    val durationWeeks: Int,
    val reason: String
)

object AdaptiveTemplateAdvisor {

    private const val DEFAULT_WEEKS = 4

    /**
     * Ranked variation keywords to try for a stalled lift, most-preferred first.
     * These read as deliberate overload tweaks a coach would prescribe.
     */
    private val VARIATION_PREFIXES = listOf(
        "Paused", "Paused 3 Second", "Tempo", "Close Grip", "Deficit", "Pin", "Pause"
    )

    /** Build a swap suggestion for a single plateau event, if a good match exists. */
    fun suggestSwap(
        plateau: PlateauEvent,
        library: List<Exercise>,
        nowWeeks: Int = DEFAULT_WEEKS
    ): AdaptiveSwap? {
        val from = library.firstOrNull { it.id == plateau.exerciseId }
            ?: library.firstOrNull { it.name.equals(plateau.exerciseName, ignoreCase = true) }
            ?: return null

        val candidate = pickVariation(from, library) ?: pickSameMuscleAlternative(from, library) ?: return null

        return AdaptiveSwap(
            fromExerciseId = from.id,
            fromName = from.name,
            toExerciseId = candidate.id,
            toName = candidate.name,
            durationWeeks = nowWeeks,
            reason = "${from.name} has stalled (${humanReason(plateau.reason)}). " +
                "Swap to ${candidate.name} for $nowWeeks weeks to change the stimulus, then return."
        )
    }

    /** Suggest swaps for all current plateaus (deduplicated by stalled exercise). */
    fun suggestSwaps(
        plateaus: List<PlateauEvent>,
        library: List<Exercise>,
        nowWeeks: Int = DEFAULT_WEEKS
    ): List<AdaptiveSwap> =
        plateaus.distinctBy { it.exerciseId }
            .mapNotNull { suggestSwap(it, library, nowWeeks) }

    /** Prefer a named variation of the same base lift (e.g. "Paused Bench Press"). */
    private fun pickVariation(from: Exercise, library: List<Exercise>): Exercise? {
        val base = baseName(from.name)
        for (prefix in VARIATION_PREFIXES) {
            val target = "$prefix $base"
            library.firstOrNull { it.id != from.id && it.name.equals(target, ignoreCase = true) }?.let { return it }
        }
        // Any library exercise that contains the base name plus an extra qualifier.
        return library.firstOrNull {
            it.id != from.id &&
                it.name != from.name &&
                it.name.contains(base, ignoreCase = true) &&
                VARIATION_PREFIXES.any { p -> it.name.startsWith(p, ignoreCase = true) }
        }
    }

    /** Fallback: a different exercise hitting the same primary muscle + movement pattern. */
    private fun pickSameMuscleAlternative(from: Exercise, library: List<Exercise>): Exercise? {
        val primary = from.primaryMuscles.split(",").firstOrNull()?.trim()?.lowercase().orEmpty()
        if (primary.isBlank()) return null
        return library.firstOrNull {
            it.id != from.id &&
                it.movementPattern == from.movementPattern &&
                it.primaryMuscles.lowercase().contains(primary)
        } ?: library.firstOrNull {
            it.id != from.id && it.primaryMuscles.lowercase().contains(primary)
        }
    }

    /** Strip a known variation prefix to recover the base lift name. */
    private fun baseName(name: String): String {
        var n = name
        for (prefix in VARIATION_PREFIXES.sortedByDescending { it.length }) {
            if (n.startsWith("$prefix ", ignoreCase = true)) {
                n = n.substring(prefix.length).trim()
            }
        }
        return n
    }

    private fun humanReason(reason: String): String = when {
        reason.contains("LOAD", true) -> "no load increase"
        reason.contains("REP", true) -> "no rep increase"
        reason.contains("VOLUME", true) -> "no volume increase"
        reason.isBlank() -> "no recent progress"
        else -> reason.lowercase().replace("_", " ")
    }
}
