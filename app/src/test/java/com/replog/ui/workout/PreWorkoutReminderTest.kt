package com.replog.ui.workout

import com.replog.domain.recovery.NutritionGuidelines
import com.replog.util.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PreWorkoutReminderTest {

    @Test
    fun missingProfileDoesNotEmitATip() {
        val reminder = PreWorkoutReminder()
        reminder.show(null, skipCount = 0)
        assertNull(reminder.state.value)
    }

    @Test
    fun profileWithoutWeightDoesNotEmitATip() {
        val reminder = PreWorkoutReminder()
        reminder.show(UserProfile("Athlete"), skipCount = 0)
        assertNull(reminder.state.value)
    }

    @Test
    fun profileWithWeightEmitsThePreWorkoutTip() {
        val reminder = PreWorkoutReminder()
        reminder.show(UserProfile("Athlete", weightKg = 80.0), skipCount = 0)
        assertNotNull(reminder.state.value)
        assertEquals(NutritionGuidelines.preWorkoutTip(), reminder.state.value)
    }

    @Test
    fun invalidWeightsDoNotEmitATip() {
        val reminder = PreWorkoutReminder()
        listOf(0.0, 25.0, 350.0).forEach { weight ->
            reminder.show(UserProfile("Athlete", weightKg = weight), skipCount = 0)
            assertNull("weight=$weight", reminder.state.value)
        }
    }

    @Test
    fun clearResetsTheReminder() {
        val reminder = PreWorkoutReminder()
        reminder.show(UserProfile("Athlete", weightKg = 80.0), skipCount = 0)
        assertNotNull(reminder.state.value)

        reminder.clear()
        assertNull(reminder.state.value)
    }

    @Test
    fun showingWithoutWeightClearsAPreviouslyShownTip() {
        val reminder = PreWorkoutReminder()
        reminder.show(UserProfile("Athlete", weightKg = 80.0), skipCount = 0)
        assertNotNull(reminder.state.value)

        reminder.show(UserProfile("Athlete"), skipCount = 0)
        assertNull(reminder.state.value)
    }

    @Test
    fun skipsBelowTheLimitStillShowTheTip() {
        val reminder = PreWorkoutReminder(maxSkipsBeforeSuppression = 3)
        reminder.show(UserProfile("Athlete", weightKg = 80.0), skipCount = 2)
        assertNotNull(reminder.state.value)
    }

    @Test
    fun reachingTheSkipLimitSuppressesTheReminder() {
        val reminder = PreWorkoutReminder(maxSkipsBeforeSuppression = 3)
        reminder.show(UserProfile("Athlete", weightKg = 80.0), skipCount = 3)
        assertNull(reminder.state.value)
    }

    @Test
    fun onceSuppressedAReminderStaysSuppressed() {
        val reminder = PreWorkoutReminder(maxSkipsBeforeSuppression = 1)
        reminder.show(UserProfile("Athlete", weightKg = 80.0), skipCount = 0)
        assertNotNull(reminder.state.value)

        reminder.clear()
        reminder.show(UserProfile("Athlete", weightKg = 80.0), skipCount = 1)
        assertNull(reminder.state.value)
    }

    @Test
    fun suppressedShowClearsAnyPreviouslyShownTip() {
        val reminder = PreWorkoutReminder(maxSkipsBeforeSuppression = 1)
        reminder.show(UserProfile("Athlete", weightKg = 80.0), skipCount = 0)
        assertNotNull(reminder.state.value)

        reminder.show(UserProfile("Athlete", weightKg = 80.0), skipCount = 1)
        assertNull(reminder.state.value)
    }

    @Test
    fun activeSnoozeSuppressesTheReminderEvenBeforeSkipLimit() {
        val reminder = PreWorkoutReminder(maxSkipsBeforeSuppression = 3)
        val now = 1_000L

        reminder.show(
            UserProfile("Athlete", weightKg = 80.0),
            skipCount = 0,
            snoozedUntilMillis = now + 1L,
            nowMillis = now
        )

        assertNull(reminder.state.value)
    }

    @Test
    fun expiredSnoozeAllowsTheReminderAgain() {
        val reminder = PreWorkoutReminder(maxSkipsBeforeSuppression = 3)
        val now = 1_000L

        reminder.show(
            UserProfile("Athlete", weightKg = 80.0),
            skipCount = 0,
            snoozedUntilMillis = now,
            nowMillis = now
        )

        assertNotNull(reminder.state.value)
    }
}
