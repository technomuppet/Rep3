package com.replog.ui.home.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.recovery.RecoveryCalendarDay
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SectionHeader

// Phase 3 Gap 4: compact 7-day recovery forecast strip.
@Composable
fun RecoveryCalendarStrip(days: List<RecoveryCalendarDay>) = RepLogCard {
    SectionHeader("7-DAY RECOVERY", Icons.Default.Healing)
    Spacer(Modifier.height(10.dp))
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        days.forEach { day ->
            val bg = day.state.background
            val fg = day.state.accent
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(day.dayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(bg, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (day.trainedToday) FontWeight.ExtraBold else FontWeight.Bold,
                        color = fg
                    )
                }
            }
        }
    }
}
