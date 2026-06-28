package com.replog.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.replog.ui.components.EmptyState
import com.replog.ui.components.RepLogCard
import com.replog.util.AppInfo
import com.replog.util.PreferencesManager
import com.replog.util.legal.LegalAcceptance
import com.replog.util.legal.LegalDocId
import com.replog.util.legal.LegalDocuments
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Read-only viewer for one legal document (reached from the Settings Legal section). */
@Composable
fun LegalDocumentScreen(docId: LegalDocId, contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val doc = LegalDocuments.byId(docId)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(doc.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                "Version ${doc.version}  -  Effective ${doc.effectiveDate}  -  Updated ${doc.lastUpdated}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { RepLogCard { Text(doc.body, style = MaterialTheme.typography.bodyMedium) } }
    }
}

/** ViewModel exposing the persisted acceptance history (read-only). */
@HiltViewModel
class AcceptanceHistoryViewModel @Inject constructor(
    prefs: PreferencesManager
) : ViewModel() {
    val history: StateFlow<List<LegalAcceptance>> = prefs.legalAcceptanceHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Acceptance History screen: date, time, accepted versions and app version. Read-only. */
@Composable
fun AcceptanceHistoryScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: AcceptanceHistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsState()
    val dateFmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    if (history.isEmpty()) {
        EmptyState(title = "No acceptance records yet", message = "Your legal acceptance history will appear here after you accept the documents.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Acceptance History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Every time you accepted the legal documents. Read-only.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(history) { record ->
            RepLogCard {
                val d = Date(record.acceptedAtEpochMillis)
                Text("${dateFmt.format(d)} at ${timeFmt.format(d)}", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Disclaimer v${record.disclaimerVersion}", style = MaterialTheme.typography.bodyMedium)
                Text("Terms of Use v${record.termsVersion}", style = MaterialTheme.typography.bodyMedium)
                Text("Privacy Policy v${record.privacyVersion}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("App version ${record.appVersion}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Accepted by ${record.displayName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Open Source Licences (static list; offline). */
@Composable
fun OpenSourceLicencesScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    val libs = listOf(
        "Jetpack Compose" to "Apache License 2.0",
        "AndroidX (Core, Activity, Lifecycle, Navigation, DataStore, Room)" to "Apache License 2.0",
        "Material Components for Android" to "Apache License 2.0",
        "Dagger Hilt" to "Apache License 2.0",
        "Gson" to "Apache License 2.0",
        "Kotlin Standard Library & Coroutines" to "Apache License 2.0",
        "AndroidX DocumentFile" to "Apache License 2.0"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Open Source Licences", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("RepLog is built with these open source components.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            RepLogCard {
                Text("RepLog ${AppInfo.versionName}", fontWeight = FontWeight.Bold)
                Text("Offline-first. No analytics, advertising or tracking.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(libs) { (name, licence) ->
            RepLogCard {
                Text(name, fontWeight = FontWeight.Bold)
                Text(licence, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
