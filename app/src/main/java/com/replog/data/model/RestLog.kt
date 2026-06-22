package com.replog.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rest_logs",
    foreignKeys = [
        ForeignKey(entity = SessionExercise::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SetLog::class, parentColumns = ["id"], childColumns = ["setId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [
        Index("sessionExerciseId"),
        Index("setId"),
        Index("startedAt")
    ]
)
data class RestLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionExerciseId: Int,
    val setId: Int?,
    val plannedSeconds: Int,
    val actualSeconds: Int?,
    val skipped: Boolean = false,
    val startedAt: Long,
    val completedAt: Long?
)
