package com.replog.domain.recovery

import com.replog.domain.recommendation.MuscleRecovery

/**
 * Turns existing recovery signals into concise, actionable training guidance.
 *
 * This is coaching-oriented presentation logic, not medical advice. It does not
 * create a second recovery score or require any persisted data.
 */
data class RecoveryGuidanceResult(
    val improvements: List<String>,
    val warnings: List<String>
)

object RecoveryGuidance {
    fun build(
        score: Int,
        recovered: List<MuscleRecovery>,
        fatigued: List<MuscleRecovery>,
        factors: List<String>
    ): RecoveryGuidanceResult {
        val improvements = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        when {
            score >= 80 -> improvements += "Recovery is in a strong place; progress one variable if your technique feels solid."
            score >= 60 -> improvements += "Recovery looks steady; follow your plan and adjust effort to how the session feels."
            score >= 45 -> warnings += "Keep volume moderate today and leave a little in reserve."
            else -> warnings += "Choose rest, mobility, or an easy session rather than pushing intensity today."
        }

        recovered.take(3).map { it.muscle }.takeIf { it.isNotEmpty() }?.let { muscles ->
            improvements += "Ready to train: ${muscles.joinToString(", ")}."
        }
        fatigued.take(2).map { it.muscle }.takeIf { it.isNotEmpty() }?.let { muscles ->
            warnings += "Give ${muscles.joinToString(" and ")} another recovery window; train around them if needed."
        }

        val factorText = factors.joinToString(" ").lowercase()
        if ("volume jumped" in factorText || "dense two-week" in factorText) {
            warnings += "Recent workload is elevated; avoid adding extra sets today."
        }
        if ("high-rpe" in factorText || "rpe" in factorText && "very high" in factorText) {
            warnings += "Recent effort was high; use longer rests and stop a set if form breaks down."
        }

        return RecoveryGuidanceResult(
            improvements = improvements.distinct(),
            warnings = warnings.distinct()
        )
    }
}
