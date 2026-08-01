package com.replog.data.repository

import com.replog.data.db.PlateauEventDao
import com.replog.data.db.SessionDao
import com.replog.data.db.TrainingDnaProgressionScoreDao
import com.replog.data.db.TrainingDnaSnapshotDao
import com.replog.data.model.PlateauEvent
import com.replog.data.model.TrainingDnaProgressionScore
import com.replog.data.model.TrainingDnaSnapshot
import com.replog.domain.trainingdna.TrainingDnaConfig
import com.replog.domain.trainingdna.TrainingDnaEngine
import com.replog.domain.trainingdna.TrainingDnaResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingDNARepository @Inject constructor(
    private val snapshotDao: TrainingDnaSnapshotDao,
    private val progressionScoreDao: TrainingDnaProgressionScoreDao,
    private val plateauEventDao: PlateauEventDao,
    private val sessionDao: SessionDao
) {
    suspend fun generateDNA(): TrainingDnaResult {
        val sessions = sessionDao.getRecentCompletedSessions(200).first().filter { it.session.endTime != null }
        val engine = TrainingDnaEngine(TrainingDnaConfig(nowMillis = System.currentTimeMillis()))
        val result = engine.generate(sessions)
        persist(result)
        return result
    }

    suspend fun generateDNAFromSessions(sessions: List<com.replog.data.model.SessionWithExercises>): TrainingDnaResult {
        val engine = TrainingDnaEngine(TrainingDnaConfig(nowMillis = System.currentTimeMillis()))
        val result = engine.generate(sessions.filter { it.session.endTime != null })
        persist(result)
        return result
    }

    fun getLatestDNA(): Flow<TrainingDnaSnapshot?> = snapshotDao.getLatest()

    fun getHistoricalDNA(): Flow<List<TrainingDnaSnapshot>> = snapshotDao.getAll()

    fun getPlateauEvents(): Flow<List<PlateauEvent>> = plateauEventDao.getAll()

    fun getProgressionScores(): Flow<List<TrainingDnaProgressionScore>> = progressionScoreDao.getAll()

    suspend fun clearOldData(olderThanMillis: Long) {
        snapshotDao.deleteOlderThan(olderThanMillis)
        progressionScoreDao.deleteOlderThan(olderThanMillis)
        plateauEventDao.deleteOlderThan(olderThanMillis)
    }

    private suspend fun persist(result: TrainingDnaResult) {
        snapshotDao.insert(result.snapshot)
        progressionScoreDao.insertAll(result.progressionScores)
        plateauEventDao.insertAll(result.plateauEvents)
    }
}
