package com.replog.domain.visual.anatomy

import androidx.compose.ui.graphics.Path

/**
 * Scalable vector body model containing silhouette and regional muscle paths.
 * Normalised over a 500x1000 coordinate grid.
 */
data class VectorBody(
    val side: BodySide,
    val silhouettePath: Path,
    val regionPaths: Map<MuscleRegion, Path>
) {
    companion object {
        val FRONT = VectorBody(BodySide.FRONT, FrontBody.silhouettePath, FrontBody.regionPaths)
        val BACK = VectorBody(BodySide.BACK, BackBody.silhouettePath, BackBody.regionPaths)
    }
}
