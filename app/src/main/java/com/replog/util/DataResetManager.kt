package com.replog.util

import com.replog.data.db.PlateauEventDao
import com.replog.data.db.RecommendationHistoryDao
import com.replog.data.db.RestDayOverrideDao
import com.replog.data.db.RestLogDao
import com.replog.data.db.TrainingDnaProgressionScoreDao
import com.replog.data.db.TrainingDnaSnapshotDao
import com.replog.data.repository.WorkoutRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates a full "Delete all workout history" reset (Settings -> Advanced).
 *
 * Clears everything derived from logged training while deliberately preserving
 * the things the user did not ask to lose:
 *
 *  DELETED  - workout sessions (and, via cascading foreign keys, their
 *             session_exercises, set_logs incl. PR flags, and prescriptions)
 *           - Training DNA metrics, snapshots and progression scores
 *           - recovery / rest history (rest_logs)
 *           - plateau events
 *           - recommendation history
 *  PRESERVED- workout templates, the exercise library, goals, bodyweight log,
 *             and all settings/preferences (DataStore is untouched).
 *
 * DNA and recovery are derived data: once their history tables are empty they
 * naturally recompute (to an empty baseline now, and from fresh sessions as the
 * user logs again), so no explicit "recalculate" call is required - the next
 * load of Home / Training DNA regenerates from the current (empty) data.
 */
@Singleton
class DataResetManager @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val trainingDnaSnapshotDao: TrainingDnaSnapshotDao,
    private val trainingDnaProgressionScoreDao: TrainingDnaProgressionScoreDao,
    private val restLogDao: RestLogDao,
    private val plateauEventDao: PlateauEventDao,
    private val recommendationHistoryDao: RecommendationHistoryDao,
    private val restDayOverrideDao: RestDayOverrideDao
) {
    /**
     * Performs the reset. Returns the number of workout sessions that were
     * removed so the UI can confirm exactly what happened.
     */
    suspend fun deleteAllWorkoutHistory(): Int {
        val sessionCount = workoutRepository.getCompletedSessionCount()
        // Sessions first: cascades remove exercises, sets, PRs and prescriptions.
        workoutRepository.deleteAllSessions()
        // Derived analytics / history (regenerated from data on next load).
        trainingDnaSnapshotDao.deleteAll()
        trainingDnaProgressionScoreDao.deleteAll()
        plateauEventDao.deleteAll()
        restLogDao.deleteAll()
        recommendationHistoryDao.deleteAll()
        restDayOverrideDao.deleteAll()
        return sessionCount
    }
}
