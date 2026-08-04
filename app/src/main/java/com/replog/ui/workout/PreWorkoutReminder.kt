package com.replog.ui.workout

import com.replog.domain.recovery.NutritionGuidelines
import com.replog.util.profile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transient pre-workout fuel reminder shown right after a session starts.
 *
 * Pure Kotlin (no Android dependencies) so the "only for onboarded users with
 * a usable weight" gate, the skip-limit suppression, and the dismiss behaviour
 * are unit-testable without the Hilt ViewModel graph.
 */
class PreWorkoutReminder(
    private val maxSkipsBeforeSuppression: Int = DEFAULT_MAX_SKIPS,
    private val flow: MutableStateFlow<String?> = MutableStateFlow(null)
) {
    val state: StateFlow<String?> = flow

    /**
     * Show the pre-workout tip, or clear when the profile has no usable weight
     * or the user has already skipped the reminder [maxSkipsBeforeSuppression]
     * times (reminder-fatigue suppression), unless a one-week snooze is active.
     * The explicit clock makes the policy deterministic and unit-testable.
     */
    fun show(
        profile: UserProfile?,
        skipCount: Int,
        snoozedUntilMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val hasUsableWeight = profile?.let { NutritionGuidelines.dailyTargets(it) != null } == true
        val snoozeActive = snoozedUntilMillis?.let { it > nowMillis } == true
        flow.value = if (hasUsableWeight && !snoozeActive && skipCount < maxSkipsBeforeSuppression) {
            NutritionGuidelines.preWorkoutTip()
        } else {
            null
        }
    }

    /** Dismiss the reminder. */
    fun clear() {
        flow.value = null
    }

    companion object {
        /** Skip this many times before the fuel-up dialog is suppressed for good. */
        const val DEFAULT_MAX_SKIPS = 3
        const val SNOOZE_DURATION_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}
