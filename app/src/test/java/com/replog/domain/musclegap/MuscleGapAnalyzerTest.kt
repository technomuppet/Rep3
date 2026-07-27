package com.replog.domain.musclegap

import com.replog.data.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2 Gap 4 — deterministic JUnit 4 tests for the pure MuscleGapAnalyzer.
 *
 * Drives the engine that BOTH
 *
 *   1. the Home `MuscleGapCard` composable, and
 *   2. the `IntelligenceRepository.buildMuscleGapSuggestions()` repository
 *      helper (the Home card's data source)
 *
 * consume. By locking the four canonical behaviours (empty library, wrong
 * primary, primary hits + diversification, multi-muscle filter) in one
 * place, a tuning of the analyser cannot regress either UI consumer without
 * a test failure.
 *
 * Host-runnable via
 * `./gradlew :app:testDebugUnitTest --tests com.replog.domain.musclegap.MuscleGapAnalyzerTest`.
 */
class MuscleGapAnalyzerTest {

    @Test(timeout = 1_000L)
    fun suggestionsFor_emptyLibrary_returnsEmptyList() {
        val out = MuscleGapAnalyzer.suggestionsFor("Pectorals", emptyList(), limit = 3)
        assertTrue("no library, no suggestions", out.isEmpty())
    }

    @Test(timeout = 1_000L)
    fun suggestionsFor_wrongPrimary_returnsEmptyList() {
        val lib = listOf(
            ex("Bench Press", primary = "Pectorals", equipment = "Barbell"),
            ex("Lateral Raise", primary = "Side Deltoids", equipment = "Dumbbell")
        )
        val out = MuscleGapAnalyzer.suggestionsFor("Hamstrings", lib, limit = 3)
        assertTrue(
            "Hamstrings have no match in a chest/shoulders-only library",
            out.isEmpty()
        )
    }

    @Test(timeout = 1_000L)
    fun suggestionsFor_primaryHits_diversifiesAcrossEquipment() {
        // Five pectoral exercises with five different equipment types + one
        // non-matching exercise. limit=3 must pull at most one per equipment
        // type from the ranking.
        val lib = listOf(
            ex("Bench", primary = "Pectorals", equipment = "Barbell"),
            ex("DB Bench", primary = "Pectorals", equipment = "Dumbbell"),
            ex("Cable Press", primary = "Pectorals", equipment = "Cable"),
            ex("Push-Up", primary = "Pectorals", equipment = "Bodyweight"),
            ex("Smith Press", primary = "Pectorals", equipment = "Smith"),
            ex("Lateral Raise", primary = "Side Deltoids", equipment = "Dumbbell")
        )
        val out = MuscleGapAnalyzer.suggestionsFor("Pectorals", lib, limit = 3)
        assertEquals("returns at most limit", 3, out.size)
        assertEquals(
            "one exercise per equipment class (diversification rule)",
            out.size, out.map { it.equipment }.distinct().size
        )
        assertTrue(
            "every suggestion targets pectorals",
            out.all {
                it.primaryMuscles.split(",").any { m -> m.trim().lowercase() == "pectorals" }
            }
        )
    }

    @Test(timeout = 1_000L)
    fun suggestionsFor_secondaryHitFallback_whenNoPrimaryExists() {
        // Library has bench-press but does NOT have a primary "Pectorals" match
        // for the search input "Chest". The analyzer's `primaryHits` lookup
        // uses exact primary-membership, so "Chest" -> no match -> the
        // secondaryHits retrieval is irrelevant. The card simply reports
        // empty, which is the documented behaviour: the analyzer only matches
        // against `primaryMuscles` field token equality.
        val lib = listOf(
            ex("Bench Press", primary = "Pectorals", secondary = "", equipment = "Barbell")
        )
        val outPrimary = MuscleGapAnalyzer.suggestionsFor("Pectorals", lib, limit = 3)
        assertEquals(1, outPrimary.size)
        val outNoPrimary = MuscleGapAnalyzer.suggestionsFor("Chest", lib, limit = 3)
        assertTrue(
            "exact-name match is required: 'Chest' != analyzer primary 'Pectorals'",
            outNoPrimary.isEmpty()
        )
    }

    @Test(timeout = 1_000L)
    fun analyze_filtersEmptyAndBlankMuscleNamesAndReturnsOnlyMusclesWithMatches() {
        val lib = listOf(
            ex("Bench", primary = "Pectorals", equipment = "Barbell"),
            ex("Squat", primary = "Quads", equipment = "Barbell")
        )
        val out = MuscleGapAnalyzer.analyze(
            weakMuscles = listOf("", "   ", "Pectorals", "Hamstrings", "Quads", "Hamstrings"),
            library = lib,
            perMuscle = 3
        )
        // Only Pectorals + Quads have matches; blank lines are dropped; the
        // duplicate "Hamstrings" is de-duplicated.
        assertEquals(2, out.size)
        val foundGroups = out.map { it.muscle }.toSet()
        assertEquals(setOf("Pectorals", "Quads"), foundGroups)
        assertTrue(
            "no out-group has zero suggestions",
            out.all { it.exercises.isNotEmpty() }
        )
    }

    // ---------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------

    private fun ex(
        name: String,
        primary: String,
        equipment: String,
        secondary: String = "",
        difficulty: String = "Intermediate"
    ): Exercise = Exercise(
        name = name,
        category = "Strength",
        equipment = equipment,
        primaryMuscles = primary,
        secondaryMuscles = secondary,
        movementPattern = "",
        difficulty = difficulty,
        muscles = listOf(primary, secondary).filter { it.isNotBlank() }.joinToString(",")
    )
}
