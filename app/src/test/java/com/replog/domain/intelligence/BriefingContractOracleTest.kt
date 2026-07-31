package com.replog.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2.5 — Briefing contract oracle.
 *
 * Locks the deterministic ORDER of [TodaysBriefing.explainSections] before any
 * Phase 3 feature reshuffles cards. The golden order below matches what
 * [IntelligenceEngine.buildExplainSections] emits when every source engine
 * contributes a signal. Any code change that reorders, removes, or adds a
 * section title must update this golden — which serves as both documentation
 * and a compile-time-runnable contract guard.
 *
 * ## Why this test matters
 *
 * [IntelligenceRepository.buildBriefing] gathers signals from 7 engines and
 * feeds them into [IntelligenceEngine.build], which produces exactly one
 * ordered list of [ExplainSection]s. The Home screen renders these sections in
 * the order they appear — Recovery first (medical-adjacent, highest
 * determinism), Goal/Muscle Gap last (actionable, contextual). Reordering or
 * dropping a section silently changes the coaching UX without a type-checker
 * alert. This oracle catches that in milliseconds on `:app:testDebugUnitTest`.
 *
 * ## Golden fixture
 *
 * The golden is checked into this file as [GOLDEN_EXPLAIN_SECTION_ORDER].
 * It is the **complete, ordered** list of section titles the engine can
 * produce when every signal is present. Subset-preservation tests ensure
 * that when some signals are absent the remaining sections appear in the
 * same relative order — no signal can "jump" ahead of another.
 *
 * ## What this does NOT test (by design)
 *
 * - It does NOT drive [IntelligenceRepository] or Room — those require an
 *   Android instrumentation host. The repository's job is to populate
 *   [IntelligenceInputs]; this test assumes correct inputs and locks the
 *   output shape.
 * - It does NOT assert section *content* (line text, confidence values) —
 *   those are covered by [IntelligenceEngineTest]. This test is purely about
 *   ORDER and EXHAUSTIVENESS.
 */
class BriefingContractOracleTest {

    // -------------------------------------------------------------------------
    // Checked-in golden: the exact, ordered list of explainSection titles
    // the engine produces when every source engine contributes a signal.
    // -------------------------------------------------------------------------

    companion object {
        /**
         * Golden explain-section order — checked into source control.
         *
         * Derived from [IntelligenceEngine.buildExplainSections]:
         *  1. Recovery        (when recoveryScore != null)
         *  2. Training Genome (when genomeBestRepRange is not blank)
         *  3. Weekly Volume   (when underVolumeGroups is not empty)
         *  4. Progress Forecast (when topForecastLabel is not blank)
         *  5. Goal            (when goalSummary is not blank)
         *  6. Muscle Gap      (when neglectedMuscles is not empty)
         *
         * DO NOT reorder, insert, or delete entries without a deliberate
         * coaching-design decision. This golden IS the contract.
         */
        val GOLDEN_EXPLAIN_SECTION_ORDER: List<String> = listOf(
            "Recovery",
            "Training Genome",
            "Weekly Volume",
            "Progress Forecast",
            "Goal",
            "Muscle Gap"
        )
    }

    // -------------------------------------------------------------------------
    // Helpers — mirror the fixture factory from IntelligenceEngineTest.
    // -------------------------------------------------------------------------

    private fun inputs(
        hasEnoughData: Boolean = true,
        recoveryScore: Int? = 78,
        readyMuscleGroups: List<String> = emptyList(),
        fatiguedMuscleGroups: List<String> = emptyList(),
        isRestRecommended: Boolean = false,
        recommendedFocus: String? = null,
        genomeBestRepRange: String? = null,
        underVolumeGroups: List<String> = emptyList(),
        neglectedMuscles: List<String> = emptyList(),
        topForecastLabel: String? = null,
        topForecastConfidenceHigh: Boolean = false,
        goalSummary: String? = null,
        recoveryStatusLabel: String? = null,
        recoveryDirective: String? = null,
        recoveryFactors: List<String> = emptyList(),
        strongestDayOfWeek: String? = null,
        prsAfterRestDays: Int? = null,
        slowRecoveryAfterHighVolume: Boolean = false,
        hourOfDay: Int = 9
    ) = IntelligenceInputs(
        hasEnoughData = hasEnoughData,
        recoveryScore = recoveryScore,
        recoveryStatusLabel = recoveryStatusLabel,
        recoveryDirective = recoveryDirective,
        recoveryFactors = recoveryFactors,
        isRestRecommended = isRestRecommended,
        readyMuscleGroups = readyMuscleGroups,
        fatiguedMuscleGroups = fatiguedMuscleGroups,
        recommendedFocus = recommendedFocus,
        genomeBestRepRange = genomeBestRepRange,
        underVolumeGroups = underVolumeGroups,
        neglectedMuscles = neglectedMuscles,
        topForecastLabel = topForecastLabel,
        topForecastConfidenceHigh = topForecastConfidenceHigh,
        goalSummary = goalSummary,
        strongestDayOfWeek = strongestDayOfWeek,
        prsAfterRestDays = prsAfterRestDays,
        slowRecoveryAfterHighVolume = slowRecoveryAfterHighVolume,
        hourOfDay = hourOfDay
    )

    // -------------------------------------------------------------------------
    // Test 1 — all signals present → golden order MATCHES production exactly.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun `fullSignalBriefing produces all six sections in golden order`() {
        val b = IntelligenceEngine.build(
            inputs(
                recoveryScore = 82,
                recoveryFactors = listOf("Sleep 8h", "Stress low"),
                readyMuscleGroups = listOf("Chest", "Triceps"),
                recommendedFocus = "Upper Push Strength",
                genomeBestRepRange = "6-8 reps",
                underVolumeGroups = listOf("Hamstrings"),
                neglectedMuscles = listOf("Rear Delts", "Calves"),
                topForecastLabel = "Bench +2.5kg in 4 wks",
                topForecastConfidenceHigh = true,
                goalSummary = "Hit 100kg bench in ~9 wks",
                hourOfDay = 9
            )
        )

        val actualTitles = b.explainSections.map { it.title }
        assertEquals(
            "explainSections order must match golden when all signals present",
            GOLDEN_EXPLAIN_SECTION_ORDER,
            actualTitles
        )
        assertEquals(
            "all 6 sections must be present when every engine contributes",
            6,
            b.explainSections.size
        )
    }

    // -------------------------------------------------------------------------
    // Test 2 — partial signals preserve relative golden order.
    //
    // When only Recovery, Goal, and Muscle Gap signals exist, the engine must
    // emit them in the same relative order they occupy in the golden — no
    // reordering, no signal "jumping" ahead.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun `partialSignals preserve golden relative order`() {
        val b = IntelligenceEngine.build(
            inputs(
                recoveryScore = 65,
                recoveryFactors = listOf("Sleep 6h"),
                // NO genomeBestRepRange — omit Training Genome
                // NO underVolumeGroups — omit Weekly Volume
                // NO topForecastLabel — omit Progress Forecast
                goalSummary = "Lose 5kg in 12 weeks",
                neglectedMuscles = listOf("Rear Delts")
            )
        )

        val actualTitles = b.explainSections.map { it.title }
        val expected = listOf("Recovery", "Goal", "Muscle Gap")

        assertEquals(
            "partial signals must appear in golden relative order",
            expected,
            actualTitles
        )
        assertEquals(
            "only 3 sections when 3 signals present",
            3,
            b.explainSections.size
        )
    }

    // -------------------------------------------------------------------------
    // Test 3 — single signal (Recovery-only) produces a single section.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun `singleSignal produces single section`() {
        val b = IntelligenceEngine.build(
            inputs(
                recoveryScore = 50,
                recoveryFactors = listOf("Sleep 7h")
                // All other signals absent
            )
        )

        assertEquals(
            "single signal must produce exactly one section",
            listOf("Recovery"),
            b.explainSections.map { it.title }
        )
        assertEquals(1, b.explainSections.size)
    }

    // -------------------------------------------------------------------------
    // Test 4 — no explainable signals → empty explainSections list.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun `noExplainableSignals produces empty explainSections`() {
        // recoveryScore = null → no Recovery section
        // No other signals set.
        val b = IntelligenceEngine.build(
            inputs(
                recoveryScore = null,
                recoveryFactors = emptyList()
            )
        )

        assertTrue(
            "explainSections must be empty when no explainable signal exists",
            b.explainSections.isEmpty()
        )
    }

    // -------------------------------------------------------------------------
    // Test 5 — golden exhaustiveness.
    //
    // The golden list IS the complete set of sections the engine can produce.
    // This test verifies that every title in the golden matches the actual
    // buildExplainSections logic (no stale/misspelled entries) AND that no
    // undocumented section can appear (every produced title must be in golden).
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun `golden is exhaustive and matches production`() {
        // Produce a briefing with EVERY signal active.
        val b = IntelligenceEngine.build(
            inputs(
                recoveryScore = 82,
                recoveryFactors = listOf("Sleep 8h"),
                readyMuscleGroups = listOf("Chest"),
                genomeBestRepRange = "6-8 reps",
                underVolumeGroups = listOf("Quads"),
                neglectedMuscles = listOf("Calves"),
                topForecastLabel = "Deadlift +5kg in 3 wks",
                topForecastConfidenceHigh = true,
                goalSummary = "Squat 140kg in 8 wks",
                hourOfDay = 9
            )
        )

        val actualTitles = b.explainSections.map { it.title }.toSet()
        val goldenSet = GOLDEN_EXPLAIN_SECTION_ORDER.toSet()

        // Every golden title must appear in production (golden is not stale).
        val goldenNotProduced = goldenSet - actualTitles
        assertTrue(
            "golden titles not produced by engine: $goldenNotProduced",
            goldenNotProduced.isEmpty()
        )

        // Every produced title must be in the golden (no undocumented section).
        val producedNotInGolden = actualTitles - goldenSet
        assertTrue(
            "engine produced sections not in golden: $producedNotInGolden",
            producedNotInGolden.isEmpty()
        )

        // Sanity: golden has exactly 6 entries (no duplicates, no missing).
        assertEquals(
            "golden must have exactly 6 section titles",
            6,
            GOLDEN_EXPLAIN_SECTION_ORDER.size
        )
        assertEquals(
            "golden must have exactly 6 unique titles (no duplicates)",
            6,
            goldenSet.size
        )
    }
}
