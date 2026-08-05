package com.replog.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.replog.util.profile.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesManagerReminderTest {

    @Test
    fun dismissalAndSnoozeStatePersistAndExpireAtomically() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        val now = 10_000L
        val duration = 7L * 24L * 60L * 60L * 1000L

        prefs.clearPreWorkoutReminderSuppression()
        prefs.recordPreWorkoutReminderDismissal()
        assertEquals(1, prefs.preWorkoutReminderSkips.first())

        prefs.snoozePreWorkoutReminder(nowMillis = now, durationMillis = duration)
        assertEquals(now + duration, prefs.preWorkoutReminderSnoozedUntil.first())
        assertFalse(prefs.resetPreWorkoutReminderIfSnoozeExpired(now + duration - 1L))
        assertEquals(1, prefs.preWorkoutReminderSkips.first())

        assertTrue(prefs.resetPreWorkoutReminderIfSnoozeExpired(now + duration))
        assertEquals(0, prefs.preWorkoutReminderSkips.first())
        assertNull(prefs.preWorkoutReminderSnoozedUntil.first())

        // Keep the shared test DataStore clean for other instrumentation tests.
        prefs.clearPreWorkoutReminderSuppression()
    }
}
