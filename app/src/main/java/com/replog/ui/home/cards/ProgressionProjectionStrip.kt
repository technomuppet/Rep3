package com.replog.ui.home.cards

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.data.repository.ForecastCardEntry
import com.replog.domain.forecast.ForecastConfidence
import com.replog.domain.forecast.ForecastTrend
import com.replog.ui.components.RepLogCard

// Phase 3 Gap 5 — Progression Projection Strip

@Composable
fun ProgressionProjectionStrip(entries: List<ForecastCardEntry>) = RepLogCard {
    Column {
        Text(
            "Next 4 Weeks",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEach { entry ->
                val trendColor = when (entry.trend) {
                    ForecastTrend.RISING -> Color(0xFF4CAF50)
                    ForecastTrend.FLAT -> Color(0xFFFF9800)
                    ForecastTrend.DECLINING -> Color(0xFFE53935)
                }
                val trendArrow = when (entry.trend) {
                    ForecastTrend.RISING -> "▲"
                    ForecastTrend.FLAT -> "—"
                    ForecastTrend.DECLINING -> "▼"
                }
                val confidenceAlpha = when (entry.confidence) {
                    ForecastConfidence.HIGH -> 1.0f
                    ForecastConfidence.MEDIUM -> 0.7f
                    ForecastConfidence.LOW -> 0.45f
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.alpha(confidenceAlpha)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            entry.exerciseName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            entry.projectionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            trendArrow,
                            color = trendColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
