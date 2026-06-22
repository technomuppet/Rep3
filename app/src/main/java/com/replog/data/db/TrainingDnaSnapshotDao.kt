package com.replog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.replog.data.model.TrainingDnaSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDnaSnapshotDao {
    @Query("SELECT * FROM training_dna_snapshots ORDER BY generatedAt DESC LIMIT 1")
    fun getLatest(): Flow<TrainingDnaSnapshot?>

    @Query("SELECT * FROM training_dna_snapshots ORDER BY generatedAt DESC")
    fun getAll(): Flow<List<TrainingDnaSnapshot>>

    @Query("SELECT * FROM training_dna_snapshots ORDER BY generatedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TrainingDnaSnapshot>

    @Insert
    suspend fun insert(snapshot: TrainingDnaSnapshot): Long

    @Query("DELETE FROM training_dna_snapshots WHERE generatedAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COUNT(*) FROM training_dna_snapshots")
    suspend fun count(): Int
}
