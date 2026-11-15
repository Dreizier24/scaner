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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    LaunchedEffect(uiState.lastTicket?.rawPayload) {
        if (uiState.lastTicket != null) {
            vibrateSuccess(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KoraCream),
    ) {
        Box {
            KoraTopBar(
                title = "КОРА",
                subtitle = uiState.concertTitle ?: "Проверка билетов",
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
            uiState.statusMessage?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyLarge)
            }

            if (uiState.redeemedCount > 0) {
                Text(
                    text = "Уже прошли: ${uiState.redeemedCount}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            OutlinedButton(
                onClick = viewModel::loadTickets,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Обновить билеты")
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
                                text = "Для сканирования нужен доступ к камере",
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

            if (!uiState.scanPaused) {
                Text(
                    text = "Наведите камеру на QR-код или штрихкод билета",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            uiState.lastTicket?.let { ticket ->
                TicketResultCard(ticket = ticket, onScanAgain = viewModel::resumeScanning)
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
                text = ticket.eventTitle ?: "Билет",
                style = MaterialTheme.typography.titleLarge,
            )
            val rowSeat = listOfNotNull(
                ticket.row?.let { "ряд $it" },
                ticket.seat?.let { "место $it" },
            ).joinToString(", ")
            if (rowSeat.isNotBlank()) {
                Text(
                    text = rowSeat.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            InfoRow("Сектор", ticket.sector)
            InfoRow("Площадка", ticket.venue)
            InfoRow("Дата и время", ticket.dateTime)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onScanAgain,
                colors = ButtonDefaults.buttonColors(containerColor = KoraBurgundy),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(text = "  Проверить следующий билет", modifier = Modifier.padding(start = 4.dp))
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