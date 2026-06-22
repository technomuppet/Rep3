package com.replog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [Index("startTime"), Index("endTime")]
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateName: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val notes: String? = null,
    val qualityScore: Int? = null,
    val totalVolume: Double = 0.0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val prCount: Int = 0
)
