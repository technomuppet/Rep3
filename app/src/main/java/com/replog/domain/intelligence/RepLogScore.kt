package com.replog.domain.intelligence

/**
 * RepLog Score (Sprint 8, Priority 5) - one 0-100 number describing overall
 * training quality, plus a transparent breakdown.
 *
 * Pure and deterministic: it combines seven already-measured component scores
 * (each itself 0-100) using fixed weights. It performs NO analysis of raw
 * sessions - the repository feeds it component values computed by the existing
 * engines, so no business logic is duplicated and the result is fully explainable.
 */

/** One weighted component of the RepLog Score. */
data class ScoreComponent(
    val name: String,
    val value: Int,        // 0-100
    val weightPercent: Int, // contribution weight, sums to 100 across components
    val explanation: String
)

data class RepLogScoreResult(
    val score: Int,                 // 0-100 overall
    val components: List<ScoreComponent>,
    val weeklyTrend: Int?,          // delta vs the previous-week score, null if unknown
    val monthlyTrend: Int?          // delta vs the previous-month score, null if unknown
)

/** The seven pre-computed component values (each 0-100) the repository supplies. */
data class RepLogScoreInputs(
    val consistency: Int,          // training consistency / frequency adherence
    val recovery: Int,             // current overall recovery
    val progressiveOverload: Int,  // are lifts trending up
    val volumeQuality: Int,        // weekly volume within optimal ranges
    val goalAdherence: Int,        // progress toward active goals
    val muscleBalance: Int,        // how balanced muscle-group volume is
    val recoveryDiscipline: Int,   // respecting rest / not overreaching
    val previousWeekScore: Int? = null,
    val previousMonthScore: Int? = null
)

object RepLogScoreEngine {

    // Fixed weights (sum = 100). Consistency and progressive overload matter most.
    private const val W_CONSISTENCY = 20
    private const val W_RECOVERY = 12
    private const val W_OVERLOAD = 20
    private const val W_VOLUME = 16
    private const val W_GOAL = 12
    private const val W_BALANCE = 12
    private const val W_DISCIPLINE = 8

    fun compute(i: RepLogScoreInputs): RepLogScoreResult {
        val components = listOf(
            ScoreComponent("Consistency", i.consistency.clamp(), W_CONSISTENCY,
                "How regularly you train versus your typical frequency."),
            ScoreComponent("Progressive overload", i.progressiveOverload.clamp(), W_OVERLOAD,
                "Whether your key lifts are trending upward over time."),
            ScoreComponent("Volume quality", i.volumeQuality.clamp(), W_VOLUME,
                "How much of your weekly volume sits inside the optimal range."),
            ScoreComponent("Recovery", i.recovery.clamp(), W_RECOVERY,
                "Your current overall recovery score."),
            ScoreComponent("Goal adherence", i.goalAdherence.clamp(), W_GOAL,
                "Progress toward your active goals."),
            ScoreComponent("Muscle balance", i.muscleBalance.clamp(), W_BALANCE,
                "How evenly volume is spread across muscle groups."),
            ScoreComponent("Recovery discipline", i.recoveryDiscipline.clamp(), W_DISCIPLINE,
                "Respecting rest and avoiding chronic overreaching.")
        )
        val weighted = components.sumOf { it.value * it.weightPercent }
        val score = (weighted / 100.0).toInt().clamp()
        return RepLogScoreResult(
            score = score,
            components = components,
            weeklyTrend = i.previousWeekScore?.let { score - it },
            monthlyTrend = i.previousMonthScore?.let { score - it }
        )
    }

    private fun Int.clamp(): Int = coerceIn(0, 100)
}
