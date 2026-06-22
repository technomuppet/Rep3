package com.replog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plateau_events",
    indices = [Index("exerciseId"), Index("detectedAt")]
)
data class PlateauEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseId: Int,
    val exerciseName: String,
    val detectedAt: Long,
    val periodDays: Int,
    val reason: String,
    val lastLoad: Double,
    val lastReps: Int,
    val lastVolume: Double
)
