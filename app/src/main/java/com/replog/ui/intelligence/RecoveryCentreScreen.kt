package com.replog.ui.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import com.replog.data.repository.MuscleRecoveryUi
import com.replog.data.repository.RecoveryCentreData
import com.replog.domain.recovery.RecoveryCalendarDay
import com.replog.domain.recovery.RecoveryDay
import com.replog.ui.components.EmptyState
import com.replog.ui.components.LoadingState
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.StatCard

@Composable
fun RecoveryCentreScreen(
    contentPadding: PaddingValues,
    viewModel: RecoveryCentreViewModel = hiltViewModel()
) {
    val data by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Recovery Centre", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("How recovered you are, and what to train today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val d = data
        when {
            d == null -> item { LoadingState("Reading your recovery") }
            !d.hasData -> item { EmptyState("Not enough data yet", "Finish a few more workouts and RepLog will track your recovery.") }
            else -> recoveryContent(d)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recoveryContent(d: RecoveryCentreData) {
    item {
        RepLogCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Default.HealthAndSafety, null, tint = recoveryColor(d.score))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Overall recovery", fontWeight = FontWeight.Bold)
                    Text(d.statusLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${d.score}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = recoveryColor(d.score))
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { d.score / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = recoveryColor(d.score))
            Spacer(Modifier.height(8.dp))
            Text(d.directive, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            if (d.directiveDetail.isNotBlank()) {
                Text(d.directiveDetail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Today / Tomorrow / 48h projection.
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Today", "${d.todayScore}%", Modifier.weight(1f))
            StatCard("Tomorrow", "${d.tomorrowScore}%", Modifier.weight(1f))
            StatCard("48 hours", "${d.in48hScore}%", Modifier.weight(1f))
        }
    }

    // Suggested session.
    item {
        RepLogCard {
            Text("Suggested session", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Intensity: ${d.suggestedIntensity}", style = MaterialTheme.typography.bodyMedium)
            if (d.suggestedDurationMinutes > 0) Text("Duration: ~${d.suggestedDurationMinutes} min", style = MaterialTheme.typography.bodyMedium)
            Text("Type: ${d.suggestedType}", style = MaterialTheme.typography.bodyMedium)
            if (d.estimatedFullRecoveryHours > 0) {
                Spacer(Modifier.height(4.dp))
                Text("Estimated full recovery: ~${d.estimatedFullRecoveryHours}h", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Recovery timeline / calendar.
    if (d.calendar.any { it.state != RecoveryDay.REST_NO_DATA }) {
        item {
            RepLogCard {
                Text("Recovery timeline", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    d.calendar.takeLast(14).forEach { day -> CalendarDot(day) }
                }
            }
        }
    }

    if (d.recovered.isNotEmpty()) {
        item { Text("Recovered muscle groups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(d.recovered, key = { "r-${it.muscle}" }) { MuscleRow(it, ready = true) }
    }
    if (d.fatigued.isNotEmpty()) {
        item { Text("Still recovering", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(d.fatigued, key = { "f-${it.muscle}" }) { MuscleRow(it, ready = false) }
    }

    if (d.factors.isNotEmpty()) {
        item {
            RepLogCard {
                Text("Why? Recovery factors", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                d.factors.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
    if (d.improvements.isNotEmpty()) {
        item {
            RepLogCard {
                Text("Improvements", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                d.improvements.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
    if (d.warnings.isNotEmpty()) {
        item {
            RepLogCard {
                Text("Warnings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(6.dp))
                d.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun MuscleRow(m: MuscleRecoveryUi, ready: Boolean) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(m.muscle, fontWeight = FontWeight.SemiBold)
            Text(
                if (ready) "Ready to train" else "~${m.hoursUntilReady}h until ready",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("${m.score}%", fontWeight = FontWeight.Bold, color = recoveryColor(m.score))
    }
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(progress = { m.score / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = recoveryColor(m.score))
}

@Composable
private fun CalendarDot(day: RecoveryCalendarDay) {
    val c = when (day.state) {
        RecoveryDay.READY -> com.replog.ui.theme.RepLogSuccess
        RecoveryDay.CAUTION -> MaterialTheme.colorScheme.tertiary
        RecoveryDay.RECOVERING -> MaterialTheme.colorScheme.error
        RecoveryDay.REST_NO_DATA -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = c, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(20.dp)) {}
        Text(day.dayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun recoveryColor(score: Int): Color = when {
    score >= 80 -> com.replog.ui.theme.RepLogSuccess
    score >= 60 -> MaterialTheme.colorScheme.primary
    score >= 45 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}
