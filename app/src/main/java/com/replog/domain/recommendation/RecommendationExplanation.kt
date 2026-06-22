package com.replog.domain.recommendation

/**
 * Phase 2 — Recommendation Explanation Layer.
 *
 * Pure presentation helper. Does NOT create any new recommendation logic; it only
 * re-organises the [Recommendation.dataUsed] and [Recommendation.reasoning] strings
 * that the existing [RecommendationEngine] already produces into the four
 * user-facing contribution buckets required by the spec:
 * Recovery, Plateau, Training DNA and Frequency.
 */
data class ExplanationContribution(
    val label: String,
    val lines: List<String>
)

object RecommendationExplanation {

    /**
     * Buckets the engine's existing data/reasoning lines into the four required
     * contribution categories. Any line that does not match a category is kept
     * under "Other factors" so nothing is hidden (no black-box behaviour).
     */
    fun contributions(recommendation: Recommendation): List<ExplanationContribution> {
        val all = (recommendation.dataUsed + recommendation.reasoning)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val recovery = mutableListOf<String>()
        val plateau = mutableListOf<String>()
        val dna = mutableListOf<String>()
        val frequency = mutableListOf<String>()
        val other = mutableListOf<String>()

        for (line in all) {
            val l = line.lowercase()
            when {
                l.contains("frequency") || l.contains("x per week") || l.contains("times per week") ->
                    frequency += line
                l.contains("recovery") || l.contains("rest day") || l.contains("fatigue") ||
                    l.contains("fatigued") || l.contains("fresh") || l.contains("recent") ->
                    recovery += line
                l.contains("stall") || l.contains("plateau") || l.contains("undertrained") ->
                    plateau += line
                l.contains("training dna") || l.contains("dna") || l.contains("rep range") ||
                    l.contains("volume") || l.contains("responsive") ->
                    dna += line
                else -> other += line
            }
        }

        return buildList {
            add(ExplanationContribution("Recovery contribution", recovery.ifEmpty { listOf("No recovery signal influenced this recommendation.") }))
            add(ExplanationContribution("Plateau contribution", plateau.ifEmpty { listOf("No active plateaus were factored in.") }))
            add(ExplanationContribution("Training DNA contribution", dna.ifEmpty { listOf("Not enough Training DNA signal yet.") }))
            add(ExplanationContribution("Frequency contribution", frequency.ifEmpty { listOf("Training frequency was not a deciding factor today.") }))
            if (other.isNotEmpty()) add(ExplanationContribution("Other factors", other))
        }
    }
}
