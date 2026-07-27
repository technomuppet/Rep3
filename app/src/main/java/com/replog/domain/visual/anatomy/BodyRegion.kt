package com.replog.domain.visual.anatomy

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Modern, premium modular body region representation — Version 2 Visual Engine
 * Separates anatomy completely from the renderer.
 * Each body region has its own independent vector path, visibility, color palettes,
 * and dynamic activation level.
 */
data class BodyRegion(
    val id: MuscleRegion,
    val path: Path,
    val isFront: Boolean,
    val primaryColor: Color = Color(0xFF1E88E5), // Electric Blue
    val secondaryColor: Color = Color(0xFF00ACC1), // Deep Cyan
    val activationLevel: Float = 0f
)
