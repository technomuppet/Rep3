package com.replog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.replog.data.model.KnowledgeGraphRelation

@Dao
interface KnowledgeGraphDao {
    @Query("""
        SELECT * FROM knowledge_graph_relations
        WHERE (sourceType = :entityType AND sourceId = :entityId)
           OR (targetType = :entityType AND targetId = :entityId)
        ORDER BY strength DESC
    """)
    suspend fun getRelations(entityType: String, entityId: String): List<KnowledgeGraphRelation>

    @Query("SELECT * FROM knowledge_graph_relations WHERE relationType = :relationType ORDER BY strength DESC")
    suspend fun getRelationsByType(relationType: String): List<KnowledgeGraphRelation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: KnowledgeGraphRelation): Long

    @Update
    suspend fun updateRelation(relation: KnowledgeGraphRelation)
}
