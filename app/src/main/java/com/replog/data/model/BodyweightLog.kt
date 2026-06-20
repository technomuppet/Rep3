package com.replog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bodyweight_logs")
data class BodyweightLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weight: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)
