package com.replog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user training goal (Goal Engine). Fully local.
 *
 * goalType matches com.replog.domain.goals.GoalType.name:
 *   STRENGTH_1RM | STRENGTH_REPS | BODYWEIGHT_LOSS | BODYWEIGHT_GAIN
 *
 * For strength goals, exerciseId/exerciseName identify the lift. For
 * STRENGTH_REPS, targetReps is the rep target (e.g. 10 pullups). For
 * bodyweight goals, exerciseId is null.
 */
@Entity(
    tableName = "goals",
    indices = [Index("status"), Index("createdAt")]
)
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalType: String,
    val title: String,
    val exerciseId: Int? = null,
    val exerciseName: String? = null,
    val targetValue: Double,
    val targetReps: Int? = null,
    val startValue: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val targetDate: Long? = null,
    /** "active" | "achieved" | "archived" */
    val status: String = "active",
    val achievedAt: Long? = null
)
