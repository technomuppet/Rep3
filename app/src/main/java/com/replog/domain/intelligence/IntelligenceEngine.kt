package com.replog.domain.intelligence

/**
 * RepLog Intelligence Engine (Sprint 7, Priority 1 + 6).
 *
 * A pure, offline composition layer that merges the outputs of the existing
 * engines - Recovery, Training Genome, Muscle Gap, Weekly Volume, Progression
 * Forecast, Goal Engine and Session History - into ONE explainable briefing.
 *
 * IMPORTANT: this object does NOT recompute any analysis. It only receives the
 * already-computed results (as the value objects below) and assembles them into
 * a single recommendation with human reasons. Every reason it emits is derived
 * from an input field, so the recommendation is always explainable from locally
 * stored workout data - nothing is invented.
 */

enum class BriefingConfidence { LOW, MEDIUM, HIGH }

/** A single, data-derived coaching statement (Priority 6). */
data class CoachInsight(
    val text: String,
    /** Short tag describing which data produced this (for transparency/testing). */
    val source: String
)

/** Inputs gathered by the repository layer from the existing engines. */
data class IntelligenceInputs(
    val hasEnoughData: Boolean,
    val recoveryScore: Int?,                 // 0-100, from RecoveryAnalyzer
    val recoveryStatusLabel: String? = null, // e.g. "Recovered"
    val recoveryDirective: String? = null,   // e.g. "Train hard"
    val recoveryFactors: List<String> = emptyList(),
    val isRestRecommended: Boolean = false,
    val readyMuscleGroups: List<String> = emptyList(),     // recovered enough to train
    val fatiguedMuscleGroups: List<String> = emptyList(),  // still recovering
    val recommendedFocus: String? = null,    // e.g. "Upper Push Strength" (from recommendation engine)
    val genomeBestRepRange: String? = null,  // e.g. "6-8 reps"
    val underVolumeGroups: List<String> = emptyList(),   // below optimal weekly volume
    val neglectedMuscles: List<String> = emptyList(),    // muscle-gap weak list
    val topForecastLabel: String? = null,    // e.g. "Bench Press 112.5 kg in 4 weeks"
    val topForecastConfidenceHigh: Boolean = false,
    val goalSummary: String? = null,
    val strongestDayOfWeek: String? = null,  // e.g. "Monday"
    val prsAfterRestDays: Int? = null,        // typical rest days before a PR
    val slowRecoveryAfterHighVolume: Boolean = false
)

/** The single unified output shown on Home (Priority 1). */
data class TodaysBriefing(
    val recoveryScore: Int?,
    val recommendation: String,
    val reasons: List<String>,
    val confidence: BriefingConfidence,
    val coachInsights: List<CoachInsight>
)

object IntelligenceEngine {

    fun build(inputs: IntelligenceInputs): TodaysBriefing {
        if (!inputs.hasEnoughData) {
            return TodaysBriefing(
                recoveryScore = inputs.recoveryScore,
                recommendation = "Log a few more workouts",
                reasons = listOf("RepLog needs a little more training history to personalise your coaching."),
                confidence = BriefingConfidence.LOW,
                coachInsights = emptyList()
            )
        }

        val recommendation = when {
            inputs.isRestRecommended -> "Rest or light recovery"
            !inputs.recommendedFocus.isNullOrBlank() -> inputs.recommendedFocus
            inputs.readyMuscleGroups.isNotEmpty() -> "${inputs.readyMuscleGroups.first()} focus"
            else -> "Balanced full-body session"
        }

        val reasons = buildReasons(inputs)
        val confidence = confidenceFor(inputs)
        val insights = buildCoachInsights(inputs)

        return TodaysBriefing(
            recoveryScore = inputs.recoveryScore,
            recommendation = recommendation,
            reasons = reasons,
            confidence = confidence,
            coachInsights = insights
        )
    }

    private fun buildReasons(i: IntelligenceInputs): List<String> {
        val out = mutableListOf<String>()
        if (i.readyMuscleGroups.isNotEmpty()) {
            out += "${joinHuman(i.readyMuscleGroups)} ${if (i.readyMuscleGroups.size == 1) "has" else "have"} recovered."
        }
        if (i.fatiguedMuscleGroups.isNotEmpty()) {
            out += "${joinHuman(i.fatiguedMuscleGroups)} ${if (i.fatiguedMuscleGroups.size == 1) "remains" else "remain"} fatigued."
        }
        if (i.underVolumeGroups.isNotEmpty()) {
            out += "Volume for ${joinHuman(i.underVolumeGroups)} has been below your optimal range."
        }
        if (!i.topForecastLabel.isNullOrBlank()) {
            out += "Projected: ${i.topForecastLabel}."
        }
        if (!i.goalSummary.isNullOrBlank()) {
            out += i.goalSummary
        }
        if (i.isRestRecommended && !i.recoveryDirective.isNullOrBlank()) {
            out += i.recoveryDirective
        }
        // Always fall back to a real factor rather than nothing.
        if (out.isEmpty()) {
            i.recoveryFactors.firstOrNull()?.let { out += it }
            if (out.isEmpty()) out += "Recent training looks consistent - keep progressing one variable at a time."
        }
        return out
    }

    private fun confidenceFor(i: IntelligenceInputs): BriefingConfidence {
        // Confidence rises with how many independent signals agree / are present.
        var signals = 0
        if (i.recoveryScore != null) signals++
        if (i.readyMuscleGroups.isNotEmpty() || i.fatiguedMuscleGroups.isNotEmpty()) signals++
        if (i.underVolumeGroups.isNotEmpty()) signals++
        if (i.topForecastConfidenceHigh) signals += 2 else if (!i.topForecastLabel.isNullOrBlank()) signals++
        if (!i.genomeBestRepRange.isNullOrBlank()) signals++
        return when {
            signals >= 4 -> BriefingConfidence.HIGH
            signals >= 2 -> BriefingConfidence.MEDIUM
            else -> BriefingConfidence.LOW
        }
    }

    /**
     * Priority 6 - personalised coaching, each statement strictly data-derived.
     * Only emits a statement when the underlying signal exists.
     */
    private fun buildCoachInsights(i: IntelligenceInputs): List<CoachInsight> {
        val out = mutableListOf<CoachInsight>()
        i.genomeBestRepRange?.takeIf { it.isNotBlank() }?.let {
            out += CoachInsight("You consistently progress fastest with $it.", "genome:repRange")
        }
        if (i.slowRecoveryAfterHighVolume) {
            out += CoachInsight("You recover slower after high-volume leg sessions.", "recovery:highVolume")
        }
        i.strongestDayOfWeek?.takeIf { it.isNotBlank() }?.let {
            out += CoachInsight("$it is historically your strongest training day.", "history:dayOfWeek")
        }
        i.neglectedMuscles.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
            out += CoachInsight("You frequently skip ${it.lowercase()}.", "muscleGap")
        }
        i.prsAfterRestDays?.takeIf { it > 0 }?.let {
            out += CoachInsight("You usually achieve PBs after $it rest day${if (it == 1) "" else "s"}.", "history:prRest")
        }
        return out
    }

    private fun joinHuman(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        2 -> "${items[0]} and ${items[1]}"
        else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
    }
}
