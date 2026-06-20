package com.replog.domain.forecasting

enum class ForecastType {
    STRENGTH,
    BODYWEIGHT,
    VOLUME
}

data class ForecastPoint(
    val timestamp: Long,
    val value: Double,
    val lowerBound: Double? = null,
    val upperBound: Double? = null
)

data class ForecastResult(
    val type: ForecastType,
    val subjectId: String,
    val subjectLabel: String,
    val points: List<ForecastPoint>,
    val confidence: Double,
    val explanation: String
)

interface ForecastingEngine {
    fun forecast(request: ForecastRequest): ForecastResult
}

data class ForecastRequest(
    val type: ForecastType,
    val subjectId: String,
    val horizonWeeks: Int,
    val metadata: Map<String, String> = emptyMap()
)
