package com.replog.domain.visual.anatomy

/**
 * How a [PngAnatomyLayerAsset] was derived for its [MuscleRegion].
 *
 * - [DIRECT] is an entry authored from an explicit muscle-to-drawable mapping.
 * - [ALIAS] is an entry that re-uses the drawable of another region (e.g.
 *   LOWER_CHEST rendering the same PNG as CHEST); the originating region is
 *   recorded in [PngAnatomyLayerAsset.mappedFrom].
 */
enum class MuscleAssetMappingType {
    DIRECT,
    ALIAS
}
