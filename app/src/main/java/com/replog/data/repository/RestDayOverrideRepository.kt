package com.replog.data.repository

import com.replog.data.db.RestDayOverrideDao
import com.replog.data.model.RestDayOverride
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores "Train Anyway" decisions made against a recommended rest day. This
 * local record is used to inform future recovery intelligence (how often, and
 * at what recovery levels, the user chooses to train through fatigue).
 */
@Singleton
class RestDayOverrideRepository @Inject constructor(
    private val dao: RestDayOverrideDao
) {
    suspend fun record(recoveryScore: Int?, recommendationReason: String?) {
        dao.insert(
            RestDayOverride(
                timestamp = System.currentTimeMillis(),
                recoveryScore = recoveryScore,
                recommendationReason = recommendationReason
            )
        )
    }

    suspend fun all(): List<RestDayOverride> = dao.getAll()
    suspend fun recent(limit: Int): List<RestDayOverride> = dao.getRecent(limit)
    suspend fun count(): Int = dao.count()
}
