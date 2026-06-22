package com.replog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.replog.data.model.TrainingDnaProgressionScore
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDnaProgressionScoreDao {
    @Query("SELECT * FROM training_dna_progression_scores ORDER BY calculatedAt DESC")
    fun getAll(): Flow<List<TrainingDnaProgressionScore>>

    @Query("SELECT * FROM training_dna_progression_scores WHERE exerciseId = :exerciseId ORDER BY calculatedAt DESC LIMIT 1")
    suspend fun getLatestForExercise(exerciseId: Int): TrainingDnaProgressionScore?

    @Insert
    suspend fun insert(score: TrainingDnaProgressionScore): Long

    @Insert
    suspend fun insertAll(scores: List<TrainingDnaProgressionScore>)

    @Query("DELETE FROM training_dna_progression_scores WHERE calculatedAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COUNT(*) FROM training_dna_progression_scores")
    suspend fun count(): Int
}
