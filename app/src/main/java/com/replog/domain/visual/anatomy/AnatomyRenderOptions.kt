package com.replog.domain.visual.anatomy

/**
 * Forward-compatible render options for future anatomy overlays.
 *
 * Current behaviour intentionally remains simple: base SVG plus active muscle
 * overlays. These fields provide stable extension points for intensity,
 * unilateral highlighting, fatigue/injury overlays and accessibility passes
 * without changing the renderer architecture again.
 */
data class AnatomyRenderOptions(
    val intensityByRegion: Map<MuscleRegion, Float> = emptyMap(),
    val sidePreference: AnatomySidePreference = AnatomySidePreference.BILATERAL,
    val overlayModes: Set<AnatomyOverlayMode> = emptySet(),
    val showSelectionOutlines: Boolean = false,
    val accessibilityHighContrast: Boolean = false
) {
    /** Future render passes should use this to avoid propagating invalid alpha/intensity values. */
    val clampedIntensityByRegion: Map<MuscleRegion, Float> = intensityByRegion.mapValues { (_, value) ->
        if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
    }
}

enum class AnatomySidePreference {
    BILATERAL,
    LEFT,
    RIGHT
}

enum class AnatomyOverlayMode {
    ACTIVATION_INTENSITY,
    FATIGUE,
    INJURY,
    SELECTION,
    ACCESSIBILITY
}
