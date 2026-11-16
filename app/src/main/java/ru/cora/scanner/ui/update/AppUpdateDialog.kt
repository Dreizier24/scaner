package ru.cora.scanner.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.cora.scanner.update.AppUpdateManifest
import ru.cora.scanner.update.AppUpdateUiState

@Composable
fun AppUpdateDialog(
    state: AppUpdateUiState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val available = state as? AppUpdateUiState.Available ?: return
    val manifest: AppUpdateManifest = available.manifest

    AlertDialog(
        onDismissRequest = {
            if (!manifest.mandatory && !available.downloading) onDismiss()
        },
        title = { Text("Доступно обновление") },
        text = {
            Column {
                Text(
                    "Версия ${manifest.versionName} (сборка ${manifest.versionCode})",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (manifest.releaseNotes.isNotBlank()) {
                    Text(
                        manifest.releaseNotes,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (available.downloading) {
                    LinearProgressIndicator(
                        progress = { available.progress ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                    Text(
                        "Загрузка…",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                available.error?.let { err ->
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (!available.downloading) {
                TextButton(onClick = {
                    if (available.error?.contains("настройках") == true) {
                        onOpenSettings()
                    } else {
                        onUpdate()
                    }
                }) {
                    Text(
                        if (available.error?.contains("настройках") == true) {
                            "Настройки"
                        } else {
                            "Обновить"
                        },
                    )
                }
            }
        },
        dismissButton = {
            if (!manifest.mandatory && !available.downloading) {
                TextButton(onClick = onDismiss) {
                    Text("Позже")
                }
            }
        },
    )
}