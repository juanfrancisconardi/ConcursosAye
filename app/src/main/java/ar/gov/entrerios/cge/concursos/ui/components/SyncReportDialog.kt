package ar.gov.entrerios.cge.concursos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.gov.entrerios.cge.concursos.domain.repository.SyncReport

/**
 * Popup que se muestra al finalizar una sincronización foreground.
 * Resume cuántas publicaciones se evaluaron, cuántas son nuevas-relevantes
 * y muestra los errores (si hubo).
 */
@Composable
fun SyncReportDialog(
    report: SyncReport,
    onDismiss: () -> Unit,
    onOpenFirstRelevant: ((Long) -> Unit)? = null
) {
    val hasErrors = report.errors.isNotEmpty()
    val hasNew = report.newRelevant.isNotEmpty()

    val icon = when {
        hasNew -> Icons.Outlined.CheckCircle
        hasErrors -> Icons.Outlined.Warning
        else -> Icons.Outlined.Info
    }
    val title = when {
        hasNew -> "Sincronización completa"
        hasErrors -> "Sincronización con avisos"
        else -> "Sin novedades"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = buildSummary(report),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (hasNew) {
                    Text(
                        text = "Nuevos relevantes:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    report.newRelevant.take(8).forEach { c ->
                        Text(
                            text = "• ${c.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (report.newRelevant.size > 8) {
                        Text(
                            text = "…y ${report.newRelevant.size - 8} más.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                if (hasErrors) {
                    Text(
                        text = "Avisos (${report.errors.size}):",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    report.errors.take(4).forEach { err ->
                        Text(
                            text = "• $err",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (hasNew && onOpenFirstRelevant != null) {
                TextButton(onClick = {
                    onOpenFirstRelevant(report.newRelevant.first().id)
                    onDismiss()
                }) {
                    Text("Ver primero")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Aceptar") }
            }
        },
        dismissButton = {
            if (hasNew && onOpenFirstRelevant != null) {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    )
}

private fun buildSummary(report: SyncReport): String {
    val total = report.totalFetched
    val nuevos = report.newRelevant.size
    return when {
        total == 0 && nuevos == 0 && report.errors.isEmpty() ->
            "No se encontraron publicaciones para procesar."
        nuevos == 0 ->
            "Se revisaron $total publicaciones del CGE.\nNo aparecieron nuevos concursos relevantes para tus palabras clave."
        else ->
            "Se revisaron $total publicaciones del CGE.\nSe encontraron $nuevos NUEVOS concursos que coinciden con tus palabras clave."
    }
}
