package com.replog.ui.home.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.data.repository.DnaEvolutionData
import com.replog.ui.components.RepLogCard

// Phase 3 Gap 6 — Training DNA Evolution Card

@Composable
fun DnaEvolutionCard(data: DnaEvolutionData) = RepLogCard {
    val latest = data.points.lastOrNull()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Training DNA",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (data.genomeMaturity) {
                    "Mature" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                    "Developing" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                    else -> Color(0xFF2196F3).copy(alpha = 0.15f)
                }
            ) {
                Text(
                    data.genomeMaturity,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when (data.genomeMaturity) {
                        "Mature" -> Color(0xFF4CAF50)
                        "Developing" -> Color(0xFFFF9800)
                        else -> Color(0xFF2196F3)
                    }
                )
            }
        }

        if (latest != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DnaStat(
                    label = "Rep Range",
                    value = latest.preferredRepRange,
                    modifier = Modifier.weight(1f)
                )
                DnaStat(
                    label = "Recovery",
                    value = "${latest.recoveryHours}h",
                    modifier = Modifier.weight(1f)
                )
                DnaStat(
                    label = "Avg Duration",
                    value = "${latest.workoutDurationMinutes}min",
                    modifier = Modifier.weight(1f)
                )
            }

            if (data.points.size >= 2) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DnaStat(
                        label = "Trend",
                        value = data.consistencyTrend,
                        modifier = Modifier.weight(1f)
                    )
                    DnaStat(
                        label = "Vol. Tolerance",
                        value = "${latest.volumeTolerance}/100",
                        modifier = Modifier.weight(1f)
                    )
                    DnaStat(
                        label = "PRs (30d)",
                        value = "${latest.monthlyPrCount}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DnaStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
