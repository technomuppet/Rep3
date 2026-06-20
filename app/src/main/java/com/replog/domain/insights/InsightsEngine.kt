package com.replog.domain.insights

import com.replog.data.model.BodyweightLog
import com.replog.data.model.SessionWithExercises

data class InsightContext(
    val sessions: List<SessionWithExercises>,
    val bodyweights: List<BodyweightLog>,
    val nowMillis: Long = System.currentTimeMillis()
)

interface InsightGenerator {
    val category: InsightCategory
    fun generate(context: InsightContext): List<Insight>
}

interface InsightsEngine {
    fun generate(context: InsightContext): List<Insight>
}

class CompositeInsightsEngine(
    private val generators: List<InsightGenerator>
) : InsightsEngine {
    override fun generate(context: InsightContext): List<Insight> = generators
        .flatMap { it.generate(context) }
        .sortedWith(compareBy<Insight> { it.severity.ordinal }.thenByDescending { it.generatedAt })
}
