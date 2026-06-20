package com.replog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RestoreMergePlannerTest {
    @Test
    fun filtersDuplicateSessionsAndDuplicatesWithinIncomingSet() {
        val existing = setOf(RestoreMergePlanner.sessionFingerprint(100L, 3, 9))
        val incoming = listOf(
            RestoreMergePlanner.sessionFingerprint(100L, 3, 9),
            RestoreMergePlanner.sessionFingerprint(200L, 3, 9),
            RestoreMergePlanner.sessionFingerprint(200L, 3, 9),
            RestoreMergePlanner.sessionFingerprint(300L, 2, 6)
        )

        val result = RestoreMergePlanner.filterDuplicates(
            incoming = incoming,
            existingFingerprints = existing,
            fingerprint = { it }
        )

        assertEquals(2, result.itemsToRestore.size)
        assertEquals(2, result.skippedDuplicateCount)
        assertEquals(200L, result.itemsToRestore[0].startTime)
        assertEquals(300L, result.itemsToRestore[1].startTime)
    }

    @Test
    fun bodyweightFingerprintUsesTimestampAndWeight() {
        val a = RestoreMergePlanner.bodyweightFingerprint(1L, 82.5)
        val b = RestoreMergePlanner.bodyweightFingerprint(1L, 82.5)
        val c = RestoreMergePlanner.bodyweightFingerprint(1L, 83.0)
        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}
