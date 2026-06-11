package ar.gov.entrerios.cge.concursos.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import ar.gov.entrerios.cge.concursos.BuildConfig
import ar.gov.entrerios.cge.concursos.R
import ar.gov.entrerios.cge.concursos.core.model.Category
import ar.gov.entrerios.cge.concursos.core.model.DarkMode
import ar.gov.entrerios.cge.concursos.core.model.Departamental
import ar.gov.entrerios.cge.concursos.core.model.SyncMode
import ar.gov.entrerios.cge.concursos.core.util.ConcursoDateFilter
import ar.gov.entrerios.cge.concursos.core.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenUrl: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val privacyUrl = BuildConfig.PRIVACY_POLICY_URL

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_settings))}) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("Modo de actualización")

            SettingCard {
                SyncModeOption(
                    label = "Solo al abrir la app",
                    description = "El chequeo se hace únicamente cuando entrás a la aplicación.",
                    selected = settings.syncMode == SyncMode.ON_APP_OPEN,
                    onClick = { viewModel.setSyncMode(SyncMode.ON_APP_OPEN) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SyncModeOption(
                    label = "Todos los días a una hora",
                    description = "La app revisa las fuentes públicas de concursos automáticamente, incluso cerrada.",
                    selected = settings.syncMode == SyncMode.DAILY,
                    onClick = { viewModel.setSyncMode(SyncMode.DAILY) }
                )

                if (settings.syncMode == SyncMode.DAILY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hora del chequeo", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Tocá para cambiarla.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = "%02d:%02d".format(settings.dailyHour, settings.dailyMinute),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            SectionLabel(stringResource(R.string.settings_category_general))

            SettingCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Avisar cuando aparezcan concursos relevantes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(checked = settings.notificationsEnabled, onCheckedChange = viewModel::setNotifications)
                }
            }

            SettingCard {
                DarkModeSelector(
                    value = settings.darkMode,
                    onChange = viewModel::setDarkMode
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            SectionLabel(stringResource(R.string.settings_days_back_title))

            SettingCard {
                Text(
                    text = stringResource(R.string.settings_days_back_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConcursoDateFilter.presetDaysBack.forEach { days ->
                        FilterChip(
                            selected = settings.syncDaysBack == days,
                            onClick = { viewModel.setSyncDaysBack(days) },
                            label = { Text(stringResource(R.string.settings_days_back_option, days)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            SectionLabel(stringResource(R.string.settings_departamental_title))

            SettingCard {
                DepartamentalSelector(
                    selected = settings.selectedDepartamental,
                    onSelect = viewModel::setDepartamental
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            SectionLabel(stringResource(R.string.settings_category_categories))

            SettingCard {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Category.monitored.forEach { cat ->
                        val selected = settings.monitoredCategories.contains(cat)
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.toggleCategory(cat) },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.settings_legal_section))
            SettingCard {
                Text(
                    text = stringResource(R.string.settings_legal_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onOpenUrl(Constants.BASE_URL + Constants.CONCURSOS_PATH) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_official_source))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        if (privacyUrl.isNotBlank()) {
                            onOpenUrl(privacyUrl)
                        } else {
                            showPrivacyDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_privacy_policy))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = viewModel::forceRefreshNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sincronizar ahora")
            }
        }
    }

    if (showPrivacyDialog) {
        Dialog(onDismissRequest = { showPrivacyDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_privacy_policy),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.privacy_policy_body),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = settings.dailyHour,
            initialMinute = settings.dailyMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                viewModel.setDailyTime(h, m)
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SyncModeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.padding(end = 8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartamentalSelector(
    selected: Departamental,
    onSelect: (Departamental) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.settings_departamental_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_departamental_hint)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Departamental.selectable.forEach { dept ->
                    DropdownMenuItem(
                        text = { Text(dept.displayName) },
                        onClick = {
                            onSelect(dept)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
        if (selected.isActive) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_departamental_deep_scan_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DarkModeSelector(value: DarkMode, onChange: (DarkMode) -> Unit) {
    Column {
        Text(stringResource(R.string.settings_dark_mode), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkMode.entries.forEach { mode ->
                FilterChip(
                    selected = value == mode,
                    onClick = { onChange(mode) },
                    label = {
                        Text(
                            when (mode) {
                                DarkMode.LIGHT -> "Claro"
                                DarkMode.DARK -> "Oscuro"
                                DarkMode.SYSTEM -> "Sistema"
                            }
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Elegí la hora del chequeo diario",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                TimePicker(state = state)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
