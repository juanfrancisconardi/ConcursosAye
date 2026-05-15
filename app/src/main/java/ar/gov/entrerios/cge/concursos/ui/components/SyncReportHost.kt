package ar.gov.entrerios.cge.concursos.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.gov.entrerios.cge.concursos.core.util.UiSyncEvent
import ar.gov.entrerios.cge.concursos.domain.repository.SyncReport
import kotlinx.coroutines.flow.SharedFlow

/**
 * Escucha el flujo global de eventos de sincronización y muestra:
 *  - Un loading dialog mientras la sync está en curso.
 *  - El [SyncReportDialog] cuando termina (Completed / Failed).
 *
 * Se monta una sola vez en la raíz de la UI (ver MainActivity).
 */
@Composable
fun SyncReportHost(
    events: SharedFlow<UiSyncEvent>,
    onOpenConcurso: (Long) -> Unit
) {
    var inProgress by remember { mutableStateOf(false) }
    var pendingReport by remember { mutableStateOf<SyncReport?>(null) }
    var pendingError by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is UiSyncEvent.Started -> {
                    inProgress = true
                    pendingReport = null
                    pendingError = null
                }
                is UiSyncEvent.Completed -> {
                    inProgress = false
                    pendingReport = event.report
                }
                is UiSyncEvent.Failed -> {
                    inProgress = false
                    pendingError = event.throwable
                }
            }
        }
    }

    if (inProgress) {
        AlertDialog(
            onDismissRequest = { /* no cerrable durante la operación */ },
            confirmButton = {},
            title = { Text("Buscando concursos…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        text = "Conectando con CGE Entre Ríos…",
                        style = MaterialTheme.typography.bodyMedium
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
