package com.replog.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isBuiltIn: Boolean = false,
    /** Sprint 5 P5: starred/pinned templates surface in the Home quick-launch row. */
    val isFavorite: Boolean = false
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(entity = WorkoutTemplate::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("templateId"), Index("exerciseId")]
)
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateId: Int,
    val exerciseId: Int,
    val defaultSets: Int = 3,
    val orderIndex: Int,
    val targetReps: Int = 8,
    val targetWeight: Double? = null
)
