package com.replog.domain.trainingdna

enum class TrainingDnaDimension {
    RECOVERY_SPEED,
    VOLUME_TOLERANCE,
    FREQUENCY_TOLERANCE,
    PREFERRED_REP_RANGE,
    EXERCISE_RESPONSIVENESS,
    FATIGUE_SENSITIVITY
}

data class TrainingDnaSignal(
    val dimension: TrainingDnaDimension,
    val subjectType: String,
    val subjectId: String? = null,
    val value: Double,
    val confidence: Double,
    val sampleSize: Int,
    val metadata: Map<String, String> = emptyMap()
)

interface TrainingDnaStore {
    suspend fun getSignals(): List<TrainingDnaSignal>
    suspend fun upsertSignal(signal: TrainingDnaSignal)
}
