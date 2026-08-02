package com.replog.ui.history

import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.SetType
import java.util.Calendar

/** Pure history calculations kept independent from Compose so their date and total semantics are testable. */
internal object HistoryCalculations {
    fun isInMonth(timestamp: Long, referenceTimeMillis: Long): Boolean {
        val timestampCalendar = Calendar.getInstance().apply { timeInMillis = timestamp}

        val referenceCalendar = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis}

        return timestampCalendar.get(Calendar.YEAR) == referenceCalendar.get(Calendar.YEAR) &&
            timestampCalendar.get(Calendar.MONTH) == referenceCalendar.get(Calendar.MONTH)
   }


    fun sessionDaysInMonth(
        sessions: List<SessionWithExercises>,
        referenceTimeMillis: Long
    ): Set<Int> = sessions.mapNotNull { session ->
        if (!isInMonth(session.session.startTime, referenceTimeMillis)) return@mapNotNull null
        Calendar.getInstance().apply { timeInMillis = session.session.startTime}

            .get(Calendar.DAY_OF_MONTH)
   }
.toSet()

    fun completedWorkSets(session: SessionWithExercises): List<SetLog> =
        session.exercises.flatMap { entry ->
            entry.sets.filter { it.completed && it.setType != SetType.WARMUP && it.reps > 0}

       }


    fun sessionsThisMonth(
        sessions: List<SessionWithExercises>,
        referenceTimeMillis: Long
    ): Int = sessions.count { isInMonth(it.session.startTime, referenceTimeMillis)}


    fun monthVolume(
        sessions: List<SessionWithExercises>,
        referenceTimeMillis: Long
    ): Double = sessions
        .filter { isInMonth(it.session.startTime, referenceTimeMillis)}

        .sumOf { session -> completedWorkSets(session).sumOf { it.weight * it.reps}
}

}
