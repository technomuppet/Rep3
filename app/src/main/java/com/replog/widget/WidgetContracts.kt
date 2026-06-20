package com.replog.widget

/**
 * Architecture-only widget contract for future Android home-screen widgets.
 *
 * This file intentionally does not register a widget provider yet. It defines stable actions and
 * state DTOs so Phase 1 can design the widget boundary without introducing UI/runtime risk.
 */
object RepLogWidgetActions {
    const val ACTION_QUICK_START_EMPTY = "com.replog.widget.action.QUICK_START_EMPTY"
    const val ACTION_OPEN_TODAYS_WORKOUT = "com.replog.widget.action.OPEN_TODAYS_WORKOUT"
    const val ACTION_OPEN_ACTIVE_WORKOUT = "com.replog.widget.action.OPEN_ACTIVE_WORKOUT"
}

data class QuickStartWidgetState(
    val hasActiveWorkout: Boolean,
    val activeWorkoutName: String?,
    val suggestedWorkoutName: String?
)

data class TodaysWorkoutWidgetState(
    val title: String,
    val subtitle: String,
    val targetCount: Int,
    val isAdaptive: Boolean
)
