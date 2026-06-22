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
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.replog.ui.components.PrimaryButton
import com.replog.ui.components.RepLogCard
import com.replog.ui.components.SecondaryButton
import com.replog.ui.components.formatWeight
import com.replog.util.PlateCalculator
import java.io.File

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var confirmLocalRestore by remember { mutableStateOf(false) }

    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (json != null) pendingImportJson = json
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
                Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("CSV export", fontWeight = FontWeight.Bold)
                    Text("Generate and share a spreadsheet-friendly workout export.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Generate", Modifier.weight(1f), enabled = !state.isBusy) { viewModel.exportCsv() }
                SecondaryButton("Share", Modifier.weight(1f), enabled = state.latestCsvPath != null) {
                    state.latestCsvPath?.let { shareFile(context, it, "text/csv") }
                }
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
                    Text("Backup, share, import, and restore structured RepLog data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Backup", Modifier.weight(1f), enabled = !state.isBusy) { viewModel.exportJsonBackup() }
                SecondaryButton("Share", Modifier.weight(1f), enabled = state.latestJsonPath != null) {
                    state.latestJsonPath?.let { shareFile(context, it, "application/json") }
                }
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

        RepLogCard {
            Row {
                Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Screenshot demo data", fontWeight = FontWeight.Bold)
                    Text(
                        "Generate realistic local demo workouts, PRs, supersets and bodyweight logs for QA or store screenshots.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Generate Demo Data", enabled = !state.isBusy) { viewModel.generateDemoData() }
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

private fun shareFile(context: Context, path: String, mimeType: String) {
    val file = File(path)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share RepLog export"))
}

private fun clean(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
