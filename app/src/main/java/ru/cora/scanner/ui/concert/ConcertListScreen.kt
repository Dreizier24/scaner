package ru.cora.scanner.ui.concert

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.cora.scanner.data.local.entity.ConcertEntity
import ru.cora.scanner.ui.components.KoraTopBar
import ru.cora.scanner.ui.theme.KoraCream
import ru.cora.scanner.ui.theme.YaBlack
import ru.cora.scanner.ui.theme.YaYellow
import java.nio.charset.StandardCharsets

@Composable
fun ConcertListScreen(
    viewModel: ConcertListViewModel,
    onConcertSelected: (String) -> Unit,
    onDigitalIdScan: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val concerts by viewModel.concerts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val text = stream.readBytes().toString(StandardCharsets.UTF_8)
                viewModel.importEventFromFile(text)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KoraCream),
    ) {
        KoraTopBar(
            title = "КОРА",
            subtitle = "Мероприятия",
            isOnline = uiState.isOnline,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    filePicker.launch(
                        arrayOf("application/json", "text/plain", "application/*"),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Text(text = "  Из файла", modifier = Modifier.padding(start = 4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDigitalIdScan,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null)
                    Text("  Цифровой ID")
                }
                OutlinedButton(
                    onClick = { viewModel.refreshConcerts() },
                    enabled = !uiState.isLoading && uiState.loadingConcertId == null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Text("  Обновить")
                }
            }

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = YaYellow,
                    contentColor = YaBlack,
                ),
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Text("  Настройки")
            }

            uiState.message?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (concerts.isEmpty()) {
                Text(
                    text = "Нет событий",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(concerts, key = { it.concertId }) { concert ->
                    val ticketCount by viewModel.ticketCount(concert.concertId)
                        .collectAsStateWithLifecycle()
                    ConcertCard(
                        concert = concert,
                        ticketCount = ticketCount,
                        loading = uiState.loadingConcertId == concert.concertId,
                        enabled = uiState.loadingConcertId == null,
                        onOpen = { onConcertSelected(concert.concertId) },
                        onLoad = { viewModel.loadTickets(concert.concertId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConcertCard(
    concert: ConcertEntity,
    ticketCount: Int,
    loading: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
    onLoad: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !loading, onClick = onOpen),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = concert.title, style = MaterialTheme.typography.titleMedium)
            if (concert.source == ConcertEntity.SOURCE_FILE) {
                Text(
                    text = "Файл",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            concert.dateTime?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = if (ticketCount > 0) "В базе: $ticketCount" else "Билеты не загружены",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (loading) {
                Text("Загрузка билетов…", style = MaterialTheme.typography.titleSmall)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                OutlinedButton(
                    onClick = onLoad,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Загрузить билеты")
                }
            }
        }
    }
}