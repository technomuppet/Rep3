package com.replog.data.db

import androidx.room.*
import com.replog.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Transaction @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC") fun getAllSessions(): Flow<List<SessionWithExercises>>
    @Transaction @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC LIMIT :limit") fun getRecentSessions(limit: Int): Flow<List<SessionWithExercises>>
    @Transaction @Query("SELECT * FROM workout_sessions WHERE id = :sessionId") suspend fun getSessionById(sessionId: Int): SessionWithExercises?
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId") suspend fun getSessionEntity(sessionId: Int): WorkoutSession?
    @Insert suspend fun insertSession(session: WorkoutSession): Long
    @Update suspend fun updateSession(session: WorkoutSession)
    @Delete suspend fun deleteSession(session: WorkoutSession)
    @Query("DELETE FROM workout_sessions WHERE id = :sessionId") suspend fun deleteSessionById(sessionId: Int)
    // Wipes all workout history. Cascading foreign keys remove the related
    // session_exercises, set_logs (incl. PR flags) and prescriptions.
    @Query("DELETE FROM workout_sessions") suspend fun deleteAllSessions()
    @Insert suspend fun insertSessionExercise(sessionExercise: SessionExercise): Long
    @Update suspend fun updateSessionExercise(sessionExercise: SessionExercise)
    @Query("DELETE FROM session_exercises WHERE id = :id") suspend fun deleteSessionExercise(id: Int)
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC LIMIT 1") suspend fun getLastSession(): WorkoutSession?
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE endTime IS NOT NULL") suspend fun getCompletedSessionCount(): Int
    @Query("SELECT COALESCE(SUM(set_logs.weight * set_logs.reps), 0) FROM set_logs INNER JOIN session_exercises ON set_logs.sessionExerciseId = session_exercises.id INNER JOIN workout_sessions ON session_exercises.sessionId = workout_sessions.id WHERE workout_sessions.endTime IS NOT NULL") suspend fun getTotalVolume(): Double
    @Query("UPDATE session_exercises SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateSessionExerciseOrder(id: Int, orderIndex: Int)
    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getSessionExercisesOrdered(sessionId: Int): List<SessionExercise>
}
