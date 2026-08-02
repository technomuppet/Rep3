package com.replog.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupersetRestPolicyTest {
    @Test
    fun normalExerciseStartsRestImmediately() {
        assertTrue(SupersetRestPolicy.shouldStartRest(null, 1, emptyList()))}


    @Test
    fun supersetWaitsUntilEveryMovementCompletesTheRound() {
        assertFalse(SupersetRestPolicy.shouldStartRest("A", 2, listOf(setOf(1, 2), setOf(1))))
        assertTrue(SupersetRestPolicy.shouldStartRest("A", 2, listOf(setOf(1, 2), setOf(1, 2))))}


    @Test
    fun groupsWithMoreThanTwoMovementsShareOneRest() {
        assertFalse(SupersetRestPolicy.shouldStartRest("B", 3, listOf(setOf(1, 2, 3), setOf(1, 2, 3), setOf(1, 2))))
        assertTrue(SupersetRestPolicy.shouldStartRest("B", 3, listOf(setOf(1, 2, 3), setOf(1, 2, 3), setOf(1, 2, 3))))}


    @Test
    fun extraSetOnOneMovementDoesNotCloseTheRound() {
        assertFalse(SupersetRestPolicy.shouldStartRest("A", 2, listOf(setOf(1, 2), setOf(1, 2, 3))))}


    @Test
    fun invalidSupersetRoundDoesNotStartRest() {
        assertFalse(SupersetRestPolicy.shouldStartRest("C", 0, listOf(emptySet(), emptySet())))
        assertFalse(SupersetRestPolicy.shouldStartRest("C", 1, emptyList()))}


    @Test
    fun missingSetNumberDoesNotMasqueradeAsCompletedRound() {
        assertFalse(SupersetRestPolicy.shouldStartRest("A", 2, listOf(setOf(1, 3), setOf(1, 2))))}


    @Test
    fun warmupsNormalizeBeforeWorkingRoundComparison() {
        assertEquals(setOf(1, 2), SupersetRestPolicy.completedRoundNumbers(listOf(
            SupersetSetProgress(setNumber = 1, isWarmup = true),
            SupersetSetProgress(setNumber = 2),
            SupersetSetProgress(setNumber = 3)
        )))
        assertTrue(SupersetRestPolicy.shouldStartRest("A", 1, listOf(setOf(1), setOf(1))))}


    @Test
    fun aDeletedOrSkippedRoundGapBlocksThatAndLaterRounds() {
        assertFalse(SupersetRestPolicy.shouldStartRest("A", 2, listOf(setOf(1, 3), setOf(1, 2))))
        assertFalse(SupersetRestPolicy.shouldStartRest("A", 3, listOf(setOf(1, 3), setOf(1, 2, 3))))}


    @Test
    fun outOfOrderCompletionStillClosesOnlyWhenTheRoundIsContiguous() {
        assertFalse(SupersetRestPolicy.shouldStartRest("A", 2, listOf(setOf(1, 2), setOf(2))))
        assertTrue(SupersetRestPolicy.shouldStartRest("A", 2, listOf(setOf(1, 2), setOf(1, 2))))}

}
