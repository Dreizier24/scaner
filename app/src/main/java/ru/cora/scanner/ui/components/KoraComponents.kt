package ru.cora.scanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.cora.scanner.domain.model.ValidationStatus
import ru.cora.scanner.ui.theme.KoraBurgundy
import ru.cora.scanner.ui.theme.KoraError
import ru.cora.scanner.ui.theme.KoraGold
import ru.cora.scanner.ui.theme.KoraMuted
import ru.cora.scanner.ui.theme.KoraSuccess
import ru.cora.scanner.ui.theme.KoraWarning

@Composable
fun KoraTopBar(
    title: String,
    subtitle: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = KoraBurgundy,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFF2E8EC),
                )
                if (!isOnline) {
                    NetworkBadge(isOnline = false)
                }
            }
        }
    }
}

@Composable
fun NetworkBadge(isOnline: Boolean) {
    val bg = if (isOnline) KoraSuccess.copy(alpha = 0.25f) else KoraWarning.copy(alpha = 0.25f)
    val fg = if (isOnline) Color(0xFFDFF5E8) else Color(0xFFFFF1CC)
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = "Нет сети",
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun StatusChip(status: ValidationStatus) {
    val (label, color) = when (status) {
        ValidationStatus.Valid -> "Допуск" to KoraSuccess
        ValidationStatus.AlreadyUsed -> "Уже использован" to KoraWarning
        ValidationStatus.NotFound -> "Не найден" to KoraError
        ValidationStatus.Expired -> "Просрочен" to KoraError
        ValidationStatus.PendingSync -> "Запишется при появлении сети" to KoraWarning
        ValidationStatus.Unknown -> "Нужна проверка" to KoraMuted
    }
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}

@Composable
fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = KoraMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun ScannerFrameOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(KoraGold)
                .padding(horizontal = 28.dp, vertical = 3.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(KoraGold)
                .padding(horizontal = 28.dp, vertical = 3.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(KoraGold)
                .padding(horizontal = 28.dp, vertical = 3.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(KoraGold)
                .padding(horizontal = 28.dp, vertical = 3.dp),
        )
    }
}
