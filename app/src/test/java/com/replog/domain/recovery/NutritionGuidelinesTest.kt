package com.replog.domain.recovery

import com.replog.util.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionGuidelinesTest {

    @Test
    fun missingWeightDoesNotProduceAnEstimate() {
        assertNull(NutritionGuidelines.recoveryGuideline(UserProfile("Athlete")))
    }

    @Test
    fun implausibleWeightDoesNotProduceAnEstimate() {
        assertNull(NutritionGuidelines.recoveryGuideline(UserProfile("Athlete", weightKg = 25.0)))
        assertNull(NutritionGuidelines.recoveryGuideline(UserProfile("Athlete", weightKg = 350.0)))
    }

    @Test
    fun hypertrophyProfileGetsProteinAndHydrationGuideline() {
        val profile = UserProfile("Athlete", weightKg = 80.0, primaryGoal = "HYPERTROPHY")

        assertEquals(
            "To support muscle repair, consider roughly 128-176g of protein and ~2.6L of water today.",
            NutritionGuidelines.recoveryGuideline(profile)
        )
    }

    @Test
    fun fatLossProfileUsesRecoveryContextAndMinimumHydration() {
        val profile = UserProfile("Athlete", weightKg = 60.0, primaryGoal = "FAT_LOSS")

        assertEquals(
            "To support recovery while preserving lean mass, consider roughly 96-132g of protein and ~2.0L of water today.",
            NutritionGuidelines.recoveryGuideline(profile)
        )
    }

    @Test
    fun unknownGoalStillGetsNeutralGuideline() {
        val profile = UserProfile("Athlete", weightKg = 75.0)

        assertEquals(
            "To support daily recovery, consider roughly 120-165g of protein and ~2.5L of water today.",
            NutritionGuidelines.recoveryGuideline(profile)
        )
    }
}
