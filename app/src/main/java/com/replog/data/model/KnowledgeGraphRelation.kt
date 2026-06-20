package com.replog.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_graph_relations",
    indices = [
        Index("sourceType"),
        Index("sourceId"),
        Index("targetType"),
        Index("targetId"),
        Index("relationType"),
        Index(value = ["sourceType", "sourceId", "targetType", "targetId", "relationType"], unique = true)
    ]
)
data class KnowledgeGraphRelation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceType: String,
    val sourceId: String,
    val targetType: String,
    val targetId: String,
    val relationType: String,
    val strength: Double,
    val evidenceCount: Int,
    val firstSeenAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val metadataJson: String = "{}"
)
