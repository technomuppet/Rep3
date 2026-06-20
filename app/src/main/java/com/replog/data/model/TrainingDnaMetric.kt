package com.replog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_dna_metrics",
    indices = [
        Index("dimension"),
        Index("subjectType"),
        Index("subjectId"),
        Index(value = ["dimension", "subjectType", "subjectId"], unique = true)
    ]
)
data class TrainingDnaMetric(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dimension: String,
    val subjectType: String,
    val subjectId: String? = null,
    val value: Double,
    val confidence: Double,
    val sampleSize: Int,
    val updatedAt: Long = System.currentTimeMillis(),
    val metadataJson: String = "{}"
)
