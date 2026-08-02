package com.replog.domain.recommendation

import com.replog.data.model.Exercise
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SetLog
import com.replog.data.model.SetType
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.WorkoutSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryAnalyzerTest {
    private val now = 20L * DAY

    @Test
    fun rollingWindowsDoNotComparePartialCalendarWeeks() {
        val previous = session(
            id = 1,
            start = now - 10L * DAY,
            end = now - 10L * DAY + HOUR,
            sets = listOf(set(weight = 100.0, reps = 10))
        )
        val recent = session(
            id = 2,
            start = now - 2L * DAY,
            end = now - 2L * DAY + HOUR,
            sets = listOf(set(weight = 100.0, reps = 10))
        )

        val result = RecoveryAnalyzer.overallRecovery(listOf(previous, recent), emptyList(), now)

        assertFalse(result.reasons.any { it.contains("volume jumped") })
        assertFalse(result.reasons.any { it.contains("volume dropped") })
    }

    @Test
    fun completionTimeDrivesRecentSessionSignal() {
        val session = session(
            id = 1,
            start = now - 48L * HOUR,
            end = now - 42L * HOUR,
            sets = listOf(set(weight = 100.0, reps = 5))
        )
        val other = session(
            id = 2,
            start = now - 10L * DAY,
            end = now - 10L * DAY + HOUR,
            sets = listOf(set(weight = 100.0, reps = 5))
        )

        val result = RecoveryAnalyzer.overallRecovery(listOf(session, other), emptyList(), now)

        assertFalse(result.reasons.any { it.contains("ended recently") })
    }

    @Test
    fun completionTimeDefinesRollingVolumeWindow() {
        val crossingBoundary = session(
            id = 1,
            start = now - 8L * DAY,
            end = now - 6L * DAY,
            sets = listOf(set(weight = 100.0, reps = 10))
        )
        val older = session(
            id = 2,
            start = now - 11L * DAY,
            end = now - 10L * DAY,
            sets = listOf(set(weight = 50.0, reps = 10))
        )

        val result = RecoveryAnalyzer.overallRecovery(listOf(crossingBoundary, older), emptyList(), now)

        assertTrue(result.reasons.any { it.contains("Recent 7-day volume jumped") })
    }

    @Test
    fun warmupOnlyCompletedSessionDoesNotCountAsTrainingHistory() {
        val warmupOnly = session(
            id = 1,
            start = now - 12L * HOUR,
            end = now - 11L * HOUR,
            sets = listOf(set(weight = 20.0, reps = 10, setType = SetType.WARMUP))
        )

        val result = RecoveryAnalyzer.overallRecovery(listOf(warmupOnly), emptyList(), now)

        assertTrue(result.reasons.single().contains("Not enough history"))
        assertTrue(RecoveryAnalyzer.muscleRecovery(listOf(warmupOnly), now).isEmpty())
    }

    @Test
    fun warmupsAndIncompleteSetsDoNotInflateEffortSignals() {
        val sets = listOf(
            set(setNumber = 1, weight = 20.0, reps = 10, setType = SetType.WARMUP, rpe = 5.0),
            set(setNumber = 2, weight = 20.0, reps = 10, completed = false, rpe = 5.0),
            set(setNumber = 3, weight = 100.0, reps = 5, rpe = 9.5)
        )
        val recent = session(
            id = 1,
            start = now - 12L * HOUR,
            end = now - 11L * HOUR,
            sets = sets
        )
        val older = session(
            id = 2,
            start = now - 10L * DAY,
            end = now - 10L * DAY + HOUR,
            sets = listOf(set(weight = 100.0, reps = 5))
        )

        val result = RecoveryAnalyzer.overallRecovery(listOf(recent, older), emptyList(), now)

        assertFalse(result.reasons.any { it.contains("high proportion") })
    }

    @Test
    fun muscleFatigueUsesHardSetCountRatherThanTonnage() {
        val heavySingleSet = RecoveryAnalyzer.muscleRecovery(
            listOf(
                session(
                    id = 1,
                    start = now - 12L * HOUR,
                    end = now - 11L * HOUR,
                    sets = listOf(set(weight = 300.0, reps = 5))
                )
            ),
            now
        ).single { it.muscle == "Quadriceps" }
        val lightManySets = RecoveryAnalyzer.muscleRecovery(
            listOf(
                session(
                    id = 1,
                    start = now - 12L * HOUR,
                    end = now - 11L * HOUR,
                    sets = (1..10).map { setNumber -> set(setNumber = setNumber, weight = 10.0, reps = 5) }
                )
            ),
            now
        ).single { it.muscle == "Quadriceps" }

        assertTrue(lightManySets.recoveryScore < heavySingleSet.recoveryScore)
        assertTrue(heavySingleSet.volumeLast7Days > lightManySets.volumeLast7Days)
    }

    @Test
    fun highRpeDensityIsReportedOnlyWithAUsefulSample() {
        val sets = (1..6).map { number ->
            set(setNumber = number, weight = 50.0, reps = 5, rpe = if (number <= 3) 9.5 else 7.0)
        }
        val recent = session(id = 1, start = now - 12L * HOUR, end = now - 11L * HOUR, sets = sets)
        val older = session(id = 2, start = now - 10L * DAY, end = now - 10L * DAY + HOUR, sets = sets)

        val result = RecoveryAnalyzer.overallRecovery(listOf(recent, older), emptyList(), now)

        assertTrue(result.reasons.any { it.contains("high proportion") })
    }

    private fun session(id: Int, start: Long, end: Long, sets: List<SetLog>): SessionWithExercises {
        val exercise = Exercise(
            id = id,
            name = "Squat $id",
            category = "Legs",
            equipment = "Barbell",
            muscles = "Quadriceps",
            primaryMuscles = "Quadriceps"
        )
        val sessionExercise = SessionExercise(id = id, sessionId = id, exerciseId = id, orderIndex = 0)
        return SessionWithExercises(
            session = WorkoutSession(id = id, templateName = "Test", startTime = start, endTime = end),
            exercises = listOf(SessionExerciseWithSets(sessionExercise, sets, exercise))
        )
    }

    private fun set(
        sessionExerciseId: Int = 1,
        setNumber: Int,
        weight: Double,
        reps: Int,
        setType: String = SetType.WORKING,
        rpe: Double? = null,
        completed: Boolean = true
    ) = SetLog(
        sessionExerciseId = sessionExerciseId,
        setNumber = setNumber,
        weight = weight,
        reps = reps,
        setType = setType,
        rpe = rpe,
        completed = completed
    )

    private companion object {
        const val HOUR = 60L * 60L * 1000L
        const val DAY = 24L * HOUR
    }
}
