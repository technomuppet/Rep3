package com.replog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.replog.data.model.PlateauEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface PlateauEventDao {
    @Query("SELECT * FROM plateau_events ORDER BY detectedAt DESC")
    fun getAll(): Flow<List<PlateauEvent>>

    @Query("SELECT * FROM plateau_events WHERE exerciseId = :exerciseId ORDER BY detectedAt DESC LIMIT 1")
    suspend fun getLatestForExercise(exerciseId: Int): PlateauEvent?

    @Insert
    suspend fun insert(event: PlateauEvent): Long

    @Insert
    suspend fun insertAll(events: List<PlateauEvent>)

    @Query("DELETE FROM plateau_events WHERE detectedAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("DELETE FROM plateau_events WHERE exerciseId = :exerciseId")
    suspend fun deleteForExercise(exerciseId: Int)

    @Query("SELECT COUNT(*) FROM plateau_events")
    suspend fun count(): Int
}
