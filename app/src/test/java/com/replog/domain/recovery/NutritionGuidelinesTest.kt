package com.replog.domain.recovery

import com.replog.util.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // ----- Structured daily targets (new API) -----

    @Test
    fun dailyTargetsForHypertrophyIncludeCarbsAndFat() {
        val profile = UserProfile("Athlete", weightKg = 80.0, primaryGoal = "HYPERTROPHY")
        val t = NutritionGuidelines.dailyTargets(profile)

        assertNotNull(t)
        assertEquals(128, t!!.proteinMinGrams)   // 1.6 g/kg
        assertEquals(176, t.proteinMaxGrams)     // 2.2 g/kg
        assertEquals(320, t.carbGrams)           // 4.0 g/kg
        assertEquals(80, t.fatGrams)             // 1.0 g/kg
        assertEquals(2.6, t.waterLitres, 0.001)
    }

    @Test
    fun dailyTargetsForFatLossUseHigherProteinToSpareLeanMass() {
        val profile = UserProfile("Athlete", weightKg = 60.0, primaryGoal = "FAT_LOSS")
        val t = NutritionGuidelines.dailyTargets(profile)

        assertNotNull(t)
        assertEquals(120, t!!.proteinMinGrams)   // 2.0 g/kg
        assertEquals(144, t.proteinMaxGrams)     // 2.4 g/kg
        assertEquals(150, t.carbGrams)           // 2.5 g/kg
    }

    @Test
    fun longSessionAddsExtraHydration() {
        val profile = UserProfile("Athlete", weightKg = 80.0)
        val base = NutritionGuidelines.dailyTargets(profile)!!
        val long = NutritionGuidelines.dailyTargets(profile, workoutDurationMinutes = 75)!!
        val veryLong = NutritionGuidelines.dailyTargets(profile, workoutDurationMinutes = 100)!!

        assertEquals(base.waterLitres + 0.5, long.waterLitres, 0.001)
        assertEquals(base.waterLitres + 1.0, veryLong.waterLitres, 0.001)
    }

    @Test
    fun missingWeightYieldsNoStructuredGuidance() {
        assertNull(NutritionGuidelines.dailyTargets(UserProfile("Athlete")))
        assertNull(NutritionGuidelines.dailyGuidance(UserProfile("Athlete")))
        assertNull(NutritionGuidelines.postWorkoutTip(UserProfile("Athlete")))
    }

    @Test
    fun guidanceIncludesSummaryAndTimingTips() {
        val profile = UserProfile("Athlete", weightKg = 80.0)
        val g = NutritionGuidelines.dailyGuidance(profile, workoutDurationMinutes = 75)

        assertNotNull(g)
        assertTrue(g!!.summary.contains("protein"))
        assertTrue(g.summary.contains("carbs"))
        assertTrue(g.summary.contains("fat"))
        assertNotNull(g.preWorkoutTip)
        assertNotNull(g.postWorkoutTip)
    }

    @Test
    fun preWorkoutTipAddsMidWorkoutCarbHintForLongSessions() {
        val base = NutritionGuidelines.preWorkoutTip()
        val long = NutritionGuidelines.preWorkoutTip(workoutDurationMinutes = 100)

        assertTrue(base.isNotBlank())
        assertTrue(long.contains("mid-workout"))
    }
}
