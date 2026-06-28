package com.replog.ui.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton
import com.replog.ui.components.formatWeight
import com.replog.util.PlateCalculator

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onOpenDisclaimer: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenAcceptanceHistory: () -> Unit = {},
    onOpenLicences: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var confirmLocalRestore by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (json != null) pendingImportJson = json
        }
    }

    // P6: import a shared .replogtemplate file.
    val importTemplateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (json != null) viewModel.importTemplateJson(json)
        }
    }

    // Storage Access Framework: let the user choose & persist an export folder.
    val pickFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val label = folderLabelFromTreeUri(uri.toString())
            viewModel.setExportFolder(uri.toString(), label)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            if (state.isBusy) CircularProgressIndicator(Modifier.width(24.dp).height(24.dp))
        }

        RepLogCard {
            Row {
                Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Everything is included", fontWeight = FontWeight.Bold)
                    Text(
                        "RepLog has no paywall. Templates, analytics, exports, backups, plate calculator and all future local tools are available from the start.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        RepLogCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Units", fontWeight = FontWeight.Bold)
                    Text(if (state.useKg) "Kilograms" else "Pounds", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(state.useKg, viewModel::setUseKg)
            }
        }

        // Phase 1: which field opens focused (with the keyboard up) when logging a set.
        RepLogCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Fast logging focus", fontWeight = FontWeight.Bold)
                    Text("Which field opens focused when you log a set.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = state.autoFocusField == "weight", onClick = { viewModel.setAutoFocusField("weight") }, label = { Text("Focus Weight") })
                FilterChip(selected = state.autoFocusField == "reps", onClick = { viewModel.setAutoFocusField("reps") }, label = { Text("Focus Reps") })
            }
        }

        RestTimerSettingsCard(
            presets = state.restPresets,
            onChange = viewModel::setRestPresets
        )

        PlateCalculatorCard(
            useKg = state.useKg,
            customKgPlates = state.customKgPlates,
            customLbPlates = state.customLbPlates,
            onCustomKgPlatesChange = viewModel::setCustomKgPlates,
            onCustomLbPlatesChange = viewModel::setCustomLbPlates
        )

        RepLogCard {
            Row {
                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Export folder", fontWeight = FontWeight.Bold)
                    Text("Current: ${state.exportFolderLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Change", Modifier.weight(1f), enabled = !state.isBusy) { pickFolderLauncher.launch(null) }
                SecondaryButton("Use Downloads", Modifier.weight(1f), enabled = !state.isBusy) { viewModel.resetExportFolder() }
            }
        }

        RepLogCard {
            Row {
                Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("CSV export", fontWeight = FontWeight.Bold)
                    Text("Saves a spreadsheet-friendly file to ${state.exportFolderLabel}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Export CSV", enabled = !state.isBusy) { viewModel.exportCsv() }
            if (state.latestCsvFileName != null) {
                ExportSuccessBlock(
                    fileName = state.latestCsvFileName!!,
                    location = state.latestCsvLocation ?: state.exportFolderLabel,
                    onOpenFolder = { openDownloads(context) },
                    onShare = { state.latestCsvShareUri?.let { shareUri(context, it, "text/csv") } },
                    shareEnabled = state.latestCsvShareUri != null
                )
            }
            state.exportStatus?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        RepLogCard {
            Row {
                Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("JSON backup and restore", fontWeight = FontWeight.Bold)
                    Text("Saves a full backup to ${state.exportFolderLabel}; import or restore later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Export JSON", enabled = !state.isBusy) { viewModel.exportJsonBackup() }
            if (state.latestJsonFileName != null) {
                ExportSuccessBlock(
                    fileName = state.latestJsonFileName!!,
                    location = state.latestJsonLocation ?: state.exportFolderLabel,
                    onOpenFolder = { openDownloads(context) },
                    onShare = { state.latestJsonShareUri?.let { shareUri(context, it, "application/json") } },
                    shareEnabled = state.latestJsonShareUri != null
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Import File", Modifier.weight(1f), enabled = !state.isBusy) {
                    importJsonLauncher.launch(arrayOf("application/json", "text/*"))
                }
                SecondaryButton("Restore Local", Modifier.weight(1f), enabled = !state.isBusy) { confirmLocalRestore = true }
            }
            state.backupStatus?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // P6: Templates - import a shared .replogtemplate file (sharing happens
        // from the Training screen per template).
        RepLogCard {
            Row {
                Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Templates", fontWeight = FontWeight.Bold)
                    Text("Import a workout template shared by another RepLog user (.replogtemplate). Share your own from the Training screen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Import Template", enabled = !state.isBusy) {
                importTemplateLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*", "*/*"))
            }
            state.templateStatus?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        RepLogCard {
            Row {
                Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Screenshot demo data", fontWeight = FontWeight.Bold)
                    Text(
                        "Generate realistic local demo workouts, personal bests, supersets and bodyweight logs for QA or store screenshots.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Generate Demo Data", enabled = !state.isBusy) { viewModel.generateDemoData() }
        }

        // P0 #4: Danger Zone - bulk delete with a typed confirmation.
        RepLogCard {
            Row {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Advanced", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Permanently delete all workout history. This cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Delete All Workout History", enabled = !state.isBusy) { showDeleteAllDialog = true }
        }

        RepLogCard {
            Row {
                Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Data ownership", fontWeight = FontWeight.Bold)
                    Text(
                        "RepLog is offline-first. Your data is local, exportable, backup-friendly and never locked behind a subscription.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Sprint 12: Legal Centre ---
        Text("Legal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
        RepLogCard(onClick = onOpenDisclaimer) {
            Text("Health & Safety Disclaimer", fontWeight = FontWeight.Bold)
            Text("Read the disclaimer you accepted.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        RepLogCard(onClick = onOpenTerms) {
            Text("Terms of Use", fontWeight = FontWeight.Bold)
            Text("Read the terms you accepted.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        RepLogCard(onClick = onOpenPrivacy) {
            Text("Privacy Policy", fontWeight = FontWeight.Bold)
            Text("How RepLog handles your data (it stays on your device).", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        RepLogCard(onClick = onOpenAcceptanceHistory) {
            Text("Acceptance History", fontWeight = FontWeight.Bold)
            Text("When you accepted each document version.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        RepLogCard(onClick = onOpenLicences) {
            Text("Open Source Licences", fontWeight = FontWeight.Bold)
            Text("The open source components RepLog is built with.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        RepLogCard {
            Text("App Version", fontWeight = FontWeight.Bold)
            Text("RepLog ${com.replog.util.AppInfo.versionName} (build ${com.replog.util.AppInfo.versionCode})", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        RepLogCard {
            Row {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Release checklist", fontWeight = FontWeight.Bold)
                    Text(
                        "Before store release: replace launcher artwork, run clean release builds, test migration, export/import, onboarding and active workout recovery.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    pendingImportJson?.let { json ->
        RestoreConfirmDialog(
            title = "Import JSON backup?",
            message = "This will merge the selected backup into your current data. Existing matching workouts are not removed, so importing the same file twice may create duplicates.",
            onCancel = { pendingImportJson = null },
            onConfirm = {
                viewModel.restoreJsonText(json)
                pendingImportJson = null
            }
        )
    }

    if (confirmLocalRestore) {
        RestoreConfirmDialog(
            title = "Restore local backup?",
            message = "This will merge the latest local JSON backup into your current data. Existing data is kept, so duplicates are possible if the backup was already restored.",
            onCancel = { confirmLocalRestore = false },
            onConfirm = {
                viewModel.restoreLatestJsonBackup()
                confirmLocalRestore = false
            }
        )
    }

    if (showDeleteAllDialog) {
        DeleteAllHistoryDialog(
            onCancel = { showDeleteAllDialog = false },
            onConfirm = {
                viewModel.deleteAllHistory()
                showDeleteAllDialog = false
            }
        )
    }
}

@Composable
private fun DeleteAllHistoryDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    val confirmed = typed.trim().equals("DELETE", ignoreCase = false)
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete all workout history?") },
        text = {
            Column {
                Text("This removes:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("- Workouts", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("- Sets", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("- Progress records", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("- Training DNA history", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("- Recovery history", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Templates, exercises and settings are kept.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text("This cannot be undone. Type DELETE to confirm.", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    placeholder = { Text("DELETE") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmed) {
                Text("Delete Everything", color = if (confirmed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

/** Best-effort human label for a SAF tree URI (e.g. "primary:Download/RepLog"). */
private fun folderLabelFromTreeUri(treeUri: String): String {
    val decoded = android.net.Uri.decode(treeUri)
    val afterColon = decoded.substringAfterLast(':', "")
    return when {
        afterColon.isNotBlank() -> afterColon
        else -> "Selected folder"
    }
}

@Composable
private fun ExportSuccessBlock(
    fileName: String,
    location: String,
    onOpenFolder: () -> Unit,
    onShare: () -> Unit,
    shareEnabled: Boolean
) {
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("Export complete", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(6.dp))
    Text("File saved", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(fileName, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text("Location", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(location, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton("Open Folder", Modifier.weight(1f), onClick = onOpenFolder)
        SecondaryButton("Share File", Modifier.weight(1f), enabled = shareEnabled, onClick = onShare)
    }
}

@Composable
private fun RestoreConfirmDialog(
    title: String,
    message: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

@Composable
private fun PlateCalculatorCard(
    useKg: Boolean,
    customKgPlates: String,
    customLbPlates: String,
    onCustomKgPlatesChange: (String) -> Unit,
    onCustomLbPlatesChange: (String) -> Unit
) {
    var target by remember(useKg) { mutableStateOf(if (useKg) "100" else "225") }
    var bar by remember(useKg) { mutableStateOf(if (useKg) "20" else "45") }
    var customInput by remember(useKg, customKgPlates, customLbPlates) { mutableStateOf(if (useKg) customKgPlates else customLbPlates) }
    val targetWeight = target.toDoubleOrNull() ?: 0.0
    val barWeight = bar.toDoubleOrNull() ?: if (useKg) 20.0 else 45.0
    val customPlates = PlateCalculator.parsePlates(customInput)
    val load = PlateCalculator.calculate(targetWeight, barWeight, useKg, customPlates)

    RepLogCard {
        Row {
            Icon(Icons.Default.Calculate, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Advanced plate calculator", fontWeight = FontWeight.Bold)
                Text("Use your gym's actual plates. Separate values with commas or spaces.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = target,
                onValueChange = { target = it.filter { char -> char.isDigit() || char == '.' } },
                modifier = Modifier.weight(1f),
                label = { Text("Target") },
                suffix = { Text(if (useKg) "kg" else "lb") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            OutlinedTextField(
                value = bar,
                onValueChange = { bar = it.filter { char -> char.isDigit() || char == '.' } },
                modifier = Modifier.weight(1f),
                label = { Text("Bar") },
                suffix = { Text(if (useKg) "kg" else "lb") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = customInput,
            onValueChange = {
                customInput = it
                if (useKg) onCustomKgPlatesChange(it) else onCustomLbPlatesChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Available plates per side") },
            placeholder = { Text(if (useKg) "25, 20, 15, 10, 5, 2.5, 1.25" else "45, 35, 25, 10, 5, 2.5") },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))
        Text("Each side", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            if (load.platesPerSide.isEmpty()) "No plates needed" else load.platesPerSide.joinToString(" + ") { clean(it) },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Loaded: ${formatWeight(load.loadedWeight, useKg)}" + if (kotlin.math.abs(load.remainingWeight) > 0.01) " • Remaining: ${clean(load.remainingWeight)}" else "",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun RestTimerSettingsCard(
    presets: com.replog.util.timer.RestPresets,
    onChange: (com.replog.util.timer.RestPresets) -> Unit
) {
    var compound by remember(presets.compoundSeconds) { mutableStateOf(presets.compoundSeconds.toString()) }
    var isolation by remember(presets.isolationSeconds) { mutableStateOf(presets.isolationSeconds.toString()) }
    var bodyweight by remember(presets.bodyweightSeconds) { mutableStateOf(presets.bodyweightSeconds.toString()) }

    fun commit() {
        onChange(
            presets.copy(
                compoundSeconds = compound.toIntOrNull()?.coerceIn(15,600) ?: presets.compoundSeconds,
                isolationSeconds = isolation.toIntOrNull()?.coerceIn(15,600) ?: presets.isolationSeconds,
                bodyweightSeconds = bodyweight.toIntOrNull()?.coerceIn(15,600) ?: presets.bodyweightSeconds
            )
        )
    }
    RepLogCard {
        Row { Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column { Text("Rest timer presets", fontWeight = FontWeight.Bold); Text("Smart rest times automatically applied after each set.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(compound, { compound = it.filter(Char::isDigit); commit() }, Modifier.fillMaxWidth(), label = { Text("Compound (Barbell, Squat, Press)") }, suffix = { Text("sec") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(isolation, { isolation = it.filter(Char::isDigit); commit() }, Modifier.fillMaxWidth(), label = { Text("Isolation") }, suffix = { Text("sec") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(bodyweight, { bodyweight = it.filter(Char::isDigit); commit() }, Modifier.fillMaxWidth(), label = { Text("Bodyweight") }, suffix = { Text("sec") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        Spacer(Modifier.height(6.dp))
        Text("Per-exercise overrides supported in data layer – UI editor coming in V2.1.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun shareUri(context: Context, uriString: String, mimeType: String) {
    val uri = android.net.Uri.parse(uriString)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share RepLog export"))
}

/** Open the system Downloads/Files view so the user can find their export. */
private fun openDownloads(context: Context) {
    val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setType("*/*")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
}

private fun clean(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
