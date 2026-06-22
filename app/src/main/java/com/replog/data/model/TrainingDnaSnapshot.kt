package com.replog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_dna_snapshots",
    indices = [Index("generatedAt")]
)
data class TrainingDnaSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val generatedAt: Long,
    val preferredRepRange: String,
    val preferredVolumeRange: String,
    val preferredFrequency: String,
    val strongestMuscles: String,
    val weakestMuscles: String,
    val fastestProgressingExercises: String,
    val stalledExercises: String,
    val averageWorkoutDuration: Double,
    val averageRecoveryHours: Double,
    val monthlyPRCount: Int,
    val volumeToleranceScore: Double
)
