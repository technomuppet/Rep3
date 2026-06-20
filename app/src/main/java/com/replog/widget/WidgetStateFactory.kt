package com.replog.widget

import com.replog.data.model.SessionWithExercises
import com.replog.util.AdaptiveWorkoutPlan

object WidgetStateFactory {
    fun quickStartState(
        activeSession: SessionWithExercises?,
        adaptivePlan: AdaptiveWorkoutPlan?
    ): QuickStartWidgetState = QuickStartWidgetState(
        hasActiveWorkout = activeSession != null,
        activeWorkoutName = activeSession?.session?.templateName ?: activeSession?.let { "Active workout" },
        suggestedWorkoutName = adaptivePlan?.title
    )

    fun todaysWorkoutState(adaptivePlan: AdaptiveWorkoutPlan?): TodaysWorkoutWidgetState = TodaysWorkoutWidgetState(
        title = adaptivePlan?.title ?: "Start workout",
        subtitle = adaptivePlan?.summary ?: "Log your next session in RepLog",
        targetCount = adaptivePlan?.targets?.size ?: 0,
        isAdaptive = adaptivePlan != null
    )
}
