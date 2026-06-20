package com.replog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.replog.data.model.WorkoutPrescription

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM workout_prescriptions WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getPrescriptionsForSession(sessionId: Int): List<WorkoutPrescription>

    @Query("SELECT * FROM workout_prescriptions WHERE sessionId = :sessionId AND exerciseId = :exerciseId LIMIT 1")
    suspend fun getPrescription(sessionId: Int, exerciseId: Int): WorkoutPrescription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: WorkoutPrescription): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptions(prescriptions: List<WorkoutPrescription>)

    @Query("DELETE FROM workout_prescriptions WHERE sessionId = :sessionId")
    suspend fun deletePrescriptionsForSession(sessionId: Int)
}
