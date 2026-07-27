package com.replog.domain.visual.anatomy

/**
 * Role a [PngAnatomyLayerAsset] plays when rendered.
 *
 * - [BASE] is the front or back body silhouette drawn before any overlays.
 * - [OVERLAY] is a muscle-specific PNG stacked on top of the matching base.
 */
enum class AnatomyLayerRole {
    BASE,
    OVERLAY
}
