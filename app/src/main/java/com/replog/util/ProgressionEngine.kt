package com.replog.util

import com.replog.data.model.SetLog
import com.replog.data.model.WorkoutPrescription

data class PrescriptionEvaluation(
    val hit: Boolean,
    val nextTargetSets: Int,
    val nextTargetReps: Int,
    val nextTargetWeight: Double?,
    val recommendation: String
)

object ProgressionEngine {
    fun evaluate(
        prescription: WorkoutPrescription,
        completedSets: List<SetLog>
    ): PrescriptionEvaluation {
        val hardSets = completedSets.filter { it.setType != "Warmup" }
        val targetWeight = prescription.targetWeight ?: 0.0
        val hit = hardSets.size >= prescription.targetSets &&
            hardSets.any { it.weight >= targetWeight && it.reps >= prescription.targetReps }
        val avgRpe = hardSets.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()
        val bestSet = hardSets.maxWithOrNull(compareBy<SetLog> { it.weight }.thenBy { it.reps })

        return when {
            hit && (avgRpe == null || avgRpe <= 7.5) -> {
                val nextWeight = (prescription.targetWeight ?: bestSet?.weight)?.let { it + jumpFor(it) }
                PrescriptionEvaluation(
                    hit = true,
                    nextTargetSets = prescription.targetSets,
                    nextTargetReps = (prescription.targetReps - 1).coerceAtLeast(3),
                    nextTargetWeight = nextWeight,
                    recommendation = "Target hit with room to spare. Increase load next time."
                )
            }
            hit && (avgRpe == null || avgRpe <= 9.0) -> PrescriptionEvaluation(
                hit = true,
                nextTargetSets = prescription.targetSets,
                nextTargetReps = prescription.targetReps + 1,
                nextTargetWeight = prescription.targetWeight,
                recommendation = "Target hit. Add one rep next time or keep load steady."
            )
            hit -> PrescriptionEvaluation(
                hit = true,
                nextTargetSets = prescription.targetSets,
                nextTargetReps = prescription.targetReps,
                nextTargetWeight = prescription.targetWeight,
                recommendation = "Target hit, but effort was high. Repeat once before progressing."
            )
            avgRpe != null && avgRpe >= 9.5 -> PrescriptionEvaluation(
                hit = false,
                nextTargetSets = (prescription.targetSets - 1).coerceAtLeast(1),
                nextTargetReps = prescription.targetReps,
                nextTargetWeight = prescription.targetWeight?.let { it * 0.975 },
                recommendation = "Target missed at very high effort. Reduce fatigue next time."
            )
            hardSets.size < prescription.targetSets -> PrescriptionEvaluation(
                hit = false,
                nextTargetSets = prescription.targetSets,
                nextTargetReps = prescription.targetReps,
                nextTargetWeight = prescription.targetWeight,
                recommendation = "Set target missed. Repeat the same prescription next time."
            )
            else -> PrescriptionEvaluation(
                hit = false,
                nextTargetSets = prescription.targetSets,
                nextTargetReps = prescription.targetReps,
                nextTargetWeight = prescription.targetWeight,
                recommendation = "Rep/load target missed. Repeat before increasing."
            )
        }
    }

    private fun jumpFor(weight: Double): Double = when {
        weight >= 180.0 -> 5.0
        weight >= 100.0 -> 2.5
        else -> 1.25
    }
}
