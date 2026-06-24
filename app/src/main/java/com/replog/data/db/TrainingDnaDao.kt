package com.replog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.replog.data.model.TrainingDnaMetric

@Dao
interface TrainingDnaDao {
    @Query("SELECT * FROM training_dna_metrics ORDER BY updatedAt DESC")
    suspend fun getAllMetrics(): List<TrainingDnaMetric>

    @Query("SELECT * FROM training_dna_metrics WHERE dimension = :dimension")
    suspend fun getMetricsByDimension(dimension: String): List<TrainingDnaMetric>

    @Query("""
        SELECT * FROM training_dna_metrics
        WHERE dimension = :dimension
          AND subjectType = :subjectType
          AND ((subjectId IS NULL AND :subjectId IS NULL) OR subjectId = :subjectId)
        LIMIT 1
    """)
    suspend fun getMetric(dimension: String, subjectType: String, subjectId: String?): TrainingDnaMetric?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: TrainingDnaMetric): Long

    @Update
    suspend fun updateMetric(metric: TrainingDnaMetric)

    @Query("DELETE FROM training_dna_metrics")
    suspend fun deleteAll()
}
