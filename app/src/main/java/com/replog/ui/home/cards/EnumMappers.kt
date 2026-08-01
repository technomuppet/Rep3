package com.replog.ui.home.cards

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.replog.domain.forecast.ForecastConfidence
import com.replog.domain.forecast.ForecastTrend
import com.replog.domain.recovery.RecoveryDay
import com.replog.domain.volume.VolumeStatus

/**
 * Single home for the enum → colour/label/icon rules the Home cards render,
 * so the same decision is not re-implemented as copy-pasted `when` blocks in
 * every card file. Lives in the UI layer to keep the domain layer pure.
 */

// ---- VolumeStatus -----------------------------------------------------

/** Display order — actionable (UNDER/NONE) groups first, then ABOVE, then IN_RANGE. */
val VolumeStatus.sortOrder: Int
    get() = when (this) {
        VolumeStatus.UNDER, VolumeStatus.NONE -> 0
        VolumeStatus.ABOVE -> 1
        VolumeStatus.IN_RANGE -> 2
    }

/** Accent colour for bars + status text. */
val VolumeStatus.accent: Color
    @Composable get() = when (this) {
        VolumeStatus.IN_RANGE -> MaterialTheme.colorScheme.primary
        VolumeStatus.UNDER -> MaterialTheme.colorScheme.tertiary
        VolumeStatus.ABOVE -> MaterialTheme.colorScheme.secondary
        VolumeStatus.NONE -> MaterialTheme.colorScheme.outline
    }

/** Subtle pill background for the status badge. */
val VolumeStatus.background: Color
    @Composable get() = when (this) {
        VolumeStatus.IN_RANGE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        VolumeStatus.UNDER -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        VolumeStatus.ABOVE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        VolumeStatus.NONE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }

// ---- ForecastTrend ----------------------------------------------------

/** Plain-English trend headline, e.g. "Rising". */
val ForecastTrend.label: String
    get() = when (this) {
        ForecastTrend.RISING -> "Rising"
        ForecastTrend.FLAT -> "Flat"
        ForecastTrend.DECLINING -> "Declining"
    }

/** Direction arrow glyph shared by the forecast card and projection strip. */
val ForecastTrend.glyph: String
    get() = when (this) {
        ForecastTrend.RISING -> "▲"
        ForecastTrend.FLAT -> "—"
        ForecastTrend.DECLINING -> "▼"
    }

/** Trend arrow colour — fixed brand palette, deliberately not theme colours. */
val ForecastTrend.color: Color
    get() = when (this) {
        ForecastTrend.RISING -> Color(0xFF4CAF50)
        ForecastTrend.FLAT -> Color(0xFFFF9800)
        ForecastTrend.DECLINING -> Color(0xFFE53935)
    }

// ---- ForecastConfidence ----------------------------------------------

/** Plain-English confidence label, e.g. "High". */
val ForecastConfidence.label: String
    get() = when (this) {
        ForecastConfidence.HIGH -> "High"
        ForecastConfidence.MEDIUM -> "Medium"
        ForecastConfidence.LOW -> "Low"
    }

/** Accent colour for the confidence label. */
val ForecastConfidence.accent: Color
    @Composable get() = when (this) {
        ForecastConfidence.HIGH -> MaterialTheme.colorScheme.primary
        ForecastConfidence.MEDIUM -> MaterialTheme.colorScheme.tertiary
        ForecastConfidence.LOW -> MaterialTheme.colorScheme.outline
    }

/** Opacity used to de-emphasise low-confidence projections. */
val ForecastConfidence.alpha: Float
    get() = when (this) {
        ForecastConfidence.HIGH -> 1.0f
        ForecastConfidence.MEDIUM -> 0.7f
        ForecastConfidence.LOW -> 0.45f
    }

// ---- RecoveryDay ------------------------------------------------------

/** Accent colour for a recovery-day chip (text/foreground). */
val RecoveryDay.accent: Color
    @Composable get() = when (this) {
        RecoveryDay.READY -> MaterialTheme.colorScheme.primary
        RecoveryDay.CAUTION -> MaterialTheme.colorScheme.tertiary
        RecoveryDay.RECOVERING -> MaterialTheme.colorScheme.secondary
        RecoveryDay.REST_NO_DATA -> MaterialTheme.colorScheme.outline
    }

/** Chip background tint for a recovery day. */
val RecoveryDay.background: Color
    @Composable get() = when (this) {
        RecoveryDay.READY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        RecoveryDay.CAUTION -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        RecoveryDay.RECOVERING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        RecoveryDay.REST_NO_DATA -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
    }

// ---- Training DNA maturity (string-based, from the snapshot engine) ----

/** Accent colour for the genome-maturity badge. */
fun dnaMaturityAccent(maturity: String): Color = when (maturity) {
    "Mature" -> Color(0xFF4CAF50)
    "Developing" -> Color(0xFFFF9800)
    else -> Color(0xFF2196F3)
}
