package com.replog.domain.insights

enum class InsightCategory {
    PR_OPPORTUNITY,
    PLATEAU_WARNING,
    RECOVERY_WARNING,
    MUSCLE_IMBALANCE,
    VOLUME_TREND,
    FREQUENCY_TREND,
    ADHERENCE,
    GENERAL
}

enum class InsightSeverity {
    INFO,
    POSITIVE,
    WARNING,
    CRITICAL
}

data class Insight(
    val id: String,
    val category: InsightCategory,
    val severity: InsightSeverity,
    val title: String,
    val message: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val relatedEntityType: String? = null,
    val relatedEntityId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
