package com.replog.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2 Gap 1 — deterministic JUnit 4 unit tests for the PURE [IntelligenceEngine].
 *
 * The engine has no Android or Hilt dependencies and performs no I/O, so every
 * test constructs an [IntelligenceInputs] directly and asserts on the
 * returned [TodaysBriefing]. These tests cover the explainability / determinism
 * guarantees documented for the V3 brief:
 *
 *  - Priority 3: every recommendation has per-engine "Why?" sections with
 *    their own confidence, only emitted when the underlying signal exists.
 *  - Priority 4: the narrative is deterministic and never fabricates — every
 *    line traces to an input field. Confidence rises strictly with independent
 *    signals.
 *  - Priority 6: coach insights are only emitted when the data-backed source
 *    exists and tagged with the source for transparency.
 *
 * They run on `kotlinc 1.9.22` standalone AND on `:app:testDebugUnitTest`.
 */
class IntelligenceEngineTest {

    // -------------------------------------------------------------------------
    // Helpers — keep test inputs short and intent-revealing.
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
    // Priority 4 / 6 — empty data → fallback briefing, low confidence, no
    // fabricated insights.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun whenNotEnoughData_returnsFallbackBriefing_withLowConfidenceAndNoFabricatedInsights() {
        // Construct inputs as if the engine MIGHT have produced a rich briefing
        // — none of that should leak through when hasEnoughData is false.
        val b = IntelligenceEngine.build(
            inputs(
                hasEnoughData = false,
                recoveryScore = null,
                recoveryFactors = emptyList(),
                readyMuscleGroups = listOf("Chest"),          // should be ignored
                recommendedFocus = "Upper Push Strength",   // should be ignored
                genomeBestRepRange = "6-8 reps",             // should be ignored
                topForecastLabel = "Bench +2.5kg in 4 wks",  // should be ignored
                topForecastConfidenceHigh = true,
                goalSummary = "Hit 100kg bench in ~9 wks"    // should be ignored
            )
        )

        // The fallback recommendation is the only shape returned when data is
        // insufficient. No other field override should win.
        assertEquals("Log a few more workouts", b.recommendation)
        assertEquals(BriefingConfidence.LOW, b.confidence)
        assertEquals(1, b.reasons.size)
        assertTrue(
            "fallback reason must mention training history: '${b.reasons.single()}'",
            b.reasons.single().contains("training history", ignoreCase = true)
        )
        // No coach insights are invented from absent data.
        assertTrue("no fabricated coach insights", b.coachInsights.isEmpty())
        // Recovery score must NOT appear in the briefing when absent.
        assertFalse(
            "narrative must not invent a recovery score",
            b.narrative.any { it.contains("Recovery is", ignoreCase = true) }
        )
        assertFalse(
            "reasons must not invent a recovery score",
            b.reasons.any { it.contains("Recovery is", ignoreCase = true) }
        )
    }

    // -------------------------------------------------------------------------
    // Priority 1 / 3 — rest recommendation short-circuits the targeted focus
    // and the per-section "Recovery" section uses HIGH confidence because the
    // recoveryScore is present.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun whenRestRecommended_overridesTargetedFocusAndNarrativeReferencesRest() {
        val b = IntelligenceEngine.build(
            inputs(
                recoveryScore = 30,
                recoveryDirective = "Train light",
                recoveryFactors = listOf("Sleep 6h", "Stress high"),
                isRestRecommended = true,
                // These three are present so other signals can still surface in
                // reasons / explainSections, but the recommendation must NOT
                // promote any of them above the rest directive.
                readyMuscleGroups = listOf("Chest", "Triceps"),
                recommendedFocus = "Upper Push Strength",
                underVolumeGroups = listOf("Hamstrings"),
                genomeBestRepRange = "6-8 reps",
                topForecastLabel = "Bench +2.5kg in 4 wks",
                topForecastConfidenceHigh = true,
                hourOfDay = 8
            )
        )

        assertEquals("Rest or light recovery", b.recommendation)
        assertFalse(
            "recommendation must not echo the targeted focus",
            b.recommendation == "Upper Push Strength"
        )

        // Narrative must mention the rest directive (not a workout).
        assertTrue(
            "narrative must reference rest: '${b.narrative.joinToString(" | ")}'",
            b.narrative.any { it.contains("rest", ignoreCase = true) }
        )
        // The targeted focus must NOT appear as a workout recommendation line.
        assertFalse(
            "narrative must not propose 'Upper Push Strength' as today's workout",
            b.narrative.any { it.contains("Upper Push Strength") }
        )

        // Recovery is presented as HIGH confidence because recoveryScore != null.
        val recoverySection = b.explainSections.firstOrNull { it.title == "Recovery" }
        assertTrue("Recovery section must exist", recoverySection != null)
        assertEquals(BriefingConfidence.HIGH, recoverySection!!.confidence)
        // Recovery factors appear in the section (top 3).
        assertTrue(
            "Recovery factors must be surfaced",
            recoverySection.lines.contains("Sleep 6h") && recoverySection.lines.contains("Stress high")
        )
    }

    // -------------------------------------------------------------------------
    // Priority 5 — confidence rises with independent signals; all 6
    // explainSections appear when every source engine contributes.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun whenMultipleIndependentSignalsAgree_returnsHighConfidenceBriefingAndDetailedExplainSections() {
        // 6 independent signals → confidence HIGH (signals counter
        // recoveryScore(1) + ready(1) + underVolume(1) + topForecastHigh(2) +
        // repRange(1) = 6, threshold for HIGH is >= 4).
        val b = IntelligenceEngine.build(
            inputs(
                recoveryScore = 82,
                recoveryStatusLabel = "Recovered",
                recoveryDirective = "Train hard",
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

        assertEquals(BriefingConfidence.HIGH, b.confidence)

        // All 6 explain sections surface because every source is present.
        val titles = b.explainSections.map { it.title }.toSet()
        assertEquals(
            "all 6 per-engine sections must appear",
            setOf("Recovery", "Training Genome", "Weekly Volume", "Progress Forecast", "Goal", "Muscle Gap"),
            titles
        )

        // Recovery is HIGH because the recoveryScore is present.
        assertEquals(
            BriefingConfidence.HIGH,
            b.explainSections.first { it.title == "Recovery" }.confidence
        )
        // Progress Forecast is HIGH because topForecastConfidenceHigh = true.
        assertEquals(
            BriefingConfidence.HIGH,
            b.explainSections.first { it.title == "Progress Forecast" }.confidence
        )
        // Genome / Volume / Goal / Muscle Gap default to MEDIUM.
        listOf("Training Genome", "Weekly Volume", "Goal", "Muscle Gap").forEach { title ->
            assertEquals(
                "$title should be MEDIUM",
                BriefingConfidence.MEDIUM,
                b.explainSections.first { it.title == title }.confidence
            )
        }

        // Coach insight for "genome:repRange" fires because repRange is present.
        assertTrue(
            "genome rep-range insight must be emitted",
            b.coachInsights.any { it.source == "genome:repRange" }
        )
        // Top forecast and goal are surfaced verbatim in reasons.
        assertTrue(
            "top forecast appears in reasons",
            b.reasons.any { it.startsWith("Projected:") && it.contains("Bench") }
        )
        assertTrue(
            "goal summary appears in reasons",
            b.reasons.any { it.contains("100kg bench") }
        )
    }

    // -------------------------------------------------------------------------
    // Priority 4 / 6 — when only a few signals are present, the engine must
    // NOT fabricate references to absent fields in either `reasons` or
    // `narrative`. Coach insights only fire for fields that are populated.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun whenFatigueAndUnderVolumeOverlap_narrativeAndReasonsNeverFabricate() {
        // Only three signals given: recoveryScore, fatigue, underVolume. Genome,
        // topForecast, goalSummary, recommendedFocus are all null/empty.
        val b = IntelligenceEngine.build(
            inputs(
                recoveryScore = 55,
                recoveryStatusLabel = "Moderate",
                recoveryDirective = "Light session OK",
                recoveryFactors = listOf("Recent leg volume high"),
                readyMuscleGroups = emptyList(),
                fatiguedMuscleGroups = listOf("Hamstrings", "Calves"),
                isRestRecommended = false,
                recommendedFocus = "Upper Push Strength",
                underVolumeGroups = listOf("Hamstrings"),
                genomeBestRepRange = null,
                topForecastLabel = null,
                topForecastConfidenceHigh = false,
                goalSummary = null,
                strongestDayOfWeek = null,
                prsAfterRestDays = null,
                slowRecoveryAfterHighVolume = false,
                hourOfDay = 14
            )
        )

        // ----- Reasons must trace ONLY to populated fields -----
        assertEquals(
            "exactly two reasons should be produced (fatigue + underVolume)",
            2,
            b.reasons.size
        )
        assertTrue(
            "fatigue line must be present",
            b.reasons.any { it.contains("Hamstrings", ignoreCase = true) && it.contains("fatigue", ignoreCase = true) }
        )
        assertTrue(
            "under-volume line must be present",
            b.reasons.any { it.contains("Hamstrings", ignoreCase = true) && it.contains("below", ignoreCase = true) }
        )

        // ----- Narrative must NOT reference absent fields. -----
        // Forbidden tokens (fields that were not populated).
        val forbidden = listOf("Bench", "Genome", "rep range", "recover fastest", "strongest", "PB", "goal")
        b.narrative.forEach { line ->
            forbidden.forEach { bad ->
                assertFalse(
                    "narrative fabrication: '$line' must not mention absent field '$bad'",
                    line.contains(bad, ignoreCase = true)
                )
            }
        }

        // ----- Positive assertions on narrative content. -----
        // Recovery score line MUST be present (recoveryScore != null).
        assertTrue(
            "narrative must include 'Recovery is 55%.'",
            b.narrative.any { it.contains("Recovery is 55%") }
        )
        // Time-of-day greeting is deterministic for 14:00 = "Good afternoon."
        assertTrue(
            "narrative greeting must reflect hourOfDay = 14",
            b.narrative.first() == "Good afternoon."
        )
        // Fatigue narrative line uses capitalised muscle name.
        assertTrue(
            "fatigue narrative line must be present and well-formed",
            b.narrative.any { it.startsWith("Hamstrings") && it.contains("recovery remains incomplete", ignoreCase = true) }
        )
        // Under-volume narrative line uses lowercase muscle name.
        assertTrue(
            "under-volume narrative line must be present and well-formed",
            b.narrative.any { it.contains("hamstrings") && it.contains("below your optimal range", ignoreCase = true) }
        )
        // Confidence footer reflects the medium signal count (3 →
        // MEDIUM: recoveryScore(1) + fatigued(1) + underVolume(1) = 3).
        assertTrue(
            "narrative confidence footer must read 'Confidence: Medium.'",
            b.narrative.last() == "Confidence: Medium."
        )

        // ----- Coach insights — none of the absent sources fire. -----
        val sources = b.coachInsights.map { it.source }.toSet()
        assertEquals(
            "no coach insight should fire when genome/days/muscleGap data is absent",
            emptySet<String>(),
            sources
        )

        // ----- Explain sections — Recovery + Weekly Volume appear
        // (underVolumeGroups = ["Hamstrings"] is non-empty). -----
        assertEquals(
            listOf("Recovery", "Weekly Volume"),
            b.explainSections.map { it.title }
        )
    }

    // -------------------------------------------------------------------------
    // confidenceFor — signal-threshold transitions 0/1/2/3/4 →
    // LOW / LOW / MEDIUM / MEDIUM / HIGH. The signal counter increments per
    // independent signal in IntelligenceEngine.confidenceFor (HIGH = >= 4,
    // MEDIUM = >= 2, else LOW). The signals=0 case passes
    // `recoveryScore = null` explicitly to defeat the helper default of 78.
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun whenConfidenceFor_ranksSignalsCorrectlyAcrossThresholds() {
        // signals = 0 → LOW. All participating fields reset to null/empty.
        val b0 = IntelligenceEngine.build(
            inputs(
                recoveryScore = null,
                readyMuscleGroups = emptyList(),
                fatiguedMuscleGroups = emptyList(),
                underVolumeGroups = emptyList(),
                topForecastLabel = null,
                topForecastConfidenceHigh = false,
                genomeBestRepRange = null
            )
        )
        assertEquals(BriefingConfidence.LOW, b0.confidence)

        // signals = 1 → LOW (only recoveryScore present).
        val b1 = IntelligenceEngine.build(
            inputs(recoveryScore = 80)
        )
        assertEquals(BriefingConfidence.LOW, b1.confidence)

        // signals = 2 → MEDIUM (recoveryScore + ready groups).
        val b2 = IntelligenceEngine.build(
            inputs(
                recoveryScore = 80,
                readyMuscleGroups = listOf("Chest")
            )
        )
        assertEquals(BriefingConfidence.MEDIUM, b2.confidence)

        // signals = 3 → MEDIUM (above + underVolume).
        val b3 = IntelligenceEngine.build(
            inputs(
                recoveryScore = 80,
                readyMuscleGroups = listOf("Chest"),
                underVolumeGroups = listOf("Hamstrings")
            )
        )
        assertEquals(BriefingConfidence.MEDIUM, b3.confidence)

        // signals = 4 → HIGH (above + genome rep range; HIGH threshold is ">= 4").
        val b4 = IntelligenceEngine.build(
            inputs(
                recoveryScore = 80,
                readyMuscleGroups = listOf("Chest"),
                underVolumeGroups = listOf("Hamstrings"),
                genomeBestRepRange = "6-8 reps"
            )
        )
        assertEquals(BriefingConfidence.HIGH, b4.confidence)
    }

    // -------------------------------------------------------------------------
    // joinHuman grammar — exercised through the narrative ready-group line.
    // The narrative lowercases items before joining, so we input capitalised
    // muscle names and assert on the lowercased joined output. Three counts
    // exercise the three branches in IntelligenceEngine.joinHuman:
    //   size == 1 → "${items[0]}" (singular verb "has")
    //   size == 2 → "${items[0]} and ${items[1]}" (plural verb "have")
    //   size >= 3 → items.dropLast(1).joinToString(", ") + " and " + items.last()
    //               (Oxford-less comma branch, plural verb "have")
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun whenNarrativeJoinsMuscleGroups_correctGrammarForOneTwoAndMany() {
        // 1 item — singular verb "has".
        val b1i = IntelligenceEngine.build(
            inputs(readyMuscleGroups = listOf("Chest"))
        )
        assertTrue(
            "1-item join: '${b1i.narrative.joinToString(" | ")}'",
            b1i.narrative.any { it == "Your chest has fully recovered." }
        )

        // 2 items — "and" joiner, plural "have".
        val b2i = IntelligenceEngine.build(
            inputs(readyMuscleGroups = listOf("Chest", "Triceps"))
        )
        assertTrue(
            "2-item join: '${b2i.narrative.joinToString(" | ")}'",
            b2i.narrative.any { it == "Your chest and triceps have fully recovered." }
        )

        // 3 items — Oxford-less comma branch ("a, b and c"), plural "have".
        val b3i = IntelligenceEngine.build(
            inputs(readyMuscleGroups = listOf("Chest", "Triceps", "Quads"))
        )
        assertTrue(
            "3-item join: '${b3i.narrative.joinToString(" | ")}'",
            b3i.narrative.any { it == "Your chest, triceps and quads have fully recovered." }
        )
    }

    // -------------------------------------------------------------------------
    // Recommendation fallback — when no rest, no targeted focus, and no ready
    // groups, the recommendation must default to "Balanced full-body session"
    // and the narrative workout line must reflect that exact text. This is the
    // last branch of the `when` in IntelligenceEngine.build().
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun whenNoFocusNoReadyAndNoRest_returnsBalancedFullBodyFallback() {
        val b = IntelligenceEngine.build(
            inputs(
                recoveryScore = 80,
                recommendedFocus = null,
                readyMuscleGroups = emptyList(),
                isRestRecommended = false
            )
        )

        // The fallback recommendation wins when every earlier branch is dead.
        assertEquals("Balanced full-body session", b.recommendation)
        // The narrative workout line interpolates the chosen recommendation.
        assertTrue(
            "narrative must surface the fallback text: '${b.narrative.joinToString(" | ")}'",
            b.narrative.any {
                it == "Today is an excellent day for a Balanced full-body session workout."
            }
        )
    }

    // -------------------------------------------------------------------------
    // timeGreeting — boundary sweep across the three ranges defined in
    // IntelligenceEngine.timeGreeting(): 0..11 morning, 12..16 afternoon,
    // 17..23 evening. We pass `recoveryScore = null` so the narrative stays
    // minimal and the greeting is unambiguously the first line; this also
    // exercises the narrative-non-empty guarantee (confidence footer always
    // appended, so .first() is safe even with no other inputs).
    // -------------------------------------------------------------------------

    @Test(timeout = 1_000L)
    fun whenTimeGreeting_returnsCorrectGreetingAtEveryBoundary() {
        // 0..11 → "Good morning." (lower boundary, mid-range, upper boundary).
        listOf(0, 8, 11).forEach { hour ->
            val b = IntelligenceEngine.build(
                inputs(recoveryScore = null, hourOfDay = hour)
            )
            assertEquals(
                "hour=$hour must produce 'Good morning.'",
                "Good morning.",
                b.narrative.first()
            )
        }
        // 12..16 → "Good afternoon." (lower boundary, mid-range, upper boundary).
        listOf(12, 14, 16).forEach { hour ->
            val b = IntelligenceEngine.build(
                inputs(recoveryScore = null, hourOfDay = hour)
            )
            assertEquals(
                "hour=$hour must produce 'Good afternoon.'",
                "Good afternoon.",
                b.narrative.first()
            )
        }
        // 17..23 → "Good evening." (lower boundary, mid-range, upper boundary).
        listOf(17, 20, 23).forEach { hour ->
            val b = IntelligenceEngine.build(
                inputs(recoveryScore = null, hourOfDay = hour)
            )
            assertEquals(
                "hour=$hour must produce 'Good evening.'",
                "Good evening.",
                b.narrative.first()
            )
        }
    }
}
