package com.replog.ui.history

import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.SetType
import com.replog.data.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HistoryCalculationsTest {
    @Test
    fun monthStatsUseYearAndMonthNotOnlyDayOfMonth() {
        val reference = calendar(2026, Calendar.AUGUST, 12, 12).timeInMillis
        val currentMonth = session(1, calendar(2026, Calendar.AUGUST, 12, 9).timeInMillis, listOf(workSet(100.0, 5)))
        val priorMonthSameDay = session(2, calendar(2026, Calendar.JULY, 12, 9).timeInMillis, listOf(workSet(200.0, 5)))

        assertEquals(1, HistoryCalculations.sessionsThisMonth(listOf(currentMonth, priorMonthSameDay), reference))
        assertEquals(setOf(12), HistoryCalculations.sessionDaysInMonth(listOf(currentMonth, priorMonthSameDay), reference))
        assertEquals(500.0, HistoryCalculations.monthVolume(listOf(currentMonth, priorMonthSameDay), reference), 0.0)}


    @Test
    fun monthVolumeExcludesWarmupIncompleteAndNonPositiveRepSets() {
        val reference = calendar(2026, Calendar.AUGUST, 12, 12).timeInMillis
        val session = session(
            1,
            reference,
            listOf(
                workSet(100.0, 5),
                workSet(50.0, 10, setType = SetType.WARMUP),
                workSet(999.0, 5, completed = false),
                workSet(999.0, 0)
            )
        )

        assertEquals(1, HistoryCalculations.completedWorkSets(session).size)
        assertEquals(500.0, HistoryCalculations.monthVolume(listOf(session), reference), 0.0)}


    @Test
    fun monthChecksHandleYearBoundary() {
        val december = calendar(2025, Calendar.DECEMBER, 31, 23).timeInMillis
        val january = calendar(2026, Calendar.JANUARY, 1, 1).timeInMillis

        assertFalse(HistoryCalculations.isInMonth(december, january))
        assertTrue(HistoryCalculations.isInMonth(january, january))}


    private fun session(id: Int, startTime: Long, sets: List<SetLog>): SessionWithExercises {
        val exercise = Exercise(
            id = id,
            name = "Test Exercise $id",
            category = "Test",
            equipment = "Barbell",
            muscles = "Quadriceps",
            primaryMuscles = "Quadriceps"
        )
        val sessionExercise = SessionExercise(id = id, sessionId = id, exerciseId = id, orderIndex = 0)
        return SessionWithExercises(
            session = WorkoutSession(id = id, templateName = "Test", startTime = startTime, endTime = startTime + 60_000L),
            exercises = listOf(SessionExerciseWithSets(sessionExercise, sets, exercise))
        )}


    private fun workSet(
        weight: Double,
        reps: Int,
        setType: String = SetType.WORKING,
        completed: Boolean = true
    ) = SetLog(sessionExerciseId = 1, setNumber = 1, weight = weight, reps = reps, setType = setType, completed = completed)

    private fun calendar(year: Int, month: Int, day: Int, hour: Int): Calendar = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, 0, 0)}

}
