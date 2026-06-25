package com.replog.data.repository

import com.replog.data.db.SessionDao
import com.replog.data.db.SetLogDao
import com.replog.data.db.PrescriptionDao
import com.replog.data.db.TemplateDao
import com.replog.data.model.ExerciseSetHistory
import com.replog.data.model.SessionExercise
import com.replog.data.model.SessionWithExercises
import com.replog.data.model.SetLog
import com.replog.data.model.TemplateExercise
import com.replog.data.model.TemplateWithExercises
import com.replog.data.model.WorkoutPrescription
import com.replog.data.model.WorkoutSession
import com.replog.data.model.WorkoutTemplate
import com.replog.domain.pr.PRDetector
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val setLogDao: SetLogDao,
    private val templateDao: TemplateDao,
    private val prescriptionDao: PrescriptionDao
) {
    fun getAllSessions(): Flow<List<SessionWithExercises>> = sessionDao.getAllSessions()
    fun getRecentSessions(limit: Int): Flow<List<SessionWithExercises>> = sessionDao.getRecentSessions(limit)
    /** Phase 2: SQL-aggregated completed-session summaries (no full graph load). */
    fun getCompletedSessionSummaries(): Flow<List<com.replog.data.model.SessionSummaryRow>> = sessionDao.getCompletedSessionSummaries()
    /** Phase 2: bounded recent completed sessions with full set detail. */
    fun getRecentCompletedSessions(limit: Int): Flow<List<SessionWithExercises>> = sessionDao.getRecentCompletedSessions(limit)
    suspend fun getSessionById(sessionId: Int): SessionWithExercises? = sessionDao.getSessionById(sessionId)
    suspend fun getSessionEntity(sessionId: Int): WorkoutSession? = sessionDao.getSessionEntity(sessionId)
    suspend fun insertSession(session: WorkoutSession): Long = sessionDao.insertSession(session)
    suspend fun updateSession(session: WorkoutSession) = sessionDao.updateSession(session)
    suspend fun setSessionRating(sessionId: Int, rating: Int) {
        sessionDao.getSessionEntity(sessionId)?.let { sessionDao.updateSession(it.copy(sessionRating = rating)) }
    }
    suspend fun deleteSession(session: WorkoutSession) = sessionDao.deleteSession(session)
    suspend fun deleteSessionById(sessionId: Int) = sessionDao.deleteSessionById(sessionId)
    /** Bulk delete: removes ALL workout history (sessions, exercises, sets, PRs, prescriptions cascade). */
    suspend fun deleteAllSessions() = sessionDao.deleteAllSessions()
    suspend fun insertSessionExercise(sessionExercise: SessionExercise): Long = sessionDao.insertSessionExercise(sessionExercise)
    suspend fun updateSessionExercise(sessionExercise: SessionExercise) = sessionDao.updateSessionExercise(sessionExercise)
    suspend fun deleteSessionExercise(id: Int) = sessionDao.deleteSessionExercise(id)
    suspend fun getCompletedSessionCount(): Int = sessionDao.getCompletedSessionCount()
    suspend fun getTotalVolume(): Double = sessionDao.getTotalVolume()
    
    suspend fun reorderSessionExercises(sessionId: Int, orderedIds: List<Int>) {
        orderedIds.forEachIndexed { index, id -> sessionDao.updateSessionExerciseOrder(id, index) }
    }

    suspend fun insertSet(setLog: SetLog): Long = setLogDao.insertSet(setLog)
    suspend fun updateSet(setLog: SetLog) = setLogDao.updateSet(setLog)
    suspend fun deleteSet(setLog: SetLog) = setLogDao.deleteSet(setLog)
    suspend fun deleteSetById(setId: Int) = setLogDao.deleteSetById(setId)
    fun getSetsForSessionExercise(sessionExerciseId: Int): Flow<List<SetLog>> = setLogDao.getSetsForSessionExercise(sessionExerciseId)
    suspend fun getMaxWeightForReps(exerciseId: Int, reps: Int): Double = setLogDao.getMaxWeightForReps(exerciseId, reps)
    suspend fun getMaxWeightForExercise(exerciseId: Int): Double = setLogDao.getMaxWeightForExercise(exerciseId)
    suspend fun getRecentSetsForExercise(exerciseId: Int, limit: Int = 3): List<SetLog> = setLogDao.getRecentSetsForExercise(exerciseId, limit)
    
    suspend fun getPreviousWorkoutSetsForExercise(exerciseId: Int, excludeSessionId: Int?): List<SetLog> =
        setLogDao.getPreviousWorkoutSetsForExercise(exerciseId, excludeSessionId)
    suspend fun getLastSetsExcludingSession(exerciseId: Int, excludeSessionId: Int?, limit: Int): List<SetLog> =
        setLogDao.getLastSetsExcludingSession(exerciseId, excludeSessionId, limit)

    suspend fun getAllSetsForExercise(exerciseId: Int): List<SetLog> = setLogDao.getAllSetsForExercise(exerciseId)

    fun getExerciseHistory(exerciseId: Int): Flow<List<ExerciseSetHistory>> = setLogDao.getExerciseHistory(exerciseId)
    fun getRecentPRs(): Flow<List<SetLog>> = setLogDao.getRecentPRs()

    fun getAllTemplates(): Flow<List<TemplateWithExercises>> = templateDao.getAllTemplates()
    fun getFavoriteTemplates(): Flow<List<TemplateWithExercises>> = templateDao.getFavoriteTemplates()
    suspend fun setTemplateFavorite(templateId: Int, favorite: Boolean) = templateDao.setFavorite(templateId, favorite)
    suspend fun getTemplateById(templateId: Int): TemplateWithExercises? = templateDao.getTemplateById(templateId)
    suspend fun insertTemplate(template: WorkoutTemplate): Long = templateDao.insertTemplate(template)
    suspend fun insertTemplateExercise(templateExercise: TemplateExercise): Long = templateDao.insertTemplateExercise(templateExercise)
    suspend fun updateTemplate(template: WorkoutTemplate) = templateDao.updateTemplate(template)
    suspend fun deleteTemplateExercises(templateId: Int) = templateDao.deleteTemplateExercises(templateId)
    suspend fun deleteTemplate(template: WorkoutTemplate) = templateDao.deleteTemplate(template)

    suspend fun getPrescriptionsForSession(sessionId: Int): List<WorkoutPrescription> = prescriptionDao.getPrescriptionsForSession(sessionId)
    suspend fun insertPrescription(prescription: WorkoutPrescription): Long = prescriptionDao.insertPrescription(prescription)
    suspend fun insertPrescriptions(prescriptions: List<WorkoutPrescription>) = prescriptionDao.insertPrescriptions(prescriptions)
    suspend fun deletePrescriptionsForSession(sessionId: Int) = prescriptionDao.deletePrescriptionsForSession(sessionId)

    suspend fun checkPR(exerciseId: Int, weight: Double, reps: Int, excludeSetId: Int? = null): com.replog.domain.pr.PRResult {
        val history = setLogDao.getAllSetsForExercise(exerciseId)
        return PRDetector.check(weight, reps, history, excludeSetId)
    }
    suspend fun isPR(exerciseId: Int, weight: Double, reps: Int): Boolean =
        checkPR(exerciseId, weight, reps).isPR

    suspend fun isPRExcludingSet(exerciseId: Int, weight: Double, reps: Int, setId: Int): Boolean =
        checkPR(exerciseId, weight, reps, setId).isPR
}
