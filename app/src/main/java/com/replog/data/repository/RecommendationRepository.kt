package com.replog.data.repository

import com.replog.data.db.RecommendationHistoryDao
import com.replog.data.model.RecommendationHistory
import com.replog.data.model.SessionWithExercises
import com.replog.domain.recommendation.Recommendation
import com.replog.domain.recommendation.RecommendationContext
import com.replog.domain.recommendation.RecommendationEngine
import com.replog.domain.recommendation.RecommendationType
import com.replog.domain.recommendation.WorkoutPlan
import com.replog.domain.trainingdna.TrainingDnaConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationRepository @Inject constructor(
    private val historyDao: RecommendationHistoryDao,
    private val trainingDNARepository: TrainingDNARepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val bodyweightRepository: BodyweightRepository
) {
    private val engine = RecommendationEngine()

    suspend fun generateRecommendation(): Recommendation {
        val context = buildContext()
        return engine.generate(context)
    }

    suspend fun generateRecommendationAndEnsureDNA(): Recommendation {
        trainingDNARepository.generateDNA()
        return generateRecommendation()
    }

    fun getRecommendationHistory(): Flow<List<RecommendationHistory>> = historyDao.getAll()

    suspend fun recordAcceptance(recommendation: Recommendation, completed: Boolean = false) {
        val history = recommendation.toHistory(
            outcome = if (completed) "Completed" else "Accepted"
        )
        historyDao.insert(history)
    }

    suspend fun recordRejection(recommendation: Recommendation, reason: String? = null) {
        val history = recommendation.toHistory(
            outcome = "Rejected",
            rejectedReason = reason
        )
        historyDao.insert(history)
    }

    suspend fun getSuccessRate(): Pair<Int, Int> {
        val accepted = historyDao.getAcceptedCount() + historyDao.getCompletedCount()
        val rejected = historyDao.getRejectedCount()
        return accepted to rejected
    }

    suspend fun clearOldHistory(olderThanMillis: Long) {
        historyDao.deleteOlderThan(olderThanMillis)
    }

    private suspend fun buildContext(): RecommendationContext {
        val sessions = workoutRepository.getAllSessions().first().filter { it.session.endTime != null }
        val exercises = exerciseRepository.getAllExercises().first()
        val dna = trainingDNARepository.getLatestDNA().first()
        val plateaus = trainingDNARepository.getPlateauEvents().first()
        val scores = trainingDNARepository.getProgressionScores().first()
        val bodyweights = bodyweightRepository.getAllBodyweights().first()

        return RecommendationContext(
            sessions = sessions,
            exercises = exercises,
            dnaSnapshot = dna,
            plateauEvents = plateaus,
            progressionScores = scores,
            bodyweights = bodyweights,
            nowMillis = System.currentTimeMillis()
        )
    }

    private fun Recommendation.toHistory(
        outcome: String,
        rejectedReason: String? = null
    ): RecommendationHistory = RecommendationHistory(
        timestamp = System.currentTimeMillis(),
        recommendationType = type.name,
        title = title,
        explanation = explanation,
        dataUsed = dataUsed.joinToString("\n"),
        reasoning = reasoning.joinToString("\n"),
        expectedOutcome = expectedOutcome,
        confidenceScore = confidenceScore,
        estimatedDurationMinutes = estimatedDurationMinutes,
        workoutSplit = workoutPlan?.split?.name,
        outcome = outcome,
        rejectedReason = rejectedReason
    )
}
