package com.replog.domain.workout

/**
 * Decides whether a completed set closes a superset round.
 *
 * A normal exercise starts rest immediately. A linked superset starts one shared
 * rest interval only after every movement in the group has completed the same
 * set number, so A1/A2 (and longer groups) do not create multiple timers.
 */
data class SupersetSetProgress(
    val setNumber: Int,
    val isWarmup: Boolean = false
)

object SupersetRestPolicy {
    /**
     * Converts completed raw set numbers to superset round numbers. Warmups are
     * counted as a per-movement offset, while gaps in working-set numbers remain
     * visible so skipped/deleted sets cannot masquerade as completed rounds.
     */
    fun completedRoundNumbers(sets: List<SupersetSetProgress>): Set<Int> {
        val warmupNumbers = sets.filter { it.isWarmup}
.map { it.setNumber}

        return sets
            .asSequence()
            .filterNot { it.isWarmup}

            .map { set ->
                set.setNumber - warmupNumbers.count { warmupNumber -> warmupNumber < set.setNumber}

           }

            .filter { it > 0}

            .toSet()
   }


    fun shouldStartRest(
        supersetGroup: String?,
        completedSetNumber: Int,
        groupedCompletedSetNumbers: List<Set<Int>>
    ): Boolean {
        if (supersetGroup.isNullOrBlank()) return true
        if (completedSetNumber <= 0 || groupedCompletedSetNumbers.isEmpty()) return false
        // A round closes only when every movement has completed this exact round
        // and none has already moved on. Using set numbers (rather than counts)
        // also prevents gaps such as {1, 3} from masquerading as round 2.
        return groupedCompletedSetNumbers.all { completedSetNumbers ->
            completedSetNumber in completedSetNumbers &&
                completedSetNumbers.maxOrNull() == completedSetNumber &&
                (1..completedSetNumber).all { it in completedSetNumbers}

       }

   }

}
