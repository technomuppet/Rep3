package com.replog.domain.coach

import com.replog.domain.insights.Insight

enum class RecommendationType {
    PROGRESSION,
    RECOVERY,
    VOLUME_ADJUSTMENT,
    FREQUENCY_ADJUSTMENT,
    EXERCISE_SELECTION,
    TECHNIQUE,
    GENERAL
}

enum class RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

data class Recommendation(
    val id: String,
    val type: RecommendationType,
    val priority: RecommendationPriority,
    val title: String,
    val message: String,
    val rationale: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sourceInsightIds: List<String> = emptyList()
)

data class CoachContext(
    val insights: List<Insight>,
    val metadata: Map<String, String> = emptyMap()
)

interface RecommendationGenerator {
    fun generate(context: CoachContext): List<Recommendation>
}

interface CoachEngine {
    fun recommend(context: CoachContext): List<Recommendation>
}
