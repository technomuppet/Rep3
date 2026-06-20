package com.replog.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_prescriptions",
    foreignKeys = [
        ForeignKey(entity = WorkoutSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("exerciseId"), Index(value = ["sessionId", "exerciseId"], unique = true)]
)
data class WorkoutPrescription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val exerciseId: Int,
    val source: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double? = null,
    val adjustment: String = "Maintain",
    val reason: String = "Programmed target",
    val createdAt: Long = System.currentTimeMillis()
)
