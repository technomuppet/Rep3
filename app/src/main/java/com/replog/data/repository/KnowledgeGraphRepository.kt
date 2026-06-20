package com.replog.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.replog.data.db.KnowledgeGraphDao
import com.replog.data.model.KnowledgeGraphRelation
import com.replog.domain.discoveries.KnowledgeEntityType
import com.replog.domain.discoveries.KnowledgeGraphStore
import com.replog.domain.discoveries.KnowledgeRelation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeGraphRepository @Inject constructor(
    private val dao: KnowledgeGraphDao
) : KnowledgeGraphStore {
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    override suspend fun getRelations(entityType: KnowledgeEntityType, entityId: String): List<KnowledgeRelation> =
        dao.getRelations(entityType.name, entityId).map { it.toDomain() }

    override suspend fun upsertRelation(relation: KnowledgeRelation) {
        dao.insertRelation(relation.toEntity())
    }

    private fun KnowledgeRelation.toEntity(): KnowledgeGraphRelation = KnowledgeGraphRelation(
        sourceType = sourceType.name,
        sourceId = sourceId,
        targetType = targetType.name,
        targetId = targetId,
        relationType = relationType,
        strength = strength,
        evidenceCount = evidenceCount,
        metadataJson = gson.toJson(metadata)
    )

    private fun KnowledgeGraphRelation.toDomain(): KnowledgeRelation = KnowledgeRelation(
        sourceType = parseType(sourceType),
        sourceId = sourceId,
        targetType = parseType(targetType),
        targetId = targetId,
        relationType = relationType,
        strength = strength,
        evidenceCount = evidenceCount,
        metadata = runCatching { gson.fromJson<Map<String, String>>(metadataJson, mapType) }.getOrDefault(emptyMap())
    )

    private fun parseType(value: String): KnowledgeEntityType =
        runCatching { KnowledgeEntityType.valueOf(value) }.getOrDefault(KnowledgeEntityType.PERFORMANCE)
}
