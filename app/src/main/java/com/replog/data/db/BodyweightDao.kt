package com.replog.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.replog.data.model.BodyweightLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyweightDao {
    @Query("SELECT * FROM bodyweight_logs ORDER BY timestamp ASC")
    fun getAllBodyweights(): Flow<List<BodyweightLog>>

    @Query("SELECT * FROM bodyweight_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBodyweight(): BodyweightLog?

    @Insert
    suspend fun insertBodyweight(log: BodyweightLog): Long

    @Update
    suspend fun updateBodyweight(log: BodyweightLog)

    @Delete
    suspend fun deleteBodyweight(log: BodyweightLog)

    @Query("DELETE FROM bodyweight_logs WHERE id = :id")
    suspend fun deleteBodyweightById(id: Int)
}
