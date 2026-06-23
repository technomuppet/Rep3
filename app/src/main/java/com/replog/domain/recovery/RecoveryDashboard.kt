package com.replog.domain.recovery

import com.replog.domain.recommendation.OverallRecovery

/**
 * Priority 3 (#13) — Recovery Dashboard.
 *
 * Presentation layer over the existing RecoveryAnalyzer.overallRecovery output.
 * Produces a daily score (0–100), a clear training directive, and the
 * contributing factors already computed by the analyzer (frequency, volume
 * jumps, RPE, rest). No new scoring engine.
 */
data class RecoveryDashboardState(
    val score: Int,
    val statusLabel: String,
    val directive: String,
    val directiveDetail: String,
    val factors: List<String>
)

enum class RecoveryDirective(val label: String) {
    TRAIN_HARD("Train Hard"),
    TRAIN_NORMAL("Train"),
    TRAIN_LIGHT("Train Light"),
    REST("Rest Day Recommended")
}

object RecoveryDashboard {

    fun from(recovery: OverallRecovery): RecoveryDashboardState {
        val score = recovery.score.toInt().coerceIn(0, 100)
        val directive = directiveFor(score)
        return RecoveryDashboardState(
            score = score,
            statusLabel = recovery.label,
            directive = directive.label,
            directiveDetail = detailFor(directive),
            factors = recovery.reasons.ifEmpty { listOf("Recovery looks normal — no strong signals either way.") }
        )
    }

    fun directiveFor(score: Int): RecoveryDirective = when {
        score >= 80 -> RecoveryDirective.TRAIN_HARD
        score >= 60 -> RecoveryDirective.TRAIN_NORMAL
        score >= 40 -> RecoveryDirective.TRAIN_LIGHT
        else -> RecoveryDirective.REST
    }

    private fun detailFor(d: RecoveryDirective): String = when (d) {
        RecoveryDirective.TRAIN_HARD -> "You're well recovered. A good day to push intensity or attack a PR."
        RecoveryDirective.TRAIN_NORMAL -> "Recovery is solid. Train as planned and progress where it feels right."
        RecoveryDirective.TRAIN_LIGHT -> "Recovery is a bit low. Keep volume moderate and leave a rep in reserve."
        RecoveryDirective.REST -> "Fatigue signals are high. Rest or do light mobility — you'll come back stronger."
    }
}
