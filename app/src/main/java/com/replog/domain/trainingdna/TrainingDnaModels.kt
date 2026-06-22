package com.replog.domain.trainingdna

import com.replog.data.model.PlateauEvent
import com.replog.data.model.TrainingDnaProgressionScore
import com.replog.data.model.TrainingDnaSnapshot

data class TrainingDnaResult(
    val snapshot: TrainingDnaSnapshot,
    val progressionScores: List<TrainingDnaProgressionScore>,
    val plateauEvents: List<PlateauEvent>
)

data class TrainingDnaConfig(
    val plateauPeriodDays: Int = 30,
    val minSessionsForPlateau: Int = 3,
    val minSetsForDna: Int = 20,
    val minSessionsForDna: Int = 5,
    val nowMillis: Long = System.currentTimeMillis()
)

data class ExerciseProgression(
    val exerciseId: Int,
    val exerciseName: String,
    val score30Day: Double,
    val score90Day: Double,
    val scoreLifetime: Double,
    val estimatedOneRm30Day: Double,
    val estimatedOneRm90Day: Double,
    val estimatedOneRmLifetime: Double
)

data class MuscleStrength(
    val muscle: String,
    val score: Double,
    val totalVolume: Double,
    val estimatedOneRm: Double
)

data class PlateauDetection(
    val exerciseId: Int,
    val exerciseName: String,
    val periodDays: Int,
    val reasons: List<PlateauReason>,
    val lastLoad: Double,
    val lastReps: Int,
    val lastVolume: Double
)

enum class PlateauReason {
    NO_LOAD_INCREASE,
    NO_REP_INCREASE,
    NO_VOLUME_INCREASE;

    fun displayName(): String = when (this) {
        NO_LOAD_INCREASE -> "No load increase"
        NO_REP_INCREASE -> "No rep increase"
        NO_VOLUME_INCREASE -> "No volume increase"
    }
}
