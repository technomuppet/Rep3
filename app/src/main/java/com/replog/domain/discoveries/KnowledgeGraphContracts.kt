package com.replog.domain.discoveries

enum class KnowledgeEntityType {
    EXERCISE,
    MUSCLE,
    BODYWEIGHT,
    VOLUME,
    FREQUENCY,
    PERFORMANCE,
    RECOVERY,
    PROGRAM
}

data class KnowledgeRelation(
    val sourceType: KnowledgeEntityType,
    val sourceId: String,
    val targetType: KnowledgeEntityType,
    val targetId: String,
    val relationType: String,
    val strength: Double,
    val evidenceCount: Int,
    val metadata: Map<String, String> = emptyMap()
)

interface KnowledgeGraphStore {
    suspend fun getRelations(entityType: KnowledgeEntityType, entityId: String): List<KnowledgeRelation>
    suspend fun upsertRelation(relation: KnowledgeRelation)
}

interface DiscoveryEngine {
    suspend fun discover(): List<KnowledgeRelation>
}
