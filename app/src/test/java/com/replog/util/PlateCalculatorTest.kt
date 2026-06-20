package com.replog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {
    @Test
    fun calculatesMetricPlatesPerSide() {
        val load = PlateCalculator.calculate(targetWeight = 100.0, barWeight = 20.0, useKg = true)
        assertEquals(100.0, load.loadedWeight, 0.001)
        assertEquals(listOf(20.0, 20.0), load.platesPerSide)
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
