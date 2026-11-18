package ru.cora.scanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import ru.cora.scanner.domain.model.ScanHistoryEntry
import ru.cora.scanner.domain.model.ValidationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ru.cora.scanner.ui.theme.KoraError
import ru.cora.scanner.ui.theme.KoraGold
import ru.cora.scanner.ui.theme.KoraMuted
import ru.cora.scanner.ui.theme.KoraSuccess
import ru.cora.scanner.ui.theme.KoraWarning
import ru.cora.scanner.ui.theme.YaBlack
import ru.cora.scanner.ui.theme.YaYellow

@Composable
fun KoraTopBar(
    title: String,
    subtitle: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = YaYellow,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = YaBlack,
                )
                NetworkBadge(isOnline = isOnline)
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = YaBlack,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun NetworkBadge(isOnline: Boolean) {
    val bg = if (isOnline) Color(0xFFE8F6EC) else Color(0xFFFFF3E0)
    val fg = if (isOnline) KoraSuccess else Color(0xFF8A4B00)
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        text = if (isOnline) "Онлайн" else "Офлайн",
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun ScanModeBanner(isOnlineControl: Boolean, forceOffline: Boolean) {
    val online = isOnlineControl && !forceOffline
    val label = if (online) "Онлайн режим" else "Офлайн режим"
    val bg = if (online) KoraSuccess.copy(alpha = 0.12f) else KoraWarning.copy(alpha = 0.18f)
    val fg = if (online) KoraSuccess else KoraWarning
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = fg,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun PassStatsRow(
    ticketsInDb: Int,
    redeemed: Int,
    pendingSync: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCell("В базе", ticketsInDb.toString())
        StatCell("Погашено", redeemed.toString())
        StatCell("Не синхр.", pendingSync.toString())
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = KoraMuted,
        )
    }
}

@Composable
fun ScanRepeatHistoryCard(
    totalScans: Int,
    entries: List<ScanHistoryEntry>,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("ru", "RU"))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(
            text = "Сканирований этого билета: $totalScans",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        entries.take(8).forEachIndexed { index, entry ->
            val statusLabel = scanStatusLabel(entry.status)
            val offline = if (entry.wasOffline) " · офлайн" else ""
            Text(
                text = "${index + 1}. ${fmt.format(Date(entry.scannedAtEpochMs))} — $statusLabel$offline",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (entries.size > 8) {
            Text(
                text = "… и ещё ${entries.size - 8}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
                color = KoraMuted,
            )
        }
    }
}

private fun scanStatusLabel(status: ValidationStatus): String = when (status) {
    ValidationStatus.Valid -> "допуск"
    ValidationStatus.AlreadyUsed -> "уже использован"
    ValidationStatus.NotFound -> "не найден"
    ValidationStatus.Expired -> "просрочен"
    ValidationStatus.PendingSync -> "ожидание синхронизации"
    ValidationStatus.Unknown -> "проверка"
}

@Composable
fun ScanResultPanel(
    status: ValidationStatus,
    isExit: Boolean,
    modifier: Modifier = Modifier,
) {
    val (headline, subline, bg) = resultCopy(status, isExit)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(20.dp),
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subline,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun resultCopy(
    status: ValidationStatus,
    isExit: Boolean,
): Triple<String, String, Color> = when (status) {
    ValidationStatus.Valid -> if (isExit) {
        Triple("Выход", "Проход зафиксирован", Color(0xFF2E6B9E))
    } else {
        Triple("Допуск", "Билет действителен, вход разрешён", KoraSuccess)
    }
    ValidationStatus.AlreadyUsed -> Triple(
        "Уже погашен",
        "Этот штрихкод уже использован",
        KoraWarning,
    )
    ValidationStatus.NotFound -> Triple(
        "Не найден",
        "Штрихкода нет в базе этого мероприятия",
        KoraError,
    )
    ValidationStatus.Expired -> Triple("Просрочен", "Билет недействителен", KoraError)
    ValidationStatus.PendingSync -> Triple(
        "Офлайн",
        "Результат сохранён, синхронизация при появлении сети",
        KoraWarning,
    )
    ValidationStatus.Unknown -> Triple(
        "Проверка",
        "Нужна связь с сервером или обновление базы",
        KoraMuted,
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
