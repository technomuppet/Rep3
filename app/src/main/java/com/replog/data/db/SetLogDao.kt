package com.replog.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.replog.data.model.ExerciseSetHistory
import com.replog.data.model.SetLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogDao {
    @Insert suspend fun insertSet(setLog: SetLog): Long
    @Update suspend fun updateSet(setLog: SetLog)
    @Delete suspend fun deleteSet(setLog: SetLog)

    @Query("DELETE FROM set_logs WHERE id = :setId")
    suspend fun deleteSetById(setId: Int)

    @Query("SELECT * FROM set_logs WHERE sessionExerciseId = :sessionExerciseId ORDER BY setNumber ASC")
    fun getSetsForSessionExercise(sessionExerciseId: Int): Flow<List<SetLog>>

    @Query("""
        SELECT COALESCE(MAX(set_logs.weight), 0)
        FROM set_logs
        INNER JOIN session_exercises ON set_logs.sessionExerciseId = session_exercises.id
        WHERE session_exercises.exerciseId = :exerciseId AND set_logs.reps = :reps
    """)
    suspend fun getMaxWeightForReps(exerciseId: Int, reps: Int): Double

    @Query("""
        SELECT COALESCE(MAX(set_logs.weight), 0)
        FROM set_logs
        INNER JOIN session_exercises ON set_logs.sessionExerciseId = session_exercises.id
        WHERE session_exercises.exerciseId = :exerciseId AND set_logs.reps = :reps AND set_logs.id != :excludeSetId
    """)
    suspend fun getMaxWeightForRepsExcludingSet(exerciseId: Int, reps: Int, excludeSetId: Int): Double

    @Query("""
        SELECT COALESCE(MAX(set_logs.weight), 0)
        FROM set_logs
        INNER JOIN session_exercises ON set_logs.sessionExerciseId = session_exercises.id
        WHERE session_exercises.exerciseId = :exerciseId
    """)
    suspend fun getMaxWeightForExercise(exerciseId: Int): Double

    @Query("""
        SELECT set_logs.*
        FROM set_logs
        INNER JOIN session_exercises ON set_logs.sessionExerciseId = session_exercises.id
        INNER JOIN workout_sessions ON session_exercises.sessionId = workout_sessions.id
        WHERE session_exercises.exerciseId = :exerciseId
          AND workout_sessions.endTime IS NOT NULL
        ORDER BY set_logs.timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentSetsForExercise(exerciseId: Int, limit: Int = 3): List<SetLog>

    @Query("""
        SELECT set_logs.*
        FROM set_logs
        INNER JOIN session_exercises ON set_logs.sessionExerciseId = session_exercises.id
        WHERE session_exercises.exerciseId = :exerciseId AND set_logs.isPR = 1
        ORDER BY timestamp DESC LIMIT 5
    """)
    fun getRecentPRsForExercise(exerciseId: Int): Flow<List<SetLog>>

    @Query("""
        SELECT set_logs.*
        FROM set_logs
        INNER JOIN session_exercises ON set_logs.sessionExerciseId = session_exercises.id
        WHERE set_logs.isPR = 1
        ORDER BY timestamp DESC LIMIT 5
    """)
    fun getRecentPRs(): Flow<List<SetLog>>

    @Query("""
        SELECT
            set_logs.id AS setId,
            workout_sessions.id AS sessionId,
            exercises.id AS exerciseId,
            exercises.name AS exerciseName,
            workout_sessions.startTime AS workoutStartTime,
            set_logs.weight AS weight,
            set_logs.reps AS reps,
            set_logs.isPR AS isPR
        FROM set_logs
        INNER JOIN session_exercises ON set_logs.sessionExerciseId = session_exercises.id
        INNER JOIN workout_sessions ON session_exercises.sessionId = workout_sessions.id
        INNER JOIN exercises ON session_exercises.exerciseId = exercises.id
        WHERE exercises.id = :exerciseId
          AND workout_sessions.endTime IS NOT NULL
        ORDER BY workout_sessions.startTime ASC, set_logs.setNumber ASC
    """)
    fun getExerciseHistory(exerciseId: Int): Flow<List<ExerciseSetHistory>>
}
