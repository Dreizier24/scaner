package ru.cora.scanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.cora.scanner.BuildConfig
import ru.cora.scanner.data.NetworkMonitor
import ru.cora.scanner.data.ScannerPreferences

data class ScannerSettingsUiState(
    val isOnline: Boolean = true,
    val forceOffline: Boolean = false,
    val autoScan: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val passStats: Boolean = true,
    val autoSync: Boolean = true,
    val useCamera: Boolean = true,
    val apiUrl: String = BuildConfig.API_BASE_URL,
)

class ScannerSettingsViewModel(
    private val preferences: ScannerPreferences,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerSettingsUiState())
    val uiState: StateFlow<ScannerSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
        viewModelScope.launch { reload() }
    }

    private suspend fun reload() {
        _uiState.update {
            it.copy(
                forceOffline = preferences.isForceOffline(),
                autoScan = preferences.isAutoScan(),
                soundEnabled = preferences.isSoundEnabled(),
                vibrationEnabled = preferences.isVibrationEnabled(),
                passStats = preferences.isPassStatsEnabled(),
                autoSync = preferences.isAutoSyncEnabled(),
                useCamera = preferences.isCameraEnabled(),
            )
        }
    }

    fun setForceOffline(value: Boolean) = viewModelScope.launch {
        preferences.setForceOffline(value)
        _uiState.update { it.copy(forceOffline = value) }
    }

    fun setAutoScan(value: Boolean) = viewModelScope.launch {
        preferences.setAutoScan(value)
        _uiState.update { it.copy(autoScan = value) }
    }

    fun setSoundEnabled(value: Boolean) = viewModelScope.launch {
        preferences.setSoundEnabled(value)
        _uiState.update { it.copy(soundEnabled = value) }
    }

    fun setVibrationEnabled(value: Boolean) = viewModelScope.launch {
        preferences.setVibrationEnabled(value)
        _uiState.update { it.copy(vibrationEnabled = value) }
    }

    fun setPassStats(value: Boolean) = viewModelScope.launch {
        preferences.setPassStatsEnabled(value)
        _uiState.update { it.copy(passStats = value) }
    }

    fun setAutoSync(value: Boolean) = viewModelScope.launch {
        preferences.setAutoSyncEnabled(value)
        _uiState.update { it.copy(autoSync = value) }
    }

    fun setUseCamera(value: Boolean) = viewModelScope.launch {
        preferences.setCameraEnabled(value)
        _uiState.update { it.copy(useCamera = value) }
    }

    class Factory(
        private val preferences: ScannerPreferences,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerSettingsViewModel(preferences, networkMonitor) as T
        }
    }
}