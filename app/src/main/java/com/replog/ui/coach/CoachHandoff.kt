package com.replog.ui.coach

import com.replog.domain.recommendation.Recommendation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 3 — Smart Start hand-off.
 *
 * Carries the currently accepted [Recommendation] from the Smart Coach card
 * (Home) to the Active Workout screen, which live in separate navigation
 * destinations with separate ViewModels. Pure transport — no logic.
 */
@Singleton
class CoachHandoff @Inject constructor() {
    @Volatile
    var pending: Recommendation? = null
        private set

    fun set(recommendation: Recommendation) { pending = recommendation }
    fun consume(): Recommendation? = pending.also { pending = null }
}
