package com.replog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.replog.data.model.RecommendationHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationHistoryDao {
    @Query("SELECT * FROM recommendation_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<RecommendationHistory>>

    @Query("SELECT * FROM recommendation_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<RecommendationHistory>

    @Query("SELECT * FROM recommendation_history WHERE outcome = :outcome ORDER BY timestamp DESC")
    suspend fun getByOutcome(outcome: String): List<RecommendationHistory>

    @Query("SELECT * FROM recommendation_history WHERE recommendationType = :type ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getByType(type: String, limit: Int): List<RecommendationHistory>

    @Query("SELECT COUNT(*) FROM recommendation_history WHERE outcome = 'Accepted'")
    suspend fun getAcceptedCount(): Int

    @Query("SELECT COUNT(*) FROM recommendation_history WHERE outcome = 'Rejected'")
    suspend fun getRejectedCount(): Int

    @Query("SELECT COUNT(*) FROM recommendation_history WHERE outcome = 'Completed'")
    suspend fun getCompletedCount(): Int

    @Insert
    suspend fun insert(history: RecommendationHistory): Long

    @Update
    suspend fun update(history: RecommendationHistory)

    @Query("DELETE FROM recommendation_history WHERE timestamp < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COUNT(*) FROM recommendation_history")
    suspend fun count(): Int
}
