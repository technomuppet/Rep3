package com.replog.data.db

import androidx.room.*
import com.replog.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC") fun getAllExercises(): Flow<List<Exercise>>
    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name ASC") fun getExercisesByCategory(category: String): Flow<List<Exercise>>
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR equipment LIKE '%' || :query || '%' OR muscles LIKE '%' || :query || '%' OR primaryMuscles LIKE '%' || :query || '%' OR secondaryMuscles LIKE '%' || :query || '%' OR movementPattern LIKE '%' || :query || '%' ORDER BY name ASC") fun searchExercises(query: String): Flow<List<Exercise>>
    @Query("SELECT * FROM exercises WHERE id = :id") suspend fun getExerciseById(id: Int): Exercise?
    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1") suspend fun getExerciseByName(name: String): Exercise?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExercise(exercise: Exercise): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExercises(exercises: List<Exercise>)
    @Update suspend fun updateExercise(exercise: Exercise)
    @Delete suspend fun deleteExercise(exercise: Exercise)
    @Query("SELECT DISTINCT category FROM exercises ORDER BY category ASC") fun getAllCategories(): Flow<List<String>>
    @Query("SELECT COUNT(*) FROM exercises") suspend fun countExercises(): Int
}
