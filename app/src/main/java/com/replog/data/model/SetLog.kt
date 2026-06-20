package com.replog.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object SetType {
    const val WORKING = "Working"
    const val WARMUP = "Warmup"
    const val DROP = "Drop Set"
    const val FAILURE = "Failure"
    const val AMRAP = "AMRAP"
    const val CLUSTER = "Cluster"
    const val REST_PAUSE = "Rest Pause"
    const val TEMPO = "Tempo"

    val all = listOf(WORKING, WARMUP, DROP, FAILURE, AMRAP, CLUSTER, REST_PAUSE, TEMPO)
}

@Entity(
    tableName = "set_logs",
    foreignKeys = [ForeignKey(entity = SessionExercise::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index("sessionExerciseId"),
        Index("timestamp"),
        Index(value = ["sessionExerciseId", "setNumber"])
    ]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionExerciseId: Int,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isBodyweight: Boolean = false,
    val isPR: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val setType: String = SetType.WORKING,
    val rpe: Double? = null,
    val tempo: String? = null
)
