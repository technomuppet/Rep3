package com.replog.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.replog.data.db.TrainingDnaDao
import com.replog.data.model.TrainingDnaMetric
import com.replog.domain.trainingdna.TrainingDnaDimension
import com.replog.domain.trainingdna.TrainingDnaSignal
import com.replog.domain.trainingdna.TrainingDnaStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingDnaRepository @Inject constructor(
    private val dao: TrainingDnaDao
) : TrainingDnaStore {
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    override suspend fun getSignals(): List<TrainingDnaSignal> = dao.getAllMetrics().map { it.toSignal() }

    override suspend fun upsertSignal(signal: TrainingDnaSignal) {
        val existing = dao.getMetric(signal.dimension.name, signal.subjectType, signal.subjectId)
        val metric = TrainingDnaMetric(
            id = existing?.id ?: 0,
            dimension = signal.dimension.name,
            subjectType = signal.subjectType,
            subjectId = signal.subjectId,
            value = signal.value,
            confidence = signal.confidence,
            sampleSize = signal.sampleSize,
            updatedAt = System.currentTimeMillis(),
            metadataJson = gson.toJson(signal.metadata)
        )
        if (existing == null) dao.insertMetric(metric) else dao.updateMetric(metric)
    }

    private fun TrainingDnaMetric.toSignal(): TrainingDnaSignal = TrainingDnaSignal(
        dimension = runCatching { TrainingDnaDimension.valueOf(dimension) }.getOrDefault(TrainingDnaDimension.EXERCISE_RESPONSIVENESS),
        subjectType = subjectType,
        subjectId = subjectId,
        value = value,
        confidence = confidence,
        sampleSize = sampleSize,
        metadata = runCatching { gson.fromJson<Map<String, String>>(metadataJson, mapType) }.getOrDefault(emptyMap())
    )
}
