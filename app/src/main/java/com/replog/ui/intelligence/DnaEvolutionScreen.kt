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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.data.repository.DnaEvolutionData
import com.replog.data.repository.DnaEvolutionPoint
import com.replog.ui.components.EmptyState
import com.replog.ui.components.LoadingState
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.StatCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DnaEvolutionScreen(
    contentPadding: PaddingValues,
    viewModel: DnaEvolutionViewModel = hiltViewModel()
) {
    val data by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("DNA Evolution", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("How your training has adapted over time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val d = data
        when {
            d == null -> item { LoadingState("Loading your history") }
            !d.hasData || d.points.size < 2 -> item {
                EmptyState("Evolution builds over time", "RepLog saves a Training DNA snapshot as you train. Come back after a few more weeks to see how you adapt.")
            }
            else -> evolutionContent(d)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.evolutionContent(d: DnaEvolutionData) {
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Genome maturity", d.genomeMaturity, Modifier.weight(1f))
            StatCard("Snapshots", d.points.size.toString(), Modifier.weight(1f))
        }
    }
    item {
        RepLogCard {
            Text("Consistency trend", fontWeight = FontWeight.Bold)
            Text(d.consistencyTrend, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    val first = d.points.first()
    val last = d.points.last()
    item { TrendCard("Preferred rep range", first.preferredRepRange, last.preferredRepRange) }
    item { TrendCard("Workout duration", "${first.workoutDurationMinutes} min", "${last.workoutDurationMinutes} min") }
    item { TrendCard("Recovery ability", "${first.recoveryHours}h", "${last.recoveryHours}h") }
    item { TrendCard("Training frequency", first.frequency, last.frequency) }
    item { TrendCard("Volume tolerance", "${first.volumeTolerance}%", "${last.volumeTolerance}%") }
    item { TrendCard("Monthly PRs", first.monthlyPrCount.toString(), last.monthlyPrCount.toString()) }

    // Volume tolerance sparkline-style timeline.
    item {
        RepLogCard {
            Text("Volume tolerance over time", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            d.points.takeLast(8).forEach { p -> EvolutionBar(p) }
        }
    }
}

@Composable
private fun TrendCard(label: String, from: String, to: String) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("$from → $to", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EvolutionBar(p: DnaEvolutionPoint) {
    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(fmt.format(Date(p.generatedAt)), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(56.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.LinearProgressIndicator(
            progress = { (p.volumeTolerance / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(10.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("${p.volumeTolerance}%", style = MaterialTheme.typography.labelMedium)
    }
    Spacer(Modifier.height(6.dp))
}
