package ru.cora.scanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.cora.scanner.data.ConcertScanRepository
import ru.cora.scanner.data.NetworkMonitor
import ru.cora.scanner.data.local.entity.ScanHistoryEntity
import ru.cora.scanner.domain.model.TicketInfo

enum class ScannerMode {
    Ticket,
    DigitalId,
}

data class ScannerUiState(
    val isOnline: Boolean = true,
    val isProcessing: Boolean = false,
    val lastTicket: TicketInfo? = null,
    val errorMessage: String? = null,
    val scanPaused: Boolean = false,
    val syncMessage: String? = null,
    val manifestLoaded: Int? = null,
    val redeemedCount: Int = 0,
    val mode: ScannerMode = ScannerMode.Ticket,
)

class ScannerViewModel(
    private val concertId: String?,
    private val repository: ConcertScanRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<ScanHistoryEntity>> = repository.recentScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
                if (online) {
                    runCatching { repository.flushPendingWhenOnline() }
                }
            }
        }
        if (concertId != null) {
            viewModelScope.launch {
                repository.observeRedeemedCount(concertId).collect { count ->
                    _uiState.update { it.copy(redeemedCount = count) }
                }
            }
        }
    }

    fun setMode(mode: ScannerMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun onBarcodeDetected(raw: String) {
        val state = _uiState.value
        if (state.isProcessing || state.scanPaused) return
        if (state.lastTicket?.rawPayload == raw) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            runCatching {
                repository.resolveScan(
                    concertId = concertId,
                    raw = raw,
                    isOnline = state.isOnline,
                    modeDigitalId = state.mode == ScannerMode.DigitalId,
                )
            }.onSuccess { ticket ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastTicket = ticket,
                        scanPaused = true,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = error.message ?: "Не удалось обработать код",
                    )
                }
            }
        }
    }

    fun resumeScanning() {
        _uiState.update { it.copy(scanPaused = false, lastTicket = null, errorMessage = null) }
    }

    fun loadManifest() {
        val id = concertId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(syncMessage = null) }
            repository.loadManifest(id)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            manifestLoaded = result.placeCount,
                            syncMessage = if (result.skippedDownload) {
                                "Манифест актуален (rev ${result.revision})"
                            } else {
                                "Загружено мест: ${result.placeCount}"
                            },
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(syncMessage = "Не удалось загрузить манифест (API ещё не подключён)")
                    }
                }
        }
    }

    fun importExternalBarcodes(json: String) {
        val id = concertId ?: return
        viewModelScope.launch {
            repository.importExternalBarcodesJson(id, json)
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(syncMessage = "Импортировано штрихкодов: $count")
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(syncMessage = "Ошибка импорта: проверьте JSON")
                    }
                }
        }
    }

    class Factory(
        private val concertId: String?,
        private val repository: ConcertScanRepository,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(concertId, repository, networkMonitor) as T
        }
    }
}
