package com.replog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [Index("name")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val equipment: String,
    val type: String = "Strength",
    val muscles: String = "",
    val primaryMuscles: String = "",
    val secondaryMuscles: String = "",
    val movementPattern: String = "",
    val difficulty: String = "Intermediate",
    val mediaAsset: String = "",
    val isCustom: Boolean = false
)
