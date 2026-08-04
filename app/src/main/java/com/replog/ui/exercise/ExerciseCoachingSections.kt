package com.replog.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replog.domain.library.CoachingInfo
import com.replog.ui.components.RepLogCard

/**
 * Premium educational coaching sections — RC44
 * Zero animation. All sections always visible for educational clarity.
 */
@Composable
fun CoachingCueSections(
    coaching: CoachingInfo,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Setup — always visible
        PremiumSectionCard(title = "SETUP", icon = "📍") {
            coaching.steps.forEachIndexed { i, s ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("${i + 1}.  ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(s, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }

        // Execution — always visible
        PremiumSectionCard(title = "EXECUTION", icon = "💪") {
            Text(coaching.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("Movement direction:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(coaching.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Lockout — derived from coaching steps/final position
        PremiumSectionCard(title = "LOCKOUT", icon = "🔒") {
            val lockoutText = when {
                coaching.steps.isNotEmpty() -> coaching.steps.lastOrNull()
                else -> "Hold the contraction briefly at peak range."
            } ?: "Hold the contraction briefly at peak range."
            Text("Finish position: $lockoutText", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Hold the contraction for 1-2 seconds to maximise muscle activation before beginning the return phase.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Breathing — always visible
        PremiumSectionCard(title = "BREATHING", icon = "🫁") {
            Text(coaching.breathing, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Exhale during exertion. Inhale during the return. Never hold your breath under load.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Bracing — always visible
        PremiumSectionCard(title = "BRACING", icon = "🧱") {
            Text("Brace your core before every rep. Take a sharp breath in, hold gently, and maintain tension throughout the movement.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("This protects your spine and improves force transfer.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Grip — always visible
        PremiumSectionCard(title = "GRIP", icon = "✋") {
            Text("Use a secure, neutral grip. Avoid a 'suicide' (thumbs-around) grip on barbell pressing. For dumbbells, maintain a neutral wrist position.", style = MaterialTheme.typography.bodyMedium)
        }

        // Foot Position — always visible
        PremiumSectionCard(title = "FOOT POSITION", icon = "👣") {
            val footNote = when {
                coaching.steps.any { it.contains("foot", true) || it.contains("heel", true) || it.contains("toe", true) } ->
                    coaching.steps.filter { it.contains("foot", true) || it.contains("heel", true) || it.contains("toe", true) }.joinToString(" ")
                else -> "Plant feet firmly at shoulder width, weight distributed through the mid-foot."
            }
            Text(footNote, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Stable feet create a stable lift. Do not let heels lift.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Common Errors — redesigned with problem / why it matters / how to correct
        PremiumSectionCard(title = "COMMON ERRORS", icon = "⚠️", errorColor = true) {
            coaching.mistakes.forEachIndexed { i, mistake ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    // Problem
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text("✗ PROBLEM:  ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(mistake, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                    // Why it matters
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text("  WHY IT MATTERS:  ", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                        Text("Reduces target-muscle activation, increases injury risk, and compromises movement quality.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    }
                    // How to correct
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text("  FIX:  ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        val fix = generateFixFromMistake(mistake)
                        Text(fix, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Safety — always visible
        PremiumSectionCard(title = "SAFETY", icon = "🛡️", errorColor = true) {
            coaching.safety.forEach { advice ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("•  ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(advice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }

        // Advanced Tips — always visible
        PremiumSectionCard(title = "ADVANCED TIPS", icon = "🚀") {
            Text("Slow down the eccentric (lowering) phase to 3-4 seconds for increased time under tension. Focus on squeezing the target muscle at the peak contraction rather than moving the load quickly.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Use a full range of motion unless it causes discomfort. Partial ranges reduce total activation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Recovery Tips — always visible
        PremiumSectionCard(title = "RECOVERY TIPS", icon = "🔄") {
            Text("Rest 48-72 hours before training the same muscle group at high intensity. Prioritise protein intake (1.6-2.2 g per kg bodyweight) and 7-9 hours of sleep for optimal repair.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Helper to generate a correction instruction from a common mistake text. */
private fun generateFixFromMistake(mistake: String): String {
    val lower = mistake.lowercase()
    return when {
        lower.contains("bounce") || lower.contains("swing") || lower.contains("momentum") ->
            "Slow the tempo. Control every inch of the movement. If you must use momentum, reduce the load immediately."
        lower.contains("elbow") || lower.contains("knee") || lower.contains("shoulder") ->
            "Reset your posture: retract shoulders, tuck elbows to 45 degrees, or adjust foot stance before continuing."
        lower.contains("back") || lower.contains("spine") || lower.contains("round") ->
            "Lower the load. Brace your core with a sharp breath, and maintain a neutral spine from neck to pelvis."
        lower.contains("range") || lower.contains("full") || lower.contains("deep") ->
            "Reduce depth or load until mobility improves. Work within a pain-free range."
        lower.contains("grip") || lower.contains("wrist") ->
            "Adjust hand position. Use a secure neutral grip. Wrap thumbs around the bar for pressing."
        lower.contains("speed") || lower.contains("fast") || lower.contains("rush") ->
            "Count 2 seconds down, 1 second hold, 2 seconds up. Never sacrifice form for speed."
        else -> "Reduce load by 20-30%, re-establish posture and tempo, and rebuild progressively."
    }
}

@Composable
private fun PremiumSectionCard(
    title: String,
    icon: String,
    modifier: Modifier = Modifier,
    errorColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val titleColor = if (errorColor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    RepLogCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(icon, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(start = 6.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = titleColor)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}
