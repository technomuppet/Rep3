package com.replog.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard

@Composable
fun OnboardingScreen(
    onComplete: (useKg: Boolean) -> Unit
) {
    var useKg by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("RepLog", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
        Text("A serious training log for progressive overload.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))
        OnboardingFeature(
            icon = Icons.Default.FitnessCenter,
            title = "Log fast in the gym",
            message = "Templates, repeat sets, previous performance and rest timers keep logging friction low."
        )
        Spacer(Modifier.height(12.dp))
        OnboardingFeature(
            icon = Icons.Default.Insights,
            title = "See progress clearly",
            message = "Track volume, PRs, estimated 1RM trends and exercise-level analytics."
        )
        Spacer(Modifier.height(12.dp))
        OnboardingFeature(
            icon = Icons.Default.Security,
            title = "Own your data",
            message = "Offline-first storage with CSV and JSON backup options."
        )

        Spacer(Modifier.height(24.dp))
        Text("Choose units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = useKg, onClick = { useKg = true }, label = { Text("Kilograms") })
            FilterChip(selected = !useKg, onClick = { useKg = false }, label = { Text("Pounds") })
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton("Start logging") { onComplete(useKg) }
    }
}

@Composable
private fun OnboardingFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) = RepLogCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
