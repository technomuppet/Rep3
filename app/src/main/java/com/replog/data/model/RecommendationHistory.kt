package com.replog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recommendation_history",
    indices = [Index("timestamp"), Index("recommendationType")]
)
data class RecommendationHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val recommendationType: String,
    val title: String,
    val explanation: String,
    val dataUsed: String,
    val reasoning: String,
    val expectedOutcome: String,
    val confidenceScore: Double,
    val estimatedDurationMinutes: Int,
    val workoutSplit: String?,
    val outcome: String? = null,
    val rejectedReason: String? = null
)
