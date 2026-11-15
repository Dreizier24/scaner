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
import ru.cora.scanner.data.ApiErrorMessages
import ru.cora.scanner.data.ConcertScanRepository
import ru.cora.scanner.data.NetworkMonitor
import ru.cora.scanner.domain.model.TicketInfo

data class ScannerUiState(
    val isOnline: Boolean = true,
    val isProcessing: Boolean = false,
    val lastTicket: TicketInfo? = null,
    val errorMessage: String? = null,
    val scanPaused: Boolean = false,
    val statusMessage: String? = null,
    val ticketsReady: Boolean = false,
    val redeemedCount: Int = 0,
    val concertTitle: String? = null,
)

class ScannerViewModel(
    private val concertId: String,
    private val repository: ConcertScanRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
                if (online) {
                    runCatching { repository.flushPendingWhenOnline() }
                }
            }
        }
        viewModelScope.launch {
            repository.observeRedeemedCount(concertId).collect { count ->
                _uiState.update { it.copy(redeemedCount = count) }
            }
        }
        viewModelScope.launch {
            val title = repository.getConcertTitle(concertId)
            _uiState.update { it.copy(concertTitle = title) }
        }
        loadTickets()
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
                    modeDigitalId = false,
                )
            }.onSuccess { ticket ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastTicket = ticket,
                        scanPaused = true,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Не удалось прочитать билет. Попробуйте ещё раз.",
                    )
                }
            }
        }
    }

    fun resumeScanning() {
        _uiState.update { it.copy(scanPaused = false, lastTicket = null, errorMessage = null) }
    }

    fun loadTickets() {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Загружаем билеты…") }
            repository.loadManifest(concertId)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            ticketsReady = true,
                            statusMessage = if (result.placeCount > 0) {
                                "Билеты загружены — можно сканировать"
                            } else {
                                "Для этого концерта пока нет билетов в системе"
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            ticketsReady = false,
                            statusMessage = ApiErrorMessages.forUser(error) +
                                " Нажмите «Обновить билеты».",
                        )
                    }
                }
        }
    }

    class Factory(
        private val concertId: String,
        private val repository: ConcertScanRepository,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(concertId, repository, networkMonitor) as T
        }
    }
}