package com.replog.domain.pr

import com.replog.data.model.SetLog

data class PRResult(
    val isPR: Boolean,
    val types: List<String>
)

object PRDetector {
    private fun e1rm(weight: Double, reps: Int): Double =
        if (reps <= 1) weight else weight * (1 + reps / 30.0)

    fun check(
        weight: Double,
        reps: Int,
        history: List<SetLog>,
        excludeSetId: Int? = null
    ): PRResult {
        val filtered = if (excludeSetId != null) history.filter { it.id != excludeSetId } else history
        val maxWeightForReps = filtered.filter { it.reps == reps }.maxOfOrNull { it.weight } ?: 0.0
        val maxRepsAny = filtered.maxOfOrNull { it.reps } ?: 0
        val maxWeightAny = filtered.maxOfOrNull { it.weight } ?: 0.0
        val maxE1rm = filtered.maxOfOrNull { e1rm(it.weight, it.reps) } ?: 0.0
        val maxVolume = filtered.maxOfOrNull { it.weight * it.reps } ?: 0.0

        val types = mutableListOf<String>()
        if (weight > maxWeightForReps) types.add(com.replog.data.model.PRType.WEIGHT)
        if (reps > maxRepsAny) types.add(com.replog.data.model.PRType.REPS)
        if (e1rm(weight, reps) > maxE1rm) types.add(com.replog.data.model.PRType.E1RM)
        if (weight * reps > maxVolume) types.add(com.replog.data.model.PRType.VOLUME)
        if (types.isEmpty() && weight > maxWeightAny) types.add(com.replog.data.model.PRType.WEIGHT)

        return PRResult(types.isNotEmpty(), types)
    }

    fun primaryType(types: List<String>): String? = types.firstOrNull()
}
