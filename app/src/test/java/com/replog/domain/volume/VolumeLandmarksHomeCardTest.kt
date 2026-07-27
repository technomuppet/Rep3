package com.replog.domain.volume

import com.replog.data.model.Exercise
import com.replog.data.model.SessionExerciseWithSets
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.SetType
import com.replog.data.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2 Gap 5 — deterministic JUnit 4 tests for the Home Weekly Volume card.
 *
 * Drives the pure `VolumeLandmarks.analyze()` engine that BOTH
 *
 *   1. the Home `WeeklyLandmarksCard` composable, and
 *   2. the IntelligenceRepository `buildBriefing()` explain-section ("Weekly
 *      Volume: Below optimal: …")
 *
 * consume. Locking the four canonical status transitions (NONE / UNDER /
 * IN_RANGE / ABOVE) plus the multi-week normalisation prevents regressions
 * in either UI consumer when the engine is tuned.
 *
 * Run host-side via
 * `./gradlew :app:testDebugUnitTest --tests com.replog.domain.volume.VolumeLandmarksHomeCardTest`.
 */
class VolumeLandmarksHomeCardTest {

    private val DAY_MS = 24L * 60L * 60L * 1000L

    @Test(timeout = 1_000L)
    fun emptySessions_yieldsAllNONE_thusHomeStaysHidden() {
        val lands = VolumeLandmarks.analyze(emptyList(), System.currentTimeMillis(), weeks = 1)
        assertEquals(
            "the engine always returns the full hierarchy (10 hypertrophy groups) even on empty input",
            10,
            lands.size
        )
        assertTrue(
            "every group should report NONE when no sessions are logged",
            lands.all { it.status == VolumeStatus.NONE }
        )
    }

    @Test(timeout = 1_000L)
    fun singleChestSessionTwelveWorkingSets_isIN_RANGE() {
        val now = System.currentTimeMillis()
        val session = completedSession(
            startMs = now - DAY_MS / 4L,
            exercises = listOf(exerciseFor(id = 1, primary = "Pectorals", secondary = "", workingSets = 12))
        )
        val chest = landmark(VolumeLandmarks.analyze(listOf(session), now, weeks = 1), "Chest")
        // Chest optimal range is 10..20 working sets/wk. 12 ⇒ IN_RANGE.
        assertEquals(VolumeStatus.IN_RANGE, chest.status)
    }

    @Test(timeout = 1_000L)
    fun singleChestSessionFourWorkingSets_isUNDER() {
        val now = System.currentTimeMillis()
        val session = completedSession(
            startMs = now - DAY_MS / 4L,
            exercises = listOf(exerciseFor(id = 1, primary = "Pectorals", secondary = "", workingSets = 4))
        )
        val chest = landmark(VolumeLandmarks.analyze(listOf(session), now, weeks = 1), "Chest")
        // 4 < 10 ⇒ UNDER. This is exactly the case the Home card highlights
        // with the tertiary-coloured bar + "Below optimal" badge.
        assertEquals(VolumeStatus.UNDER, chest.status)
    }

    @Test(timeout = 1_000L)
    fun singleChestSessionTwentyFiveWorkingSets_isABOVE() {
        val now = System.currentTimeMillis()
        val session = completedSession(
            startMs = now - DAY_MS / 4L,
            exercises = listOf(exerciseFor(id = 1, primary = "Pectorals", secondary = "", workingSets = 25))
        )
        val chest = landmark(VolumeLandmarks.analyze(listOf(session), now, weeks = 1), "Chest")
        // 25 > 20 ⇒ ABOVE. The card renders this with the secondary-colour bar
        // so the user can spot creep before it becomes a recovery problem.
        assertEquals(VolumeStatus.ABOVE, chest.status)
    }

    @Test(timeout = 1_000L)
    fun twoWeekWindowNormalisesTotalToPerWeek_andMatchesIN_RANGE() {
        val now = System.currentTimeMillis()
        // Two sessions, ten chest working sets each, ten days apart,
        // over a 2-week window ⇒ per-week volume = 10 ⇒ Chest IN_RANGE.
        val sessionA = completedSession(
            startMs = now - 1L * DAY_MS,
            exercises = listOf(exerciseFor(id = 1, primary = "Pectorals", secondary = "", workingSets = 10))
        )
        val sessionB = completedSession(
            startMs = now - 11L * DAY_MS,
            exercises = listOf(exerciseFor(id = 1, primary = "Pectorals", secondary = "", workingSets = 10))
        )
        val chest = landmark(
            VolumeLandmarks.analyze(listOf(sessionA, sessionB), now, weeks = 2),
            "Chest"
        )
        assertEquals(10.0, chest.weeklySets, 0.01)
        assertEquals(VolumeStatus.IN_RANGE, chest.status)
    }

    // ---------------------------------------------------------------------
    // Fixture helpers — minimal SessionWithExercises with a single chest
    // entry containing `workingSets` working sets.
    // ---------------------------------------------------------------------

    private fun landmark(landmarks: List<VolumeLandmark>, group: String): VolumeLandmark =
        landmarks.first { it.muscleGroup == group }

    private fun completedSession(
        startMs: Long,
        exercises: List<SessionExerciseWithSets>
    ): SessionWithExercises {
        val ws = WorkoutSession(
            startTime = startMs,
            endTime = startMs + 30L * 60L * 1000L
        )
        return SessionWithExercises(session = ws, exercises = exercises)
    }

    private fun exerciseFor(
        id: Int,
        primary: String,
        secondary: String,
        workingSets: Int
    ): SessionExerciseWithSets {
        val exercise = Exercise(
            id = id,
            name = "test-$primary-$id",
            category = groupFor(primary),
            equipment = "Barbell",
            primaryMuscles = primary,
            secondaryMuscles = secondary,
            movementPattern = "",
            muscles = listOf(primary, secondary).filter { it.isNotBlank() }.joinToString(",")
        )
        val sets = (1..workingSets).map { setNumber ->
            SetLog(
                sessionExerciseId = id,
                setNumber = setNumber,
                weight = 60.0,
                reps = 8,
                setType = SetType.WORKING,
                completed = true
            )
        }
        val sessionExercise = com.replog.data.model.SessionExercise(
            id = id,
            sessionId = id,
            exerciseId = id,
            orderIndex = 0
        )
        return SessionExerciseWithSets(
            sessionExercise = sessionExercise,
            sets = sets,
            exercise = exercise
        )
    }

    private fun groupFor(primary: String): String = when (primary.lowercase()) {
        "pectorals" -> "Chest"
        else -> primary
    }
}
