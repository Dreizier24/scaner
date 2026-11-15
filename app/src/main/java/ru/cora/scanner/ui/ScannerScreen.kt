package ru.cora.scanner.ui

import android.Manifest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import ru.cora.scanner.camera.QrCameraPreview
import ru.cora.scanner.domain.model.TicketInfo
import ru.cora.scanner.domain.model.TicketKind
import ru.cora.scanner.ui.components.InfoRow
import ru.cora.scanner.ui.components.KoraTopBar
import ru.cora.scanner.ui.components.ScannerFrameOverlay
import ru.cora.scanner.ui.components.StatusChip
import ru.cora.scanner.ui.theme.KoraBurgundy
import ru.cora.scanner.ui.theme.KoraCream
import ru.cora.scanner.ui.theme.KoraGold

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    concertId: String?,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    LaunchedEffect(concertId) {
        if (concertId != null) {
            viewModel.loadManifest()
        }
    }

    LaunchedEffect(uiState.lastTicket?.rawPayload) {
        if (uiState.lastTicket != null) {
            vibrateSuccess(context)
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Сторонние штрихкоды") },
            text = {
                Column {
                    Text(
                        text = "Вставьте JSON-массив: barcode, row, seat, sector, isPushkin",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        minLines = 4,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importExternalBarcodes(importJson)
                        showImportDialog = false
                    },
                ) { Text("Импорт") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Отмена") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KoraCream),
    ) {
        Box {
            KoraTopBar(
                title = "КОРА",
                subtitle = if (concertId != null) "Контроль входа" else "Сканер билетов",
                isOnline = uiState.isOnline,
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.mode == ScannerMode.Ticket,
                    onClick = { viewModel.setMode(ScannerMode.Ticket) },
                    label = { Text("Билеты") },
                )
                FilterChip(
                    selected = uiState.mode == ScannerMode.DigitalId,
                    onClick = { viewModel.setMode(ScannerMode.DigitalId) },
                    label = { Text("Цифровой ID") },
                )
            }

            if (concertId != null) {
                Text(
                    text = "Погашено (кэш): ${uiState.redeemedCount}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(20.dp)),
                ) {
                    if (cameraPermission.status.isGranted) {
                        QrCameraPreview(
                            enabled = !uiState.scanPaused && !uiState.isProcessing,
                            onQrDetected = viewModel::onBarcodeDetected,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.72f)
                                .height(220.dp),
                        ) {
                            ScannerFrameOverlay(modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Нужен доступ к камере",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                                Text("Разрешить камеру")
                            }
                        }
                    }

                    if (uiState.isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = KoraGold)
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            uiState.syncMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            uiState.lastTicket?.let { ticket ->
                TicketResultCard(ticket = ticket, onScanAgain = viewModel::resumeScanning)
            }

            if (concertId != null) {
                OutlinedButton(
                    onClick = viewModel::loadManifest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Text(text = "  Загрузить манифест мест (API)", modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Text(text = "  Импорт сторонних штрихкодов (JSON)", modifier = Modifier.padding(start = 4.dp))
                }
            }

            if (!uiState.scanPaused) {
                Text(
                    text = "QR / Code128 / EAN / PDF417 и др.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (history.isNotEmpty()) {
                HistorySection(
                    history.map { item ->
                        val place = listOfNotNull(item.rowLabel, item.seatLabel).joinToString(", ")
                        val title = item.eventTitle ?: item.rawPayload.take(32)
                        if (place.isNotEmpty()) "$title — $place" else title
                    },
                )
            }
        }
    }
}

@Composable
private fun TicketResultCard(
    ticket: TicketInfo,
    onScanAgain: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            StatusChip(ticket.validationStatus)
            if (ticket.ticketKind == TicketKind.Pushkin) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Пушкинская карта",
                    style = MaterialTheme.typography.labelLarge,
                    color = KoraGold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = ticket.eventTitle ?: "Результат сканирования",
                style = MaterialTheme.typography.titleLarge,
            )
            InfoRow("Ряд", ticket.row)
            InfoRow("Место", ticket.seat)
            InfoRow("Сектор", ticket.sector)
            InfoRow("ID места", ticket.placeId ?: ticket.ticketId)
            InfoRow("Площадка", ticket.venue)
            InfoRow("Дата и время", ticket.dateTime)
            InfoRow("Посетитель", ticket.holderName)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Тип: ${ticket.ticketKind.name} · ${ticket.source.name}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onScanAgain,
                colors = ButtonDefaults.buttonColors(containerColor = KoraBurgundy),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(text = "  Сканировать следующий", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun HistorySection(items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Последние сканы", style = MaterialTheme.typography.titleLarge)
            items.take(5).forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private fun vibrateSuccess(context: android.content.Context) {
    val duration = 40L
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}
