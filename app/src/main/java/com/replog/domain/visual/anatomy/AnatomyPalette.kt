package com.replog.domain.visual.anatomy

import androidx.compose.ui.graphics.Color

/**
 * Reusable color and style system for anatomical muscle rendering.
 * Supports light and dark theme compatibility with accessibility-compliant
 * contrast for primary, secondary, stabiliser, and silhouette layers.
 */
data class AnatomyPalette(
    val bodyFill: Color,
    val bodyOutline: Color,
    val primaryFill: Color,
    val primaryOutline: Color,
    val secondaryFill: Color,
    val secondaryOutline: Color,
    val stabiliserFill: Color,
    val stabiliserOutline: Color,
    val background: Color
) {
    companion object {
        fun default(isDarkTheme: Boolean = true): AnatomyPalette = if (isDarkTheme) {
            AnatomyPalette(
                bodyFill = Color(0xFF1E293B),        // Slate 800
                bodyOutline = Color(0xFF64748B),     // Slate 500
                primaryFill = Color(0xFFEF4444),     // Red 500
                primaryOutline = Color(0xFFFCA5A5),  // Red 300
                secondaryFill = Color(0xFFF97316),   // Orange 500 (with opacity in renderer)
                secondaryOutline = Color(0xFFFDBA74),// Orange 300
                stabiliserFill = Color(0xFF3B82F6),  // Blue 500
                stabiliserOutline = Color(0xFF93C5FD),// Blue 300
                background = Color.Transparent
            )
        } else {
            AnatomyPalette(
                bodyFill = Color(0xFFE2E8F0),        // Slate 200
                bodyOutline = Color(0xFF475569),     // Slate 600
                primaryFill = Color(0xFFDC2626),     // Red 600
                primaryOutline = Color(0xFF991B1B),  // Red 800
                secondaryFill = Color(0xFFEA580C),   // Orange 600
                secondaryOutline = Color(0xFFC2410C),// Orange 700
                stabiliserFill = Color(0xFF2563EB),  // Blue 600
                stabiliserOutline = Color(0xFF1E40AF),// Blue 800
                background = Color.Transparent
            )
        }
    }
}
