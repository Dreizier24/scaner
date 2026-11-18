package ru.cora.scanner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.cora.scanner.ui.components.KoraTopBar
import ru.cora.scanner.ui.theme.KoraCream
import ru.cora.scanner.ui.theme.YaBlack
import ru.cora.scanner.ui.theme.YaYellow

/**
 * Меню как в «Яндекс Билеты: сканер»: иконка слева → переключатели
 * (см. https://yandex.ru/support/tickets/ru/enter#settings).
 */
@Composable
fun ScannerSettingsScreen(
    viewModel: ScannerSettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KoraCream),
    ) {
        Box {
            KoraTopBar(
                title = "Настройки",
                subtitle = if (state.isOnline && !state.forceOffline) "Онлайн" else "Офлайн",
                isOnline = state.isOnline && !state.forceOffline,
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = YaBlack,
                )
            }
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .background(Color.White),
        ) {
            SettingSwitch("Статистика проходов", state.passStats, viewModel::setPassStats)
            SettingSwitch("Автосканирование", state.autoScan, viewModel::setAutoScan)
            SettingSwitch(
                title = "Онлайн режим",
                subtitle = "База штрихкодов с сервера",
                checked = !state.forceOffline,
                onChecked = { online -> viewModel.setForceOffline(!online) },
            )
            SettingSwitch(
                title = "Офлайн режим",
                subtitle = "База не обновляется сама, пока нет сети",
                checked = state.forceOffline,
                onChecked = viewModel::setForceOffline,
            )
            SettingSwitch(
                title = "Автосинхронизация",
                subtitle = "В офлайне: отправка очереди, когда появится сеть",
                checked = state.autoSync,
                onChecked = viewModel::setAutoSync,
            )
            SettingSwitch("Звук при сканировании", state.soundEnabled, viewModel::setSoundEnabled)
            SettingSwitch("Вибрация при сканировании", state.vibrationEnabled, viewModel::setVibrationEnabled)
            SettingSwitch(
                title = "Камера",
                subtitle = "Смартфон. На терминале сбора данных можно выключить",
                checked = state.useCamera,
                onChecked = viewModel::setUseCamera,
            )
            Text(
                text = "Направление «Вход» или «Выход» выбирается на экране сканирования.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = YaYellow,
                    checkedThumbColor = YaBlack,
                ),
            )
        }
        HorizontalDivider(color = Color(0xFFEEEEEE))
    }
}