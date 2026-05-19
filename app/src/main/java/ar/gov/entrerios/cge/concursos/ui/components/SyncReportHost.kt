package ar.gov.entrerios.cge.concursos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.gov.entrerios.cge.concursos.R
import ar.gov.entrerios.cge.concursos.core.util.UiSyncEvent
import ar.gov.entrerios.cge.concursos.domain.repository.SyncReport
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Escucha el flujo global de eventos de sincronización y muestra:
 *  - Un loading dialog mientras la sync está en curso (cerrable; la sync sigue).
 *  - El [SyncReportDialog] cuando termina (Completed / Failed).
 *
 * Se monta una sola vez en la raíz de la UI (ver MainActivity).
 */
@Composable
fun SyncReportHost(
    events: SharedFlow<UiSyncEvent>,
    syncInProgress: StateFlow<Boolean>,
    onOpenConcurso: (Long) -> Unit
) {
    val busSyncInProgress by syncInProgress.collectAsStateWithLifecycle()
    var progressDismissed by rememberSaveable { mutableStateOf(false) }
    var pendingReport by remember { mutableStateOf<SyncReport?>(null) }
    var pendingError by remember { mutableStateOf<Throwable?>(null) }

    val dismissProgressDialog = {
        progressDismissed = true
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is UiSyncEvent.Started -> {
                    progressDismissed = false
                    pendingReport = null
                    pendingError = null
                }
                is UiSyncEvent.Completed -> {
                    progressDismissed = false
                    pendingReport = event.report
                }
                is UiSyncEvent.Failed -> {
                    progressDismissed = false
                    pendingError = event.throwable
                }
            }
        }
    }

    if (busSyncInProgress && !progressDismissed) {
        AlertDialog(
            onDismissRequest = dismissProgressDialog,
            confirmButton = {
                TextButton(onClick = dismissProgressDialog) {
                    Text(stringResource(R.string.sync_continue_background))
                }
            },
            title = { Text(stringResource(R.string.sync_searching_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(
                            text = stringResource(R.string.sync_searching_message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = stringResource(R.string.sync_searching_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        )
    }

    pendingReport?.let { report ->
        SyncReportDialog(
            report = report,
            onDismiss = { pendingReport = null },
            onOpenFirstRelevant = { id ->
                pendingReport = null
                onOpenConcurso(id)
            }
        )
    }

    pendingError?.let { error ->
        AlertDialog(
            onDismissRequest = { pendingError = null },
            confirmButton = {
                TextButton(onClick = { pendingError = null }) { Text("Aceptar") }
            },
            title = { Text("No se pudo sincronizar") },
            text = {
                Text(
                    text = error.message ?: "Error desconocido. Revisá tu conexión.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }
}
