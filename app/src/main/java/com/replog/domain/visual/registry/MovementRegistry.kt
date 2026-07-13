package com.replog.domain.visual.registry

/**
 * Single source of truth registry mapping family identifiers and pattern strings
 * to their parametric MovementFamilyMetadata definitions.
 */
object MovementRegistry {
    private val byId: Map<String, MovementFamilyMetadata> = MovementFamily.ALL.associateBy { it.id }

    fun getById(familyId: String): MovementFamilyMetadata =
        byId[familyId] ?: MovementFamily.GENERIC_UNMAPPED

    fun getAllFamilies(): List<MovementFamilyMetadata> = MovementFamily.ALL

    fun isRegistered(familyId: String): Boolean = byId.containsKey(familyId) && familyId != "GENERIC_UNMAPPED"
}
