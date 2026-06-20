package com.replog.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithExercises(
    @Embedded val session: WorkoutSession,
    @Relation(entity = SessionExercise::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithSets>
)

data class SessionExerciseWithSets(
    @Embedded val sessionExercise: SessionExercise,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId") val sets: List<SetLog>,
    @Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: Exercise
)

data class TemplateWithExercises(
    @Embedded val template: WorkoutTemplate,
    @Relation(entity = TemplateExercise::class, parentColumn = "id", entityColumn = "templateId")
    val exercises: List<TemplateExerciseWithDetails>
)

data class TemplateExerciseWithDetails(
    @Embedded val templateExercise: TemplateExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: Exercise
)
