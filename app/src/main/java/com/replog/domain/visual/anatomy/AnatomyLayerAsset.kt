package com.replog.domain.visual.anatomy

/**
 * Immutable description of a complete SVG layer in the anatomy renderer.
 *
 * A layer is a whole SVG file from assets/anatomy, not an extracted path. All
 * SVGs share the same canvas and are composited in deterministic order by the
 * layered renderer.
 */
data class AnatomyLayerAsset(
    val id: String,
    val side: BodySide,
    val assetPath: String,
    val role: AnatomyLayerRole,
    val order: Int,
    val region: MuscleRegion? = null,
    val mappingType: MuscleAssetMappingType = MuscleAssetMappingType.DIRECT,
    val mappedFrom: MuscleRegion? = null
)

enum class AnatomyLayerRole {
    BASE,
    OVERLAY
}

enum class MuscleAssetMappingType {
    /** The SVG directly represents this MuscleRegion. */
    DIRECT,

    /** The SVG is an intentional aggregate/closest visual alias for this MuscleRegion. */
    ALIAS
}
