package com.replog.domain.trainingdna

import com.replog.data.model.TrainingDnaSnapshot

/**
 * Phase 5 — DNA interpretation layer.
 *
 * Pure presentation helper that translates an existing [TrainingDnaSnapshot]
 * (produced by the existing TrainingDnaEngine) into plain-English statements.
 * It introduces NO new analytics — every value comes straight from the snapshot.
 */
data class DnaInterpretation(
    val headline: String,
    val statements: List<DnaStatement>
)

data class DnaStatement(
    val prompt: String,   // e.g. "You respond best to:"
    val value: String,    // e.g. "4–6 reps"
    val detail: String? = null
)

object DnaInterpreter {

    fun interpret(snapshot: TrainingDnaSnapshot?): DnaInterpretation? {
        if (snapshot == null) return null

        val statements = mutableListOf<DnaStatement>()

        // Preferred rep range
        snapshot.preferredRepRange.cleaned()?.let {
            statements += DnaStatement(
                prompt = "You respond best to:",
                value = "$it reps",
                detail = "This is where you accumulate the most quality work."
            )
        }

        // Preferred frequency
        snapshot.preferredFrequency.cleaned()?.let {
            statements += DnaStatement(
                prompt = "You recover fastest when training:",
                value = it,
                detail = recoveryDetail(snapshot.averageRecoveryHours)
            )
        }

        // Volume tolerance (plain English from preferredVolumeRange + score)
        snapshot.preferredVolumeRange.cleaned()?.let {
            statements += DnaStatement(
                prompt = "Your volume tolerance is:",
                value = it,
                detail = volumeDetail(snapshot.volumeToleranceScore)
            )
        }

        // Most responsive muscle (strongest / fastest progressing)
        snapshot.strongestMuscles.firstCsv()?.let {
            statements += DnaStatement(
                prompt = "Most responsive muscle:",
                value = it.capitalizeWords(),
                detail = "It progresses fastest relative to the work you put in."
            )
        }

        // Fastest progressing lift
        snapshot.fastestProgressingExercises.firstCsv()?.let {
            statements += DnaStatement(
                prompt = "Fastest progressing lift:",
                value = it,
                detail = "Keep applying progressive overload here — it's working."
            )
        }

        // Slowest progressing / stalled lift
        snapshot.stalledExercises.firstCsv()?.let {
            statements += DnaStatement(
                prompt = "Slowest progressing lift:",
                value = it,
                detail = "Consider a variation, a deload, or a rep-range change to break the stall."
            )
        }

        // Weakest / undertrained muscle
        snapshot.weakestMuscles.firstCsv()?.let {
            statements += DnaStatement(
                prompt = "Needs more attention:",
                value = it.capitalizeWords(),
                detail = "Prioritise this area to keep your physique and strength balanced."
            )
        }

        if (statements.isEmpty()) return null

        return DnaInterpretation(
            headline = "What your DNA means",
            statements = statements
        )
    }

    private fun recoveryDetail(hours: Double): String = when {
        hours <= 0.0 -> "Based on the gaps between your sessions."
        hours < 36 -> "You bounce back quickly — short rest gaps suit you."
        hours < 72 -> "You recover at a typical pace; keep rest days consistent."
        else -> "You tend to need longer between hard sessions — don't rush it."
    }

    private fun volumeDetail(score: Double): String = when {
        score <= 0.0 -> "Built from how consistently you handle your weekly workload."
        score < 40 -> "Lower tolerance — quality over quantity works best for you."
        score < 70 -> "Moderate tolerance — steady, repeatable volume suits you."
        else -> "High tolerance — you handle and benefit from larger workloads."
    }

    private fun String?.cleaned(): String? =
        this?.trim()?.takeIf { it.isNotBlank() && !it.equals("Not enough data", true) && it != "—" }

    private fun String?.firstCsv(): String? =
        this?.split(",")?.map { it.trim() }?.firstOrNull { it.isNotBlank() }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
}
