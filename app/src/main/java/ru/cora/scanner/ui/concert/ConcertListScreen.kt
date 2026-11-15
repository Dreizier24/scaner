package ru.cora.scanner.ui.concert

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.cora.scanner.data.local.entity.ConcertEntity
import ru.cora.scanner.ui.components.KoraTopBar
import ru.cora.scanner.ui.theme.KoraCream

@Composable
fun ConcertListScreen(
    viewModel: ConcertListViewModel,
    onConcertSelected: (String) -> Unit,
    onDigitalIdScan: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val concerts by viewModel.concerts.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshConcerts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KoraCream),
    ) {
        KoraTopBar(
            title = "КОРА",
            subtitle = "Выберите концерт",
            isOnline = uiState.isOnline,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Концерты КОРА — билеты с сервера. Для гостевых или сторонних билетов выберите мероприятие и загрузите файл на следующем экране.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Button(
                onClick = viewModel::refreshConcerts,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(text = "  Обновить список концертов", modifier = Modifier.padding(start = 4.dp))
            }

            OutlinedButton(
                onClick = onDigitalIdScan,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Badge, contentDescription = null)
                Text(
                    text = "  Цифровой ID (Госкан)",
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            uiState.message?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            if (!uiState.isLoading && concerts.isEmpty()) {
                Text(
                    text = "Концерты не загружены. Нажмите кнопку обновления выше.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(concerts, key = { it.concertId }) { concert ->
                    ConcertCard(concert = concert, onClick = { onConcertSelected(concert.concertId) })
                }
            }
        }
    }
}

@Composable
private fun ConcertCard(concert: ConcertEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = concert.title, style = MaterialTheme.typography.titleLarge)
            concert.dateTime?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
            concert.venue?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}