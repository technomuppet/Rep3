package com.replog.domain.recovery

import com.replog.domain.recommendation.MuscleRecovery
import com.replog.domain.recommendation.MuscleRecoveryStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryGuidanceTest {
    private fun muscle(name: String, status: MuscleRecoveryStatus) = MuscleRecovery(
        muscle = name,
        recoveryScore = if (status == MuscleRecoveryStatus.FRESH) 90.0 else 30.0,
        lastTrainedMillis = 0L,
        volumeLast7Days = 0.0,
        status = status
    )

    @Test
    fun strongRecoverySuggestsProgressingOneVariable() {
        val result = RecoveryGuidance.build(
            score = 85,
            recovered = listOf(muscle("Chest", MuscleRecoveryStatus.FRESH)),
            fatigued = emptyList(),
            factors = listOf("Training load looks steady.")
        )

        assertTrue(result.improvements.any { it.contains("progress one variable") })
        assertTrue(result.improvements.any { it.contains("Chest") })
    }

    @Test
    fun lowRecoverySuggestsBackingOffAndNamesFatiguedMuscles() {
        val result = RecoveryGuidance.build(
            score = 35,
            recovered = emptyList(),
            fatigued = listOf(muscle("Quadriceps", MuscleRecoveryStatus.VERY_FATIGUED)),
            factors = listOf("Multiple high-RPE sets this week")
        )

        assertTrue(result.warnings.any { it.contains("rest, mobility, or an easy session") })
        assertTrue(result.warnings.any { it.contains("Quadriceps") })
        assertTrue(result.warnings.any { it.contains("Recent effort was high") })
    }

    @Test
    fun elevatedVolumeAddsNoExtraSetsWarning() {
        val result = RecoveryGuidance.build(
            score = 62,
            recovered = emptyList(),
            fatigued = emptyList(),
            factors = listOf("Weekly volume jumped 40%")
        )

        assertTrue(result.warnings.any { it.contains("avoid adding extra sets") })
    }
}
