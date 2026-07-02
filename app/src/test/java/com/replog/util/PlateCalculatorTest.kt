package com.replog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {
    @Test
    fun calculatesMetricPlatesPerSide() {
        val load = PlateCalculator.calculate(targetWeight = 100.0, barWeight = 20.0, useKg = true)
        assertEquals(100.0, load.loadedWeight, 0.001)
        // 40 kg per side, greedy over [25,20,15,10,5,2.5,1.25] -> 25 + 15 (standard
        // greedy loading). The previous assertion [20,20] was incorrect: greedy will
        // always take the 25 first. Sum is still 40 kg per side.
        assertEquals(listOf(25.0, 15.0), load.platesPerSide)
        assertEquals(40.0, load.platesPerSide.sum(), 0.001)
    }

    @Test
    fun parsesCustomPlateInput() {
        val plates = PlateCalculator.parsePlates("25, 20 10; 2.5\n1.25")
        assertEquals(listOf(25.0, 20.0, 10.0, 2.5, 1.25), plates)
    }

    @Test
    fun customPlateCalculationDoesNotOverload() {
        val load = PlateCalculator.calculate(targetWeight = 103.0, barWeight = 20.0, useKg = true, customPlates = listOf(25.0, 10.0, 1.25))
        assertTrue(load.loadedWeight <= 103.0)
    }
}
