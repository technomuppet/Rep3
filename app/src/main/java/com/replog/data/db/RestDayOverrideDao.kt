package com.replog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.replog.data.model.RestDayOverride

@Dao
interface RestDayOverrideDao {
    @Insert suspend fun insert(override: RestDayOverride): Long

    @Query("SELECT * FROM rest_day_overrides ORDER BY timestamp DESC")
    suspend fun getAll(): List<RestDayOverride>

    @Query("SELECT * FROM rest_day_overrides ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<RestDayOverride>

    @Query("SELECT COUNT(*) FROM rest_day_overrides")
    suspend fun count(): Int

    @Query("DELETE FROM rest_day_overrides")
    suspend fun deleteAll()
}
