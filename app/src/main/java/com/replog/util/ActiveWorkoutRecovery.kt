package com.replog.util

enum class ActiveWorkoutRecoveryDecision {
    NONE,
    RESUME,
    CLEAR_STALE
}

object ActiveWorkoutRecovery {
    fun decide(storedSessionId: Int?, sessionExists: Boolean, sessionEnded: Boolean): ActiveWorkoutRecoveryDecision = when {
        storedSessionId == null || storedSessionId <= 0 -> ActiveWorkoutRecoveryDecision.NONE
        !sessionExists -> ActiveWorkoutRecoveryDecision.CLEAR_STALE
        sessionEnded -> ActiveWorkoutRecoveryDecision.CLEAR_STALE
        else -> ActiveWorkoutRecoveryDecision.RESUME
    }
}
