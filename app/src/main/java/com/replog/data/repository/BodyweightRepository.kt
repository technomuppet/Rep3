package com.replog.data.repository

import com.replog.data.db.BodyweightDao
import com.replog.data.model.BodyweightLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyweightRepository @Inject constructor(
    private val bodyweightDao: BodyweightDao
) {
    fun getAllBodyweights(): Flow<List<BodyweightLog>> = bodyweightDao.getAllBodyweights()
    suspend fun getLatestBodyweight(): BodyweightLog? = bodyweightDao.getLatestBodyweight()
    suspend fun insertBodyweight(log: BodyweightLog): Long = bodyweightDao.insertBodyweight(log)
    suspend fun updateBodyweight(log: BodyweightLog) = bodyweightDao.updateBodyweight(log)
    suspend fun deleteBodyweight(log: BodyweightLog) = bodyweightDao.deleteBodyweight(log)
    suspend fun deleteBodyweightById(id: Int) = bodyweightDao.deleteBodyweightById(id)
}
