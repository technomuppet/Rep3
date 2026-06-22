package com.replog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.replog.data.model.RestLog

@Dao
interface RestLogDao {
    @Insert suspend fun insert(log: RestLog): Long
    @Update suspend fun update(log: RestLog)
    @Query("SELECT * FROM rest_logs WHERE id = :id") suspend fun getById(id: Int): RestLog?
    @Query("SELECT AVG(actualSeconds) FROM rest_logs WHERE actualSeconds IS NOT NULL AND skipped = 0") suspend fun getAverageRest(): Double?
    @Query("SELECT * FROM rest_logs ORDER BY startedAt DESC LIMIT 100") suspend fun getRecent(): List<RestLog>
}
