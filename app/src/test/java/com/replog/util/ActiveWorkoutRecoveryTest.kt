package com.replog.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveWorkoutRecoveryTest {
    @Test
    fun noStoredSessionReturnsNone() {
        assertEquals(
            ActiveWorkoutRecoveryDecision.NONE,
            ActiveWorkoutRecovery.decide(null, sessionExists = false, sessionEnded = false)
        )
    }

    @Test
    fun existingUnfinishedSessionResumes() {
        assertEquals(
            ActiveWorkoutRecoveryDecision.RESUME,
            ActiveWorkoutRecovery.decide(42, sessionExists = true, sessionEnded = false)
        )
    }

    @Test
    fun missingStoredSessionClearsStaleId() {
        assertEquals(
            ActiveWorkoutRecoveryDecision.CLEAR_STALE,
            ActiveWorkoutRecovery.decide(42, sessionExists = false, sessionEnded = false)
        )
    }

    @Test
    fun endedStoredSessionClearsStaleId() {
        assertEquals(
            ActiveWorkoutRecoveryDecision.CLEAR_STALE,
            ActiveWorkoutRecovery.decide(42, sessionExists = true, sessionEnded = true)
        )
    }
}
