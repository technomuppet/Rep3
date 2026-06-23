package com.replog.domain.swap

import com.replog.data.model.Exercise

/**
 * Smart Exercise Swap (offline). When a piece of equipment is unavailable or a
 * user simply wants an alternative, suggest exercises that train the same
 * primary muscle via the same movement pattern, ranked by closeness.
 *
 * Pure library lookup + ranking — no analytics, no network.
 */
data class ExerciseSwap(
    val exercise: Exercise,
    val matchReason: String
)

object ExerciseSwapEngine {

    /**
     * Alternatives to [from], optionally excluding equipment the user can't use.
     *
     * @param excludeEquipment equipment names to exclude (e.g. ["Cable"]); the
     *        source's own equipment is always excluded so we return a real swap.
     */
    fun alternatives(
        from: Exercise,
        library: List<Exercise>,
        excludeEquipment: Set<String> = emptySet(),
        limit: Int = 5
    ): List<ExerciseSwap> {
        val fromPrimary = primarySet(from)
        if (fromPrimary.isEmpty()) return emptyList()
        val blocked = (excludeEquipment + from.equipment).map { it.lowercase() }.toSet()

        val candidates = library.asSequence()
            .filter { it.id != from.id }
            .filter { it.equipment.lowercase() !in blocked }
            .filter { primarySet(it).any { m -> m in fromPrimary } } // shares a primary muscle
            .toList()

        // Score: same movement pattern is best; then primary-muscle overlap;
        // then matching difficulty. Diversify equipment so the list is useful.
        val ranked = candidates.sortedWith(
            compareByDescending<Exercise> { if (it.movementPattern == from.movementPattern) 1 else 0 }
                .thenByDescending { primarySet(it).count { m -> m in fromPrimary } }
                .thenByDescending { if (it.difficulty == from.difficulty) 1 else 0 }
                .thenBy { it.name }
        )

        return diversifyByEquipment(ranked, limit).map { ex ->
            ExerciseSwap(
                exercise = ex,
                matchReason = matchReason(from, ex)
            )
        }
    }

    private fun matchReason(from: Exercise, to: Exercise): String = when {
        to.movementPattern == from.movementPattern && to.movementPattern.isNotBlank() ->
            "Same movement (${to.movementPattern}) on ${to.equipment}"
        else -> {
            val shared = primarySet(to).intersect(primarySet(from)).firstOrNull()?.capitalizeWords()
            if (shared != null) "Hits ${shared} on ${to.equipment}" else "${to.equipment} alternative"
        }
    }

    private fun diversifyByEquipment(ranked: List<Exercise>, limit: Int): List<Exercise> {
        val seen = mutableSetOf<String>()
        val chosen = mutableListOf<Exercise>()
        for (ex in ranked) {
            if (chosen.size >= limit) break
            if (seen.add(ex.equipment)) chosen += ex
        }
        if (chosen.size < limit) {
            for (ex in ranked) {
                if (chosen.size >= limit) break
                if (ex !in chosen) chosen += ex
            }
        }
        return chosen
    }

    private fun primarySet(ex: Exercise): Set<String> =
        ex.primaryMuscles.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
}
