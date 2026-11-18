package ru.cora.scanner.ui

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import ru.cora.scanner.data.ScanDirection
import ru.cora.scanner.domain.model.TicketInfo
import ru.cora.scanner.domain.model.TicketKind
import ru.cora.scanner.domain.model.ValidationStatus
import ru.cora.scanner.ui.components.InfoRow
import ru.cora.scanner.ui.components.KoraTopBar
import ru.cora.scanner.ui.components.PassStatsRow
import ru.cora.scanner.ui.components.ScanRepeatHistoryCard
import ru.cora.scanner.ui.components.ScanResultPanel
import ru.cora.scanner.ui.components.ScannerFrameOverlay
import ru.cora.scanner.ui.theme.KoraCream
import ru.cora.scanner.ui.theme.KoraGold
import ru.cora.scanner.ui.theme.YaBlack
import ru.cora.scanner.ui.theme.YaYellow

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var manualCode by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
        viewModel.refreshPreferences()
    }

    LaunchedEffect(uiState.lastTicket?.rawPayload, uiState.vibrationEnabled, uiState.soundEnabled) {
        val ticket = uiState.lastTicket ?: return@LaunchedEffect
        if (uiState.vibrationEnabled) {
            vibrateResult(context, ticket.validationStatus)
        }
        if (uiState.soundEnabled) {
            playScanTone(ticket.validationStatus)
        }
    }

    val subtitle = when (uiState.purpose) {
        ScannerPurpose.DigitalId -> "Цифровой ID"
        ScannerPurpose.Concert -> uiState.concertTitle ?: "Сканирование"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KoraCream),
    ) {
        KoraTopBar(
            title = "КОРА",
            subtitle = subtitle,
            isOnline = uiState.isOnline && !uiState.forceOffline,
        )
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = YaBlack)
            Text("Назад", color = YaBlack)
        }

        val canScanCamera = uiState.useCamera &&
            (uiState.purpose != ScannerPurpose.Concert || uiState.ticketCount > 0)
        if (uiState.useCamera) {
        CameraCard(
            cameraGranted = cameraPermission.status.isGranted,
            scanPaused = uiState.scanPaused,
            isProcessing = uiState.isProcessing,
            autoScan = uiState.autoScan && canScanCamera,
            onRequestCamera = { cameraPermission.launchPermissionRequest() },
            onBarcode = { viewModel.onBarcodeDetected(it, fromManual = false) },
        )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.purpose == ScannerPurpose.Concert) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.scanDirection == ScanDirection.Entry,
                        onClick = { viewModel.setScanDirection(ScanDirection.Entry) },
                        label = { Text("Вход") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = uiState.scanDirection == ScanDirection.Exit,
                        onClick = { viewModel.setScanDirection(ScanDirection.Exit) },
                        label = { Text("Выход") },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (uiState.passStats) {
                    PassStatsRow(
                        ticketsInDb = uiState.ticketCount,
                        redeemed = uiState.redeemedCount,
                        pendingSync = uiState.pendingSyncCount,
                    )
                }

                if (uiState.ticketCount == 0) {
                    Button(
                        onClick = viewModel::loadTickets,
                        enabled = !uiState.isLoadingTickets,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YaYellow,
                            contentColor = YaBlack,
                        ),
                    ) {
                        if (uiState.isLoadingTickets) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Text(text = "  Загрузить билеты", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            uiState.statusMessage?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedTextField(
                value = manualCode,
                onValueChange = { manualCode = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Штрихкод вручную") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            TextButton(
                onClick = {
                    val code = manualCode.trim()
                    if (code.isNotEmpty()) {
                        viewModel.onBarcodeDetected(code, fromManual = true)
                        manualCode = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Проверить штрихкод")
            }

            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            val lastTicket = uiState.lastTicket
            if (lastTicket != null) {
                ScanResultPanel(
                    status = lastTicket.validationStatus,
                    isExit = uiState.scanDirection == ScanDirection.Exit &&
                        uiState.purpose == ScannerPurpose.Concert,
                )
                if (
                    lastTicket.validationStatus == ValidationStatus.AlreadyUsed &&
                    lastTicket.scanHistoryCount > 0
                ) {
                    ScanRepeatHistoryCard(
                        totalScans = lastTicket.scanHistoryCount,
                        entries = lastTicket.scanHistory,
                    )
                }
                TicketDetailsSection(
                    ticket = lastTicket,
                    digitalMode = uiState.purpose == ScannerPurpose.DigitalId,
                    expanded = uiState.showTicketDetails,
                    onToggleDetails = viewModel::toggleTicketDetails,
                )
                Button(
                    onClick = viewModel::resumeScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YaYellow,
                        contentColor = YaBlack,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(text = "  Дальше", modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TicketDetailsSection(
    ticket: TicketInfo,
    digitalMode: Boolean,
    expanded: Boolean,
    onToggleDetails: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextButton(onClick = onToggleDetails, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "Скрыть" else "Подробнее")
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
            if (expanded) {
                if (ticket.ticketKind == TicketKind.Pushkin) {
                    Text(
                        text = "Пушкинская карта",
                        style = MaterialTheme.typography.labelLarge,
                        color = KoraGold,
                    )
                }
                if (digitalMode) {
                    Text(text = ticket.rawPayload.take(120), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(text = ticket.eventTitle ?: "Билет", style = MaterialTheme.typography.titleMedium)
                    val rowSeat = listOfNotNull(
                        ticket.row?.let { "ряд $it" },
                        ticket.seat?.let { "место $it" },
                    ).joinToString(", ")
                    if (rowSeat.isNotBlank()) {
                        Text(text = rowSeat, style = MaterialTheme.typography.headlineSmall)
                    }
                    InfoRow("Номер заказа", ticket.orderId)
                    InfoRow("Сектор", ticket.sector)
                    InfoRow("Ряд", ticket.row)
                    InfoRow("Место", ticket.seat)
                    InfoRow("ФИО", ticket.holderName)
                    InfoRow("Площадка", ticket.venue)
                    InfoRow("Дата и время", ticket.dateTime)
                }
            }
        }
    }
}

@Composable
private fun CameraCard(
    cameraGranted: Boolean,
    scanPaused: Boolean,
    isProcessing: Boolean,
    autoScan: Boolean,
    onRequestCamera: () -> Unit,
    onBarcode: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            if (cameraGranted) {
                QrCameraPreview(
                    enabled = !scanPaused && !isProcessing && autoScan,
                    onQrDetected = onBarcode,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.78f)
                        .height(140.dp),
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
                        text = "Включите камеру в настройках",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRequestCamera) {
                        Text("Разрешить камеру")
                    }
                }
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = KoraGold)
                }
            }
        }
    }
}

private fun vibrateResult(context: android.content.Context, status: ValidationStatus) {
    val duration = if (status == ValidationStatus.Valid) 50L else 120L
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

private fun playScanTone(status: ValidationStatus) {
    val tone = when (status) {
        ValidationStatus.Valid -> ToneGenerator.TONE_PROP_ACK
        ValidationStatus.AlreadyUsed -> ToneGenerator.TONE_PROP_NACK
        ValidationStatus.NotFound -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
        else -> ToneGenerator.TONE_PROP_BEEP
    }
    runCatching {
        val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        generator.startTone(tone, 180)
        generator.release()
    }
}