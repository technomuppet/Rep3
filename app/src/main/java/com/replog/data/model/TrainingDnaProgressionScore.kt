package com.replog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_dna_progression_scores",
    indices = [Index("exerciseId"), Index("calculatedAt")]
)
data class TrainingDnaProgressionScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseId: Int,
    val calculatedAt: Long,
    val score30Day: Double,
    val score90Day: Double,
    val scoreLifetime: Double,
    val estimatedOneRm30Day: Double,
    val estimatedOneRm90Day: Double,
    val estimatedOneRmLifetime: Double
)
