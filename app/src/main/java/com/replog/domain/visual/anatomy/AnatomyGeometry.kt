package com.replog.domain.visual.anatomy

import androidx.compose.ui.graphics.Path

/**
 * Helper utilities for constructing immutable, normalized vector paths.
 * Coordinate space is defined over a normalized 500x1000 grid (x: 0..500, y: 0..1000).
 */
object AnatomyGeometry {

    fun buildPath(block: Path.() -> Unit): Path {
        return Path().apply {
            block()
            close()
        }
    }
}
