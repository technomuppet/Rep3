package com.replog.data.db

import androidx.room.*
import com.replog.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Transaction @Query("SELECT * FROM workout_templates ORDER BY isBuiltIn DESC, name ASC") fun getAllTemplates(): Flow<List<TemplateWithExercises>>
    @Transaction @Query("SELECT * FROM workout_templates WHERE id = :templateId") suspend fun getTemplateById(templateId: Int): TemplateWithExercises?
    @Insert suspend fun insertTemplate(template: WorkoutTemplate): Long
    @Insert suspend fun insertTemplateExercise(templateExercise: TemplateExercise): Long
    @Update suspend fun updateTemplate(template: WorkoutTemplate)
    @Delete suspend fun deleteTemplate(template: WorkoutTemplate)
    @Query("DELETE FROM template_exercises WHERE templateId = :templateId") suspend fun deleteTemplateExercises(templateId: Int)
    @Query("UPDATE workout_templates SET isFavorite = :favorite WHERE id = :templateId") suspend fun setFavorite(templateId: Int, favorite: Boolean)
    @Transaction @Query("SELECT * FROM workout_templates WHERE isFavorite = 1 ORDER BY name ASC") fun getFavoriteTemplates(): Flow<List<TemplateWithExercises>>
}
