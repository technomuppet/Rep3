package com.replog.domain.recovery

import com.replog.data.model.SessionWithExercises

enum class RecoveryState {
    READY,
    RECOVERING,
    FATIGUED,
    UNKNOWN
}

data class MuscleRecoverySignal(
    val muscle: String,
    val state: RecoveryState,
    val score: Int,
    val explanation: String
)

interface RecoveryAnalyzer {
    fun analyzeByMuscle(sessions: List<SessionWithExercises>, nowMillis: Long = System.currentTimeMillis()): List<MuscleRecoverySignal>
}
