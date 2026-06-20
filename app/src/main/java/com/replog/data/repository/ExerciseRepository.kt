package com.replog.data.repository

import com.replog.data.db.ExerciseDao
import com.replog.data.model.Exercise
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(private val dao: ExerciseDao) {
    fun getAllExercises(): Flow<List<Exercise>> = dao.getAllExercises()
    fun getExercisesByCategory(category: String): Flow<List<Exercise>> = dao.getExercisesByCategory(category)
    fun searchExercises(query: String): Flow<List<Exercise>> = dao.searchExercises(query)
    fun getAllCategories(): Flow<List<String>> = dao.getAllCategories()
    suspend fun getExerciseById(id: Int): Exercise? = dao.getExerciseById(id)
    suspend fun getExerciseByName(name: String): Exercise? = dao.getExerciseByName(name)
    suspend fun insertExercise(exercise: Exercise): Long = dao.insertExercise(exercise)
    suspend fun insertExercises(exercises: List<Exercise>) = dao.insertExercises(exercises)
    suspend fun updateExercise(exercise: Exercise) = dao.updateExercise(exercise)
    suspend fun deleteExercise(exercise: Exercise) = dao.deleteExercise(exercise)
    suspend fun countExercises(): Int = dao.countExercises()
}
