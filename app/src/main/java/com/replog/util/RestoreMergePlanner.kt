package com.replog.util

data class RestoreSessionFingerprint(
    val startTime: Long,
    val exerciseCount: Int,
    val setCount: Int
)

data class RestoreBodyweightFingerprint(
    val timestamp: Long,
    val weight: Double
)

data class RestoreMergeDecision<T>(
    val itemsToRestore: List<T>,
    val skippedDuplicateCount: Int
)

object RestoreMergePlanner {
    fun sessionFingerprint(startTime: Long, exerciseCount: Int, setCount: Int): RestoreSessionFingerprint =
        RestoreSessionFingerprint(startTime, exerciseCount, setCount)

    fun bodyweightFingerprint(timestamp: Long, weight: Double): RestoreBodyweightFingerprint =
        RestoreBodyweightFingerprint(timestamp, weight)

    fun <T, K> filterDuplicates(
        incoming: List<T>,
        existingFingerprints: Set<K>,
        fingerprint: (T) -> K
    ): RestoreMergeDecision<T> {
        val accepted = mutableListOf<T>()
        var skipped = 0
        val seen = existingFingerprints.toMutableSet()
        incoming.forEach { item ->
            val key = fingerprint(item)
            if (key in seen) {
                skipped++
            } else {
                seen += key
                accepted += item
            }
        }
        return RestoreMergeDecision(accepted, skipped)
    }
}
