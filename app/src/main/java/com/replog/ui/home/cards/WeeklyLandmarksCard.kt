package com.replog.ui.home.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.volume.VolumeLandmark
import com.replog.domain.volume.VolumeStatus
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SectionHeader

// Phase 2 Gap 5: dedicated weekly-volume card with its own visual hierarchy.
// Renders the ten hypertrophy groups from `VolumeLandmarks.analyze(weeks = 1)`
// as compact horizontal bars with a colour-coded status badge (UNDER /
// IN_RANGE / ABOVE / NONE). UNDER groups are surfaced first so the user sees
// the gap signal before the in-range noise; the CTA opens the existing
// Muscle Balance drill-down screen (the same route the IntelligenceNavCard
// "Muscle Balance" tile uses).
@Composable
fun WeeklyLandmarksCard(
    landmarks: List<VolumeLandmark>,
    onOpen: () -> Unit
) = RepLogCard {
    SectionHeader("WEEKLY VOLUME", Icons.Default.StackedBarChart)
    Spacer(Modifier.height(4.dp))
    val underCount = landmarks.count {
        it.status == VolumeStatus.UNDER ||
            it.status == VolumeStatus.NONE
    }
    val inRangeCount = landmarks.count {
        it.status == VolumeStatus.IN_RANGE
    }
    val summary = when {
        underCount == 0 && inRangeCount > 0 ->
            "All $inRangeCount trained muscle groups are in their optimal range."
        underCount > 0 && inRangeCount > 0 ->
            "$inRangeCount in range • $underCount below optimal — tap for suggestions."
        underCount > 0 && inRangeCount == 0 ->
            "$underCount groups need attention — tap for suggestions."
        else -> "Tap for full weekly volume analysis."
    }
    Text(
        summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(10.dp))
    // Priority order: UNDER first (action signal), then ABOVE, then IN_RANGE,
    // then NONE, so the user sees the actionable rows at the top of the card.
    val ordered = landmarks.sortedWith(
        compareBy<VolumeLandmark> { it.status.sortOrder }.thenBy { it.muscleGroup }
    )
    ordered.forEach { lm ->
        WeeklyLandmarkRow(lm)
        Spacer(Modifier.height(4.dp))
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Text("Open Muscle Balance →", fontWeight = FontWeight.Bold)
    }
}

/** One horizontal-bar muscle-group row in the WeeklyLandmarksCard. */
@Composable
fun WeeklyLandmarkRow(lm: VolumeLandmark) {
    val barColor = lm.status.accent
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fraction = if (lm.optimalHigh > 0) {
        (lm.weeklySets / lm.optimalHigh).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            lm.muscleGroup,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(88.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.height(10.dp).weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(trackColor, RoundedCornerShape(5.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(barColor, RoundedCornerShape(5.dp))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            when (lm.status) {
                VolumeStatus.NONE -> "—"
                else -> "${lm.weeklySets.toInt()} / ${lm.optimalLow}-${lm.optimalHigh}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Spacer(Modifier.width(8.dp))
        WeeklyStatusBadge(lm.status)
    }
}

/** Compact coloured pill showing the VolumeStatus label for one landmark. */
@Composable
fun WeeklyStatusBadge(status: VolumeStatus) {
    val fg = status.accent
    val bg = status.background
    Text(
        status.label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
